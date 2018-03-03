package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;
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
@SuppressWarnings("unused")
public class SCVCRCSA implements RSA{
		
	public PhysicalTopology pt;
	public VirtualTopology vt;
	public ControlPlaneForRSA cp;
	public WeightedGraph graph;
	public ArrayList<int []> paths;
	public int totalSlotsAvailable = 0;
	public int kPaths = 3;
	
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
	public int chooseModulationFormat(int rate, int []links) {
		
		int totalLength = 0;
		
		for(int i : links) {
			
			totalLength += (pt.getLink(i).getDistance());
		}
		
		//invalid path
		if(totalLength > ModulationsMuticore.maxDistance[0]) {
			return -1;
		}
		
		int modulationLevel =  ModulationsMuticore.getModulationByDistance(totalLength);
		
		modulationLevel = rate < ModulationsMuticore.subcarriersCapacity[modulationLevel] &&  modulationLevel > 0 ? modulationLevel-1 : modulationLevel;
	
		modulationLevel = rate < ModulationsMuticore.subcarriersCapacity[0] ? 0 : modulationLevel;

		return modulationLevel;
	}
	
	public boolean[][]bitMapAll(int []links) {
	
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
		
		for(int i : links) {
			bitMap(pt.getLink(i).getSpectrum(), spectrum, spectrum);
		}
		
		this.totalSlotsAvailable = 0;
		for(int i = 0; i < spectrum.length; i++) {
			for(int j = 0; j < spectrum[i].length; j++) {
				this.totalSlotsAvailable += spectrum[i][j] ? 1 : 0;
			}
		}
		
		return spectrum;
	}
	
	protected boolean runRCSA(Flow flow) {
		
		setkShortestPaths(flow);

		for(int i = 0; i < this.paths.size(); i++) {
			
			if(fitConnection(flow, bitMapAll(this.paths.get(i)), this.paths.get(i))) {
					this.paths.clear();
//					System.out.println("ACCEPTED: "+flow);
					return true;
			}
		}
//		System.out.println("BLOCKED:"+ flow);
		this.paths.clear();
		
		return false;
	}

	public ArrayList<int[]> getkShortestPaths() {
		
		return this.paths;
	}

