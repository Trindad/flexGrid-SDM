package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VirtualTopology;
import flexgridsim.util.KShortestPaths;

/**
 * 
 * @author trindade
 *
 */

public class ZhangDefragmentationRCSA extends DefragmentationRCSA{
	
	private int nConnectionDisruption = 0;
	private ArrayList<Flow> connectionDisruption = new ArrayList<Flow>();
	
	private Map<Long, Flow> allflows;
	private Map<Flow, LightPath> accepted = new HashMap<Flow, LightPath>();

	
	public void copyStrutures(PhysicalTopology pt, VirtualTopology vt) {

		this.pt = new PhysicalTopology(pt);
		this.vt = new VirtualTopology(vt);
		this.graph = this.pt.getWeightedGraph();
	}
	
	protected void releaseResourcesAssigned(Map<Long, Flow> flows) {
		
		this.allflows = new HashMap<Long, Flow>(cp.getActiveFlows());
		
		for(Long key: flows.keySet()) {
			this.allflows.remove(key);
			System.out.println("Removing " + flows.get(key).getLightpathID());
			cp.removeFlowFromPT(flows.get(key), this.vt.getLightpath(flows.get(key).getLightpathID()), this.pt, this.vt);	
		}
		
		this.nConnectionDisruption = 0;
	}
	
	protected ArrayList<int[]>findKPaths(Flow flow) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(this.graph, flow.getSource(), flow.getDestination(), 2);
		ArrayList<int[]> p = new ArrayList<int[]>();

		if(kPaths.length >= 1)
		{
			for (int k = 0; k < kPaths.length; k++) {
				
				int[] links = new int[kPaths[k].length - 1];

				for (int j = 0; j < kPaths[k].length - 1; j++) {
					
					links[j] = this.pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
				}
				p.add(links);
			}
		}
		
