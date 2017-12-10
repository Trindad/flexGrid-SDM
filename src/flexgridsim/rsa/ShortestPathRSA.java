package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.WeightedGraph;


/**
 * The Class ModifiedShortestPath.
 * 
 * @author pedrom
 */
public class ShortestPathRSA implements RSA {
	private PhysicalTopology pt;
	private VirtualTopology vt;
	private ControlPlaneForRSA cp;
	private WeightedGraph graph;

	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt,
			ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
	}

	/**
	 * Instantiates a new modified shortest path.
	 */
	public ShortestPathRSA() {
	}

	public void flowArrival(Flow flow) {
		long id;
		int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());
		// Shortest-Path routing
		LightPath lps = MSP(graph, flow.getSource(),  flow.getDestination(),
				demandInSlots);
		if (lps != null){
			id = vt.createLightpath(lps.getLinks(), lps.getSlotList(), 0);
			if (id >= 0) {
				// Single-hop routing (end-to-end lightpath)
				cp.acceptFlow(flow.getID(), vt.getLightpath(id));
				return;
			}
		} else {
			cp.blockFlow(flow.getID());
		}
	}

	public void flowDeparture(Flow flow) {
	}

	// Dijkstra's algorithm to find shortest path from s to all other nodes
	/**
	 * Msp.
	 *
	 * @param G the g
	 * @param src the s
	 * @param dst the dst
	 * @param demand the demand
	 * @return the int[]
	 */
	public LightPath MSP(WeightedGraph G, int src, int dst, int demand) {
		final double[] dist = new double[G.size()]; // shortest known distance
													// from "s"
		final int[] pred = new int[G.size()]; // preceding node in path
		final boolean[] visited = new boolean[G.size()]; // all false initially
		HashMap<Integer, boolean[]> T = new HashMap<Integer, boolean[]>();
		//boolean[] freeSlots = new boolean[pt.getNumSlots()];
//		Arrays.fill(freeSlots, Boolean.TRUE);
		for (int i = 0; i < dist.length; i++) {
			pred[i] = -1;
			dist[i] = 1000000;
		}
		final int[] n0 = G.neighbors(0);
		for (int i = 0; i < dist.length; i++) {
			T.put(src, pt.getLink(src, n0[i]).getSpectrumCore(0));
		}
		dist[src] = 0;
		for (int i = 0; i < dist.length; i++) {
			final int next = minVertex(dist, visited);
			if (next >= 0) {
				visited[next] = true;

				// The shortest path to next is dist[next] and via pred[next].
				final int[] n = G.neighbors(next);
				for (int j = 0; j < n.length; j++) {
					final int v = n[j];
					final double d = dist[next] + G.getWeight(next, v);
					if (contiguousSlotsAvailable(pt.getLink(next, v).getSpectrumCore(0), demand)){
						if (dist[v] > d) {
							dist[v] = d;
							pred[v] = next;
							T.put(v, arrayAnd(T.get(next), pt.getLink(next, v).getSpectrumCore(0)));
						}
					}					
				}
			}
		}
		
		if (T.get(dst)!=null){
			ArrayList<Integer> path = new ArrayList<Integer>();
			int x = dst;
			while (x != src) {
				path.add(0, x);
				x = pred[x];
				// No path
				if (x == -1) {
					return null;
				}
			}
			path.add(0, src);
			int[] sp;
			sp = new int[path.size()-1];
			for (int i = 0; i < path.size(); i++) {
				sp[i] = pt.getLink( path.get(i), path.get(i+1)).getID();
			}
			int index = contiguousSlotsAvailableIndex(T.get(dst), demand);
			ArrayList<Slot> slotList = new ArrayList<Slot>();
			for (int j = index; j <= index+demand; j++) {
				slotList.add(new Slot(0, j));
			}
			return new LightPath(Long.MAX_VALUE, src, dst, sp, slotList, 0);
		} else {
			return null;
		}
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
	public static boolean contiguousSlotsAvailable(boolean[] array, int n) {
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
					return true;
			}
		}
		return false;
	}

	/**
	 * Contiguous slots available index.
	 *
	 * @param array the array
	 * @param n the n
	 * @return the int
	 */
	public static int contiguousSlotsAvailableIndex(boolean[] array, int n) {
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
	 * Finds, from the list of unvisited vertexes, the one with the lowest
	 * distance from the initial node.
	 * 
	 * @param dist
	 *            vector with shortest known distance from the initial node
	 * @param v
	 *            vector indicating the visited nodes
	 * @return vertex with minimum distance from initial node, or -1 if the
	 *         graph is unconnected or if no vertexes were visited yet
	 */
	public int minVertex(double[] dist, boolean[] v) {
		double x = Double.MAX_VALUE;
		int y = -1; // graph not connected, or no unvisited vertices
		for (int i = 0; i < dist.length; i++) {
			if (!v[i] && dist[i] < x) {
				y = i;
				x = dist[i];
			}
		}
		return y;
	}

	/**
	 * Retrieves the shortest path between a source and a destination node,
	 * within a weighted graph.
	 * 
	 * @param G
	 *            the weighted graph in which the shortest path will be found
	 * @param src
	 *            the source node
	 * @param dst
	 *            the destination node
	 * @param demand
	 *            size of the demand
	 * @return the shortest path, as a vector of integers that represent node
	 *         coordinates
	 */
//	public int[] getShortestPath(WeightedGraph G, int src, int dst, int demand) {
//		int x;
//		int[] sp;
//		ArrayList<Integer> path = new ArrayList<Integer>();
//		final int[] pred = MSP(G, src, dst, demand);
//		if (pred == null) {
//			return null;
//		}
//		x = dst;
//
//		while (x != src) {
//			path.add(0, x);
//			x = pred[x];
//			// No path
//			if (x == -1) {
//				return new int[0];
//			}
//		}
//		path.add(0, src);
//		
//		sp = new int[path.size()];
//		for (int i = 0; i < path.size(); i++) {
//			sp[i] = path.get(i);
//		}
//		return sp;
//	}

	/**
	 * Finds the shortest path between a source and a destination node.
	 * 
	 * @param pred
	 *            list of the destination node's predecessors
	 * @param src
	 *            the source node
	 * @param dst
	 *            the destination node
	 * @return the shortest path
	 */
	public int[] getShortestPath(int[] pred, int src, int dst) {
		int x;
		int[] sp;
		ArrayList<Integer> path = new ArrayList<Integer>();

		x = dst;

		while (x != src) {
			path.add(0, x);
			x = pred[x];
			// No path
			if (x == -1) {
				return new int[0];
			}
		}
		path.add(0, src);
		sp = new int[path.size()];
		for (int i = 0; i < path.size(); i++) {
			sp[i] = path.get(i);
		}
		return sp;
	}

	@Override
	public void runDefragmentantion() {
		// TODO Auto-generated method stub
		
	}

}