	public void setkShortestPaths(Flow flow) {
		
		this.paths  = new ArrayList<int []>();
		org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge> kShortestPaths1 = new org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge>(pt.getGraph(), kPaths);
		List< GraphPath<Integer, DefaultWeightedEdge> > KPaths = kShortestPaths1.getPaths( flow.getSource(), flow.getDestination() );
			
		if(KPaths.size() >= 1)
		{
			for (int k = 0; k < KPaths.size(); k++) {
				
				List<Integer> listOfVertices = KPaths.get(k).getVertexList();
				int[] links = new int[listOfVertices.size()-1];
				
				for (int j = 0; j < listOfVertices.size()-1; j++) {
					
					links[j] = pt.getLink(listOfVertices.get(j), listOfVertices.get(j+1)).getID();
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
		
		if(runRCSA(flow)) 
		{
			return;
		}
		
		cp.blockFlow(flow.getID());
	}
	
	public void printSpectrum(boolean [][]spectrum) {
		
		for (int u = 0; u < spectrum.length; u++) {
			for (int w = 0; w < spectrum[u].length; w++) System.out.print(" "+spectrum[u][w]);
			System.out.println();
		}
		System.out.println("-----------");
	}

	
	public void bitMap(boolean[][] s1, boolean[][] s2, boolean[][] result) {
//			printSpectrum(s1);
//			printSpectrum(s2);
		for (int i = 0; i < result.length; i++) {
			
			for (int j = 0; j < result[i].length; j++) {
				result[i][j] = s1[i][j] && s2[i][j];
			}
		}
	}
	

	/**
	 * Search to a core that has available slots and considering the cross-talk threshold
	 * @param links 
	 * @param spectrum 
	 * @return list of available slots
	 */
	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<ArrayList<Slot>> setOfSlots = new ArrayList<ArrayList<Slot>> ();
		
		for(int i = 0; i < spectrum.length ; i++) {
			
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int j = 0; j < spectrum[i].length; j++) {	
				
				if(spectrum[i][j] == true) 
				{
					temp.add( new Slot(i,j) );
				}
				else {
					
					temp.clear();
					if(Math.abs(spectrum[i].length-j) < demandInSlots) break;
				}
				
				if(temp.size() == demandInSlots) {
					
					if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {
						setOfSlots.add(new ArrayList<Slot>(temp));
						break;
					}
					
					temp.remove(0);
				}
			}
		}
		
		
		if(!setOfSlots.isEmpty()) {
			
			setOfSlots.sort( (a , b) -> {
				int diff = a.get(0).s - b.get(0).s;
				
				if(diff != 0) {
					return diff;
				}
				
				return ( b.get(0).c - a.get(0).c );
			});
			
			return setOfSlots.get(0);		
		}
	    
		return new ArrayList<Slot>();
	}
	
	public ArrayList<Slot> canBeFitConnection(Flow flow, int[]links, boolean [][]spectrum, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int modulation = chooseModulationFormat(rate, links);
		
		while(modulation >= 0) {
			
			double requestedBandwidthInGHz = ( (double)rate / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
			fittedSlotList = FirstFitPolicy(flow, spectrum, links, demandInSlots, modulation);
			
			if(fittedSlotList.size() == demandInSlots) {
					
				if(!flow.isMultipath()) 
				{
					flow.setModulationLevel(modulation);
				}
				else 
				{
					flow.addModulationLevel(modulation);
				}
				
				return fittedSlotList;
				
			}
			
			modulation--;
		}
		
		
		return new ArrayList<Slot>();
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
				
		fittedSlotList  = canBeFitConnection(flow, links, spectrum, flow.getRate());
		
		if(!fittedSlotList.isEmpty()) {
			
			if(establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow)) {
				return true;
			}
		}
		
		
		return false;
	}
	
	
	public ArrayList<Slot> searchSlotList(Flow flow, boolean [][]spectrum, int[] links) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
				
			
		fittedSlotList  = canBeFitConnection(flow, links, spectrum, flow.getRate());
		
		if(!fittedSlotList.isEmpty()) 
		{
			return fittedSlotList;
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
			flow.setCore(slotList.get(0).c);
			flow.setModulationLevel(modulation);
			flow.setAccepeted(true);

			cp.acceptFlow(flow.getID(), lps);
			
			//update cross-talk
//			updateCrosstalk(links, flow);
	
			return true;
		} 
		else 
		{
			return false;
		}
	}
	
	public void updateCrosstalk(int []links, Flow flow) {

		for(int i = 0; i < links.length; i++) {
			this.pt.getLink(links[i]).updateCrosstalk(flow.getSlotList(),  ModulationsMuticore.subcarriersCapacity[flow.getModulationLevel()]);
		}
	}
	
	public void updateCrosstalk(int []links, ArrayList<Slot> slotList, int db) {

		for(int i = 0; i < links.length; i++) {
			pt.getLink(links[i]).updateCrosstalk(slotList,  db);
		}
	}

	
	public void flowDeparture(Flow flow) {
		
		if(!flow.isAccepeted()) return;
	
		removeCrosstalk(flow.getLinks(), flow);
	}

	protected void removeCrosstalk(int[] links, Flow flow) {
		
		for(int l : links) {
			this.pt.getLink(l).resetCrosstalk(flow.getSlotList());
        }
	}

	public boolean runRCSA(Flow flow, int[] oldLinks, ArrayList<Slot> oldSlotList) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 4);

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
				
				ArrayList<Slot> slotList = searchSlotList(flow, spectrum, links);
				
				if(!slotList.isEmpty()) 
				{
					if(!isEqual(links, oldLinks)) {
						return establishConnection(links, slotList, flow.getModulationLevel(), flow);
					}
					
					if(!isEqual(slotList, oldSlotList)) {
						return establishConnection(links, slotList, flow.getModulationLevel(), flow);
					}
				}
			}
		}
		
		return false;
		
	}

	private boolean isEqual(int[] links, int[] oldLinks) {

		return links.toString().equals(oldLinks.toString());
	}

	private boolean isEqual(ArrayList<Slot> s1, ArrayList<Slot> s2) {
		
		if(s1.get(0).c != s2.get(0).c) {
			
			return false;
		}
		else if(s1.toString().equals(s2.toString())) 
		{
			return true;
		}
		
		return false;
	}

}