		return p;
	}
	
	/**
	 * De-fragmentation based routing, core and spectrum assignment
	 */
	public void runDefragmentantion(Map<Long, Flow> flowsToReroute) {
		
		try {
			
			releaseResourcesAssigned(flowsToReroute);
			ArrayList<Flow> flows = new ArrayList<Flow>();
			
			for(Long key: flowsToReroute.keySet()) flows.add(flowsToReroute.get(key));
			
//			flows.sort(Comparator.comparing(Flow::getRate));
//			Collections.reverse(flows);
			//in descending order
			for(Flow f: flows) {
				
				this.flowArrival(f);
				this.changeFlowStatus(f, flowsToReroute);
			}
			
			BestEffortTrafficMigration bf = new BestEffortTrafficMigration(cp, this.pt, this.vt,flowsToReroute);
			flowsToReroute = bf.runBestEffort();
			updateControlPlane(flowsToReroute);
		}
		catch(Exception e) {
			e.printStackTrace();
		}	
		
		if(this.nConnectionDisruption >= 1) System.out.println(" disruption: "+this.nConnectionDisruption);
	}
	
	
	private void changeFlowStatus(Flow f, Map<Long, Flow> flows) {
		
		for(Long key: flows.keySet()) {
			
			if(flows.get(key).equals(f)) {
				
				flows.remove(key);
				flows.put(key, f);
				return;
			}
		}
	}

	private void updateControlPlane(Map<Long, Flow> flows) {
		cp.updateControlPlane(this.pt, this.vt, flows);
		
		connectionDisruption.clear();
	}
	
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, int modulation, Flow flow) {
		
		if(links == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		long id = this.vt.createLightpath(links, slotList ,modulation);
		
		if (id >= 0) 
		{
			
			flow.setLinks(links);
			flow.setSlotList(slotList);
			flow.setModulationLevel(modulation);
			
			LightPath lps = this.vt.getLightpath(id);
			flow.setLinks(links);
			flow.setLightpathID(id);
			
			accepted.put(flow, lps);
			
			for (int j = 0; j < links.length; j++) {
				
	            this.pt.getLink(links[j]).reserveSlots(slotList);
	            this.pt.getLink(links[j]).updateNoise(lps.getSlotList(), flow.getModulationLevel());
	        }
			
			//update cross-talk
			for(int i = 0; i < links.length; i++) {
				this.pt.getLink(links[i]).updateCrosstalk();
			}
			
			System.out.println("Connection reaccepted: "+flow);
			return true;
		} 
		

		return false;
	}
	
	
	public int slotFrequency(Map<Long, Flow> flows, Slot slot) {
		
		int n = 0;
		
		for(Long key: flows.keySet()) 
		{
			
			if(flows.get(key).getSlotList().contains(slot)) {
				n++;
			}
		}
		
		return n;
		
	}
	
	protected int orderedMostfrequencyUsed(ArrayList<Slot> slots, Map<Long, Flow> flows, ArrayList<int[]> setOfPaths, 
			Flow flow, ArrayList< ArrayList<Slot> > candidateSlotLists, ArrayList<Slot> slotList) {
		
		slots.sort((a,b) -> slotFrequency(flows, a) - slotFrequency(flows, b));
		int index = 0;
		
		for(ArrayList<Slot> s: candidateSlotLists) {
			
			if(s.contains(slots.get(0))) 
			{
				flow.setLinks(setOfPaths.get(index));
				slotList.addAll(s);
				return index;
			}
			
			index++;
		}
		
		return index;
	}
	
	protected Slot mostfrequencyUsed(ArrayList<Slot> slots, Map<Long, Flow> flows) {
		
		slots.sort((a,b) -> slotFrequency(flows, b) - slotFrequency(flows, a));
		
		return slots.get(0);
	}

	protected ArrayList<Slot> canBeProvided(Flow flow, int[] links) {
		
		boolean [][]spectrum = new boolean[this.pt.getCores()][this.pt.getNumSlots()];
		spectrum = initMatrix(spectrum, this.pt.getCores(),this.pt.getNumSlots());

		for(int i = 0; i < links.length; i++) {
			
			int src = this.pt.getLink(links[i]).getSource();
			int dst = this.pt.getLink(links[i]).getDestination();
			
			this.bitMap(this.pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
		}
		
		return preFitConnection(flow, spectrum, links);
	}
	
	public ArrayList<Slot> preFitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		printSpectrum(spectrum);
				
		for (int i = 0; i < spectrum.length; i++) {
			
			fittedSlotList  = canBeFitConnection(flow, links, spectrum[i], i, flow.getRate());
			
			if(!fittedSlotList.isEmpty()) return fittedSlotList;
			
		}
		
		return fittedSlotList;
	}

	@Override
	public void flowArrival(Flow flow) {
		
		ArrayList<int[]> kPaths = findKPaths(flow);
		ArrayList<int[]> p = new ArrayList<int[]>();
		ArrayList<Slot> candidateMostFrequencySlot = new ArrayList<Slot>();
		ArrayList<ArrayList<Slot>> candidateSlotLists = new ArrayList< ArrayList<Slot> >();

		for(int[] pi: kPaths)
		{
			ArrayList<Slot> slotList = canBeProvided(flow, pi); 
			
			if(!slotList.isEmpty()) 
			{
				candidateMostFrequencySlot.add(mostfrequencyUsed(slotList, allflows));
				candidateSlotLists.add(slotList); 
				p.add(pi);
			}
		}
		
		System.out.println("old: "+ flow.getSlotList()+" n: "+candidateMostFrequencySlot.size()+ ": "+kPaths.size());

		if(!candidateMostFrequencySlot.isEmpty()) {
			
			while(candidateMostFrequencySlot.size() >= 1) {
				
				ArrayList<Slot> slotList = new ArrayList<Slot>();
				int i = this.orderedMostfrequencyUsed(candidateMostFrequencySlot, allflows, p, flow, candidateSlotLists, slotList);
				
				if(this.establishConnection(flow.getLinks(), slotList, flow.getModulationLevel(), flow)) 
				{
					System.out.println("new "+flow.getID()+" "+flow.getSlotList());
					return;
				}
				
				candidateMostFrequencySlot.remove(i);
				candidateSlotLists.remove(i);
				p.remove(i);
			}
		}
		
		System.out.println("not-accepted "+flow);
		connectionDisruption.add(flow);
		flow.setConnectionDisruption(true);
		this.nConnectionDisruption++;
		
	}
	
	public int getConnectionDisruption() {
		return this.nConnectionDisruption;
	}
	
	@Override
	public void flowDeparture(Flow flow) {

		if(!flow.isAccepeted()) return;
	
		int []links = flow.getLinks();
		
		for(int i = 0; i < links.length; i++) {
			this.pt.getLink(links[i]).updateCrosstalk();
		}
	}

	@Override
	public void runDefragmentantion() {
		// TODO Auto-generated method stub
		
	}
}
