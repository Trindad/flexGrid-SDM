package flexgridsim.rsa;

import java.util.ArrayList;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
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
	protected ArrayList<int []> paths;
	
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
	 * 
	 * @param flow
	 * @param links
	 * @return
	 */
	protected int chooseModulationFormat(Flow flow, int []links) {
		
		int totalLength = 0;
		
		for(int i = 0; i < links.length; i++) {
			
			totalLength += (pt.getLink(links[i]).getDistance());
		}
		
		int modulationLevel = ModulationsMuticore.getModulationByDistance(totalLength);
		
		return modulationLevel;
	}
	
	protected boolean runRCSA(Flow flow) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 3);

		if(kPaths.length >= 1)
		{
			boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
			
			for (int k = 0; k < kPaths.length; k++) {
				
				spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());

				int[] links = new int[kPaths[k].length - 1];
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
					bitMap(pt.getLink(kPaths[k][j], kPaths[k][j+1]).getSpectrum(), spectrum, spectrum);
				}
				
				if( fitConnection(flow, spectrum, links) == true) {
					return true;
				}
			}
		}
		
		return false;
	}

	public ArrayList<int[]> getkShortestPaths() {
		
		return this.paths;
	}

	public void setkShortestPaths(Flow flow) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 2);
		this.paths = new ArrayList<int[]> ();

		if(kPaths.length >= 1)
		{
			for (int k = 0; k < kPaths.length; k++) {
				
				int[] links = new int[kPaths[k].length - 1];


				for (int j = 0; j < kPaths[k].length - 1; j++) {
					
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
				}

				this.paths.add(links);
			}
		}
	}

	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {
		
		if(runRCSA(flow)) {
			return;
		}
		
		System.out.println("Connection blocked:"+flow);
		cp.blockFlow(flow.getID());
	}
	
	protected void printSpectrum(boolean [][]spectrum) {
		
		for (int u = 0; u < spectrum.length; u++) {
			for (int w = 0; w < spectrum[u].length; w++) System.out.print(" "+spectrum[u][w]);
			System.out.println();
		}
		System.out.println("-----------");
	}

	
	protected void bitMap(boolean[][] s1, boolean[][] s2, boolean[][] res) {
//		printSpectrum(s1);
//		printSpectrum(s2);
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
	protected ArrayList<Slot> FirstFitPolicy(boolean []spectrum, int core, int[] links, int demandInSlots) {
		
		ArrayList<Slot> setOfSlots = new ArrayList<Slot>();
		
		if (spectrum.length >= demandInSlots) {

			for(int i = 0; i < spectrum.length; i++) {
				
				if(spectrum[i] == true) {
					
					setOfSlots.add( new Slot(core,i) );
				}
				else
				{
					setOfSlots.clear();
					
					if(Math.abs(i-spectrum.length) < demandInSlots) return setOfSlots;
				}
				
				if(setOfSlots.size() == demandInSlots) return setOfSlots;
			}
	    }
		
		return setOfSlots;
	}
	
	
	/**
	 * Search to a core that has available slots and considering the cross-talk threshold
	 * @param links 
	 * @param spectrum 
	 * @return list of available slots
	 */
	protected ArrayList<Slot> LastFitPolicy(boolean []spectrum, int core, int[] links, int demandInSlots) {
		
		ArrayList<Slot> setOfSlots = new ArrayList<Slot>();
		
		if (spectrum.length >= demandInSlots) {

			for(int i = spectrum.length-1; i >= 0 ; i--) {
				
				if(spectrum[i] == true) {
					
					setOfSlots.add( new Slot(core,i) );
				}
				else
				{
					setOfSlots.clear();
				}
				
				if(setOfSlots.size() == demandInSlots) return setOfSlots;
			}
	    }
		
		return setOfSlots;
	}
	
	/**
	 * 
	 * @param flow
	 * @param links
	 * @param spectrum
	 * @param core
	 * @return
	 */
	public ArrayList<Slot> canBeFitConnection(Flow flow, int[]links, boolean []spectrum, int core, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		double xt = 0.0f;
		int modulation = chooseModulationFormat(flow, links);
		
		while(modulation >= 0)
		{
			double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
			int demandInSlots = (int) Math.ceil((double)rate / subcarrierCapacity);
			
			xt = pt.getSumOfMeanCrosstalk(links, core);//returns the sum of cross-talk	
			
			if(xt == 0 || (xt < ModulationsMuticore.inBandXT[modulation]) ) {

				fittedSlotList = this.FirstFitPolicy(spectrum, core, links, demandInSlots);
				
				if(!fittedSlotList.isEmpty()) {
					
					if(fittedSlotList.size() == demandInSlots) {
						
						if(!flow.isMultipath()) flow.setModulationLevel(modulation);
						else flow.addModulationLevel(modulation);
						return fittedSlotList;
					}
				}
				
				fittedSlotList.clear();
			}
			
			modulation--;
		}
		
		fittedSlotList.clear();
		return fittedSlotList;
	}

	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @return
	 */
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
				
		for (int i = spectrum.length-1; i >= 0; i--) {
			
			fittedSlotList  = canBeFitConnection(flow, links, spectrum[i], i, flow.getRate());
			
			if(!fittedSlotList.isEmpty()) return establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow);
			
		}
		
		return false;
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
		
		if(links == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		long id = vt.createLightpath(links, slotList ,flow.getModulationLevel());
		
		if (id >= 0) 
		{
			LightPath lps = vt.getLightpath(id);
			flow.setLinks(links);
			flow.setLightpathID(id);
			flow.setSlotList(slotList);
			flow.setModulationLevel(modulation);

			cp.acceptFlow(flow.getID(), lps);
			
			//update cross-talk
			updateCrosstalk(links);
			
			System.out.println("Connection accepted:"+flow);
			return true;
		} 
		else 
		{
			return false;
		}
	}
	
	public void updateCrosstalk(int []links) {
		
		for(int i = 0; i < links.length; i++) {
			pt.getLink(links[i]).updateCrosstalk();
		}
	}

	@Override
	public void flowDeparture(Flow flow) {
		
		if(!flow.isAccepeted()) return;
	
		int []links = flow.getLinks();
		
		for(int i = 0; i < links.length; i++) {
			pt.getLink(links[i]).updateCrosstalk();
		}
	}

}