package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.Modulations;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.WeightedGraph;
/**
 * The Class EnergyEfficientRMLSA.
 */
public class EnergyEfficientRMLSA implements RSA {
	protected PhysicalTopology pt;
	protected VirtualTopology vt;
	protected ControlPlaneForRSA cp;
	protected WeightedGraph graph;
	protected Element rsaXml;
	
	public void simulationInterface(Element xml, PhysicalTopology pt,
			VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
		this.rsaXml = xml;

	}

	public void flowArrival(Flow flow) {
		BestAllocation bestAllocation = energyEfficientPath(flow.getSource(), flow.getDestination(), flow.getRate());
		if (bestAllocation == null) {
			cp.blockFlow(flow.getID());
			return;
		}
//		if (establishConnection(flow, bestAllocation))
//			return;
		if (!vt.getLightpaths(flow.getSource(), flow.getDestination()).isEmpty()){
			if (establishConnection(flow, bestAllocation))
				return;
		} else {
			boolean groomSucessfull = false;
			for (Iterator<LightPath> iterator = vt.getLightpaths(flow.getSource(), flow.getDestination()).iterator(); iterator.hasNext();) {
				ArrayList<Slot> slotList = new ArrayList<Slot>();
				for (int i = bestAllocation.getBestSlot(); i <= bestAllocation.getBestDemandInSlots(); i++) {
					slotList.add(new Slot(0, i));
				}
				if (pt.canGroom(flow, slotList)){
					//cp.groomFlow(flow);
					groomSucessfull = true;
				}
			}
			if (!groomSucessfull){
				if (establishConnection(flow, bestAllocation)){
					return;
				}
			}
				
		}
		cp.blockFlow(flow.getID());
	}

	public void flowDeparture(Flow flow) {
		
	}
	
