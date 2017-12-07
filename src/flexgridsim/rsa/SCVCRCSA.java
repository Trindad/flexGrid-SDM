package flexgridsim.rsa;

import java.util.ArrayList;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
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
	
	protected boolean[][] initMatrix(boolean[][] m, int r, int c) {
		
		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				m[i][j] = true;
			}
		}
		
		return m;
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
				
				spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
				
				int[] links = new int[kPaths[k].length - 1];
				
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
					bitMap(pt.getLink(kPaths[k][j], kPaths[k][j+1]).getSpectrum(), spectrum, spectrum);
				}
				
				ArrayList<Slot> slotList = fitConnection(spectrum, links, demandInSlots, 0);
				
		    	if(establishConnection(links, slotList, 0, flow)) {
					return;
				}
			}
		}
		
		cp.blockFlow(flow.getID());
	}
	
	protected void bitMap(boolean[][] s1, boolean[][] s2, boolean[][] res) {
			
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[0].length; j++) {
				res[i][j] = s1[i][j] & s2[i][j];
			}
		}
	}
	

	/**
	 * Search to a core that has available slots and considering the cross-talk threshold
	 * @param links 
	 * @param spectrum 
	 * @return list of available slots
	 */
	private ArrayList<Slot> FirstFitPolicy(boolean []spectrum, int core, int[] links, int demandInSlots) {
		
		ArrayList<Slot> setOfSlots = new ArrayList<Slot>();
		
		if (spectrum.length >= demandInSlots) {
			
			for(int i = 0; i < spectrum.length; i++) {
				
				if(spectrum[i] == true) {
					
					setOfSlots.add( new Slot(core,i) );
				}
				else
				{
					setOfSlots.clear();
				}
			}
	    }
		
		return setOfSlots;
	}

	/**
	 * 
	 * @param availableSlosts
	 * @param links
	 * @param demandInSlots
	 * @param modulation
	 * @return fittedSlotList
	 */
	public ArrayList<Slot> fitConnection(boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {

		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		double xt = pt.getSumOfMeanCrosstalk(links);//returns the sum of cross-talk
		
		if(xt < 1) {
			
			for (int i = 0; i < spectrum.length; i++) {
				
				fittedSlotList = this.FirstFitPolicy(spectrum[i], i, links, demandInSlots);
				
				if(fittedSlotList.size() == demandInSlots) {
					break;
				}
				
				fittedSlotList.clear();
			}
		}
		
		return fittedSlotList;
	}
	
	/**
	 * 
	 * @param links
	 * @param slotList
	 * @param modulation
	 * @param flow
	 * @return
	 */
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, int modulation, Flow flow) {
		
		if(links == null) System.out.println("ERROR LINKS");
		long id = vt.createLightpath(links, slotList ,0);
		
		if (id >= 0) 
		{
			LightPath lps = vt.getLightpath(id);
			
			flow.setLinks(links);
			flow.setSlotList(slotList);
			cp.acceptFlow(flow.getID(), lps);
			
			return true;
		} 
		else 
		{
			return false;
		}
	}

	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
		
	}
}
