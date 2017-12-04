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
import flexgridsim.util.ConnectedComponent;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.WeightedGraph;

/**
 * Paper: Crosstalk-aware cross-core virtual concatenation in spatial division multiplexing elastic optical networks
 * Authors: Zhao and Zhang
 * Published: September 2016
 * 
 * @author trindade
 *
 */
public class SCVCRCSA implements RSA{
	
	protected PhysicalTopology pt;
	protected VirtualTopology vt;
	protected ControlPlaneForRSA cp;
	protected WeightedGraph graph;
	
	public void simulationInterface(Element xml, PhysicalTopology pt,
			VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
	}

	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 3);
		
		if(kPaths.length >= 1)
		{
			boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
			
			int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());
			
			for (int k = 0; k < kPaths.length; k++) {
				
				int[] links = new int[kPaths[k].length - 1];
				
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
				}
	
				ArrayList<Slot> fittedSlotList = FirstFitPolicy(spectrum, links, demandInSlots);
				
				if (fitConnection(links, fittedSlotList, 0, flow)) {
					return;
				}
			}
		}
		
		cp.blockFlow(flow.getID());
	}
	
	/**
	 * Search to a core that has available slots and considering the cross-talk threshold
	 * @param links 
	 * @param spectrum 
	 * @return list of available slots
	 */
	private ArrayList<Slot> FirstFitPolicy(boolean[][] spectrum, int[] links, int demandInSlots) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 * @param links
	 * @param listOfSlots
	 * @param demandInSlots
	 * @param flow
	 * @return
	 */
	public boolean fitConnection(int[] links, ArrayList<Slot> slotList, int demandInSlots, Flow flow){
		
		
		return establishConnection(links, slotList, flow);
	}
	
	/**
	 * @param links
	 * @param slotList
	 * @param flow
	 * @return true if the connection was successfully established; false otherwise
	 */
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, Flow flow) {
		
		return true;
	}

	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
		
	}
}