	/**
	 * Establish connection.
	 *
	 * @param flow the flow
	 * @param bestAllocation the best allocation
	 * @return true, if successful
	 */
	public boolean establishConnection(Flow flow, BestAllocation bestAllocation){		
		ArrayList<Slot> slotList = new ArrayList<Slot>();
		for (int i = bestAllocation.getBestSlot(); i <= bestAllocation.getBestSlot()+bestAllocation.getBestDemandInSlots()-1; i++) {
			slotList.add(new Slot(0, i));
		}
		long id = vt.createLightpath(bestAllocation.getLinks(), slotList, bestAllocation.getBestModulationLevel());
				
		if (id >= 0) {
			// Single-hop routing (end-to-end lightpath)
			LightPath lps = vt.getLightpath(id);
			flow.setSlotList(slotList);
			flow.setModulationLevel(bestAllocation.getBestModulationLevel());
			flow.setLinks(bestAllocation.getLinks());
			cp.acceptFlow(flow.getID(), lps);
			return true;
		}
		return false;
	}
	/**
	 * Energy efficient rmlsa.
	 *
	 * @param src the src
	 * @param dst the dst
	 * @param demand the demand
	 * @return the best allocation
	 */
	public BestAllocation energyEfficientPath(int src, int dst, double demand){
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, src, dst, 3);
//		for (int i=0; i<kPaths.length;i++){
//			for (int j=0;j<kPaths[i].length;j++){
//				System.out.print(kPaths[i][j]+" ");
//			}
//			System.out.println();
//		}
		double lowestMetricPC = Double.MAX_VALUE;
		BestAllocation bestAllocation = null;
		for (int i = 0; i < kPaths.length; i++) {
			boolean[] spectrum = null;
			if (kPaths[i].length > 1){
				spectrum = pt.getLink(kPaths[i][0], kPaths[i][1]).getSpectrumCore(0);
			} else {
				continue;
			}
			for (int j = 1; j < kPaths[i].length-1; j++) {
				spectrum = arrayAnd(spectrum, pt.getLink(kPaths[i][j], kPaths[i][j+1]).getSpectrumCore(0));
			}
			int maxModulationLevel = getMaxModulationFormat(kPaths[i]);
			for (int j = 0; j <= maxModulationLevel; j++) {
				int demandInSlots = (int) Math.ceil(demand / (double) Modulations.getBandwidth(j)) + 2;
				int firstSlot = contiguousSlotsAvailable(spectrum, demandInSlots);
				if (firstSlot >= 0){
					double metricPC = getMetricPC(kPaths[i], demandInSlots-2, j);
					if (metricPC < lowestMetricPC){
						lowestMetricPC = metricPC;
						bestAllocation = new BestAllocation(kPaths[i], firstSlot, demandInSlots, j);
					}
				}
			}
		}
		return bestAllocation;
	}
	
	/**
	 * Gets the metric pc.
	 *
	 * @param nodes the nodes
	 * @param numberOfDataSlots the number of data slots
	 * @param modulationLevel the modulation level
	 * @return the metric pc
	 */
	public double getMetricPC(int[] nodes, int numberOfDataSlots, int modulationLevel){
//		double PCoxc = 0;
//		for (int id: nodes) {
//			PCoxc += pt.getNodeDegree(id) * 85 + 150;
//		}
//		return (double)(numberOfDataSlots*Modulations.getPowerConsumption(modulationLevel))
//				+((numberOfDataSlots+2)/(pt.getNumSlots()))*(PCoxc+200*(nodes.length-1));
		return 1;
	}
	/**
	 * Gets the modulation format.
	 *
	 * @param path the path
	 * @return the modulation format
	 */
	public int getMaxModulationFormat(int[] path){
		int totalDistance = 0;
		for (int i = 0; i < path.length-1; i++) {
				totalDistance += this.pt.getLink(path[i], path[i+1]).getDistance();
		}
		return Modulations.getModulationByDistance(totalDistance);
	}
	
	/**
	 * Array and.
	 *
	 * @param array1 the array1
	 * @param array2 the array2
	 * @return the boolean[]
	 */
	public static boolean[] arrayAnd(boolean[] array1, boolean[] array2) {
		if (array1.length != array2.length) {
			throw (new IllegalArgumentException());
		}
		boolean[] result = new boolean[array1.length];
		for (int i = 0; i < array1.length; i++) {
			result[i] = array1[i] & array2[i];
		}
		return result;
	}
	
	/**
	 * Verify if the array of booleans has n contiguous true values
	 * 
	 * @param array
	 *            array to be verified
	 * @param n
	 *            number of contiguous slots
	 * @return return true in case it has n contiguous slots and false in case
	 *         it doesnt
	 */
	public static int contiguousSlotsAvailable(boolean[] array, int n) {
		int j;
		for (int i = 0; i < array.length; i++) {
			if (array[i]) {
				for (j = i; j < i + n && j < array.length; j++) {
					if (!array[j]) {
						i = j;
						break;
					}
				}
				if (j == i + n)
					return i;
			}
		}
		return -1;
	}
	
	class BestAllocation {
		private int[] bestPath;
		private int bestSlot;
		private int bestDemandInSlots;
		private int bestModulationLevel;
		public BestAllocation(int[] bestPath, int bestSlot,
				int bestDemandInSlots, int bestModulationLevel) {
			super();
			this.bestPath = bestPath;
			this.bestSlot = bestSlot;
			this.bestDemandInSlots = bestDemandInSlots;
			this.bestModulationLevel = bestModulationLevel;
		}
		public int[] getLinks(){
			int[] links = new int[bestPath.length-1];
			for (int i = 0; i < bestPath.length-1; i++) {
				links[i] = pt.getLink(bestPath[i], bestPath[i+1]).getID();
			}
			return links;
		}
		public int[] getBestPath() {
			return bestPath;
		}
		public int getBestSlot() {
			return bestSlot;
		}
		public int getBestDemandInSlots() {
			return bestDemandInSlots;
		}
		public int getBestModulationLevel() {
			return bestModulationLevel;
		}
	}
}
