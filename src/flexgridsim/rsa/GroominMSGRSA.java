/*
 * 
 */
package flexgridsim.rsa;


import java.util.ArrayList;

import org.w3c.dom.Element;

import flexgridsim.FlexGridLink;
import flexgridsim.Flow;
import flexgridsim.Modulations;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.ConstantsRSA;
import flexgridsim.util.CostedLightPath;
import flexgridsim.util.MultiGraph;
import flexgridsim.util.WeightedGraph;
/**
 * A weighted graph associates a label (weight) with every edge in the graph. If
 * a pair of nodes has a array of weights equal to zero, it means the edge between them
 * doesn't exist.
 * 
 * @author pedrom
 */
public class GroominMSGRSA implements RSA {
	protected PhysicalTopology pt;
	protected VirtualTopology vt;
	protected ControlPlaneForRSA cp;
	protected WeightedGraph graph;
	protected MultiGraph[] modulationSpectrumGraph;
	protected Element rsaXml;

	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt,
			ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
		this.modulationSpectrumGraph = new MultiGraph[Modulations.numberOfModulations()];
		for (int i = 0; i < modulationSpectrumGraph.length; i++) {
			modulationSpectrumGraph[i] = new MultiGraph(this.graph, pt, traffic);
		}
		this.rsaXml = xml;
	}

	public void flowArrival(Flow flow) {
		// Shortest-Path routing
		if (cp.canGroom(flow)){
			flow.setGroomed(true);
		} else {
			CostedLightPath path[] = new CostedLightPath[modulationSpectrumGraph.length];
			for (int i = 0; i < modulationSpectrumGraph.length; i++) {
				int demandInSlots = (int) Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(i)) + 2;
					path[i] = getShortestPath(this.modulationSpectrumGraph[i],
						flow.getSource(), flow.getDestination(), demandInSlots, i);
			}
			
			CostedLightPath minCost = minCostedPath(path);
			if (minCost == null) {
				cp.blockFlow(flow.getID());
				return;
			}
			// Create the links vector
			
			long id = vt.createLightpath(minCost.getLinks(),minCost.getSlotList(), minCost.getModulationLevel());
			if (id >= 0) {
				// Single-hop routing (end-to-end lightpath)
				flow.setSlotList(minCost.getSlotList());
				flow.setLinks(minCost.getLinks());
				flow.setModulationLevel(minCost.getModulationLevel());
				cp.acceptFlow(flow.getID(), vt.getLightpath(id));
				for (int i = 0; i < minCost.getLinks().length; i++) {
					for (int j = 0; j < modulationSpectrumGraph.length; j++) {
						FlexGridLink curentLink = pt.getLink(minCost.getLink(i));
						this.modulationSpectrumGraph[j].markEdgesRemoved(
										curentLink.getSource(),
										curentLink.getDestination(),
										minCost.getSlotList().get(0).s,
										minCost.getSlotList().get(minCost.getSlotList().size()-1).s);
					}
					
				}
				return;
			}
			// Block the call
			cp.blockFlow(flow.getID());
		}
	}

	public void flowDeparture(Flow flow) {
		if (flow== null || flow.isGroomed())
			return;
		for (int i = 0; i < flow.getLinks().length; i++) {
			FlexGridLink curentLink = pt.getLink(flow.getLink(i));
			for (int j = 0; j < modulationSpectrumGraph.length; j++) {
				this.modulationSpectrumGraph[j].restoreRemovedEdges(curentLink.getSource(), curentLink.getDestination(), flow.getSlotList().get(0).s, flow.getSlotList().get(flow.getSlotList().size()-1).s);
			}
		}
	}

	/**
	 * Retrieves the shortest path between a source and a destination node,
	 * within a weighted graph.
	 *
	 * @param G the weighted graph in which the shortest path will be found
	 * @param src the source node
	 * @param dst the destination node
	 * @param demand size of the demand
	 * @param modulationLevel the modulation level
	 * @return the shortest path, as a vector of integers that represent node
	 * coordinates
	 */
	public CostedLightPath getShortestPath(MultiGraph G, int src, int dst,	int demand, int modulationLevel) {
		double[][] distance = new double[G.size()][pt.getNumSlots()];
		int[][] previous = new int[G.size()][pt.getNumSlots()];
		for (int i = 0; i < G.size(); i++) {
			for (int j = 0; j < pt.getNumSlots(); j++) {
				distance[i][j] = ConstantsRSA.INFINITE;
				previous[i][j] = -1;
			}
		}
		for (int i = 0; i < pt.getNumSlots(); i++) {
			distance[src][i] = 0;
		}
		ArrayList<Integer> Q = new ArrayList<Integer>();
		for (int i = 0; i < G.size(); i++) {
			Q.add(Integer.valueOf(i));
		}
		while (!Q.isEmpty()) {
			int u = minMatrixIndex(distance, Q);
			int uIndex = getListIndex(Q, u);
			Q.remove(uIndex);
			int[] neighbors = G.neighbors(u);
			for (int i = 0; i < neighbors.length; i++) {
				int v = neighbors[i];
				for (int s = 0; s < pt.getNumSlots() - demand - 1; s++) {
					if (G.hasSetOfEdges(u, v, s, s + demand - 1) && (getPhysicalDistance(v, dst, s, previous) < Modulations.getMaxDistance(modulationLevel) || modulationLevel == 0)) {	
						double cost = distance[u][s] + calculateCost(u, v, s, demand-1, modulationLevel);
						if (cost < distance[v][s]) {
							distance[v][s] = cost;
							previous[v][s] = u;
						}
					}
				}
			}
		}
		ArrayList<Integer> path = new ArrayList<Integer>();
		int minDistanceSlot = minArrayIndex(distance[dst]);
		int last = dst;
		while (last != src) {
			path.add(0,last);
			last = previous[last][minDistanceSlot];
			// No path
			if (last == -1) {
				return null;
			}
		}
		path.add(0, src);
		int[] links = new int[path.size() - 1];
		for (int i = 0; i < path.size() - 1; i++) {
			links[i] = pt.getLink(path.get(i), path.get(i+1)).getID();
		}
		ArrayList<Slot> slotList = new ArrayList<Slot>();
		for (int j = minDistanceSlot; j <= minDistanceSlot + demand - 1; j++) {
			slotList.add(new Slot(0, j));
		}
		return new CostedLightPath(Integer.MAX_VALUE, src, dst,	links, slotList, 0, distance[dst][minDistanceSlot]);
	}
	
	/**
	 * Min costed path.
	 *
	 * @param path the path
	 * @return the costed light path
	 */
	public CostedLightPath minCostedPath(CostedLightPath[] path){
		int i=0;
		while (path[i] == null && i < Modulations.numberOfModulations()-1) {
			i++;
		}
		int minCostPathModulation = 0;
		CostedLightPath minCostPath = path[i];
		i++;
		while (i < Modulations.numberOfModulations()){
			if (path[i]!=null){
				if (path[i].getCost() < minCostPath.getCost()){
					minCostPath = path[i];
					minCostPathModulation = i;
				}
			}
			i++;
			
		}
		if (minCostPath != null)
			minCostPath.setModulationLevel(minCostPathModulation);
		return minCostPath;
	}
	
	/**
	 * Gets the physical distance.
	 *
	 * @param src the src
	 * @param dst the dst
	 * @param slot the slot
	 * @param previous the previous
	 * @return the physical distance
	 */
	public double getPhysicalDistance(int src, int dst, int slot, int[][] previous){
		ArrayList<Integer> path = new ArrayList<Integer>();
		int last = dst;
		while (last != src) {
			path.add(0,last);
			last = previous[last][slot];
			// No path
			if (last == -1) {
				return Double.MAX_VALUE;
			}
		}
		path.add(0, src);
		
		double physicalDistance = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			physicalDistance += pt.getLink(path.get(i), path.get(i+1)).getDistance();
		}
		return physicalDistance;
	}
	/**
	 * Check if and array list contais an integer value.
	 *
	 * @param list the list
	 * @param value the value
	 * @return true, if it contains
	 */
	public boolean contain(ArrayList<Integer> list, int value) {
		for (int i = 0; i < list.size(); i++) {
			if (value == list.get(i).intValue()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * implement a function that.
	 *
	 * @param src the source node
	 * @param dst the destination node
	 * @param firstSlot the first slot
	 * @param demand the demand
	 * @param modulationLevel the modulation level
	 * @return the double
	 */
	public double calculateCost(int src, int dst, int firstSlot, int demand, int modulationLevel) {
		 double cost =  (double)((demand-2)*Modulations.getPowerConsumption(modulationLevel)) + (pt.getNodeDegree(dst)*85+150);
//		double cost = Modulations.getPowerConsumption(modulationLevel);
		return cost;
//		return (double)(numberOfDataSlots*Modulations.getPowerConsumption(modulationLevel))+
//				((numberOfDataSlots+2)/(pt.getNumSlots()))*(PCoxc+200*(nodes.length-1));
	}

	/**
	 * Min array index.
	 *
	 * @param array the array
	 * @return the int
	 */
	public static int minArrayIndex(double[] array) {
		double min = Double.MAX_VALUE;
		int index = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] < min) {
				min = array[i];
				index = i;
			}
		}
		return index;
	}

	/**
	 * Min matrix index.
	 *
	 * @param array the array
	 * @param Q the q
	 * @return the int
	 */
	public static int minMatrixIndex(double[][] array, ArrayList<Integer> Q) {
		double min = Double.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				if (array[i][j] < min && containsValue(Q, i)) {
					min = array[i][j];
					index = i;
				}
			}

		}
		return index;
	}

	/**
	 * Contains value.
	 *
	 * @param Q the q
	 * @param value the value
	 * @return true, if successful
	 */
	public static boolean containsValue(ArrayList<Integer> Q, int value) {
		for (int i = 0; i < Q.size(); i++) {
			if (Q.get(i).intValue() == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the list index.
	 *
	 * @param Q the q
	 * @param value the value
	 * @return the list index
	 */
	public static int getListIndex(ArrayList<Integer> Q, int value) {
		for (int i = 0; i < Q.size(); i++) {
			if (Q.get(i).intValue() == value) {
				return i;
			}
		}
		return -1;
	}
}