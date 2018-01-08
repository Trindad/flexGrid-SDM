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
	private Map<Flow, LightPath> listOfFlows = new HashMap<Flow, LightPath>();

	
	public void copyStrutures() {

		this.pt = new PhysicalTopology(pt);
		this.vt = new VirtualTopology(vt);
		this.graph = this.pt.getWeightedGraph();
	}
	
	protected void releaseResourcesAssigned(Map<Long, Flow> flows) {
		
		this.allflows = new HashMap<Long, Flow>(cp.getActiveFlows());
		
		for(Long key: flows.keySet()) {
			
			//System.out.println("before: " + cp.getMappedFlows().containsKey(flows.get(key)) + " size: " + cp.getMappedFlows().size());
			this.allflows.remove(key);
			this.vt.removeLightPath((int)flows.get(key).getLightpathID());
			//System.out.println("after: " + cp.getMappedFlows().containsKey(flows.get(key)) + " size: " + cp.getMappedFlows().size());
			
			int []links = flows.get(key).getLinks();
			for(int i = 0; i < links.length; i++) {
				pt.getLink(links[i]).updateCrosstalk();
			}
		}
		
		
	}
	
	protected ArrayList<int[]>findKPaths(Flow flow) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 2);
		ArrayList<int[]> p = new ArrayList<int[]> ();

		if(kPaths.length >= 1)
		{
			for (int k = 0; k < kPaths.length; k++) {
				
				int[] links = new int[kPaths[k].length - 1];


				for (int j = 0; j < kPaths[k].length - 1; j++) {
					
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
				}
				p.add(links);
			}
		}
		
		return p;
	}
	
	/**
	 * De-fragmentation based routing, core and spectrum assignment
	 */
	public void runDefragmentantion(Map<Long, Flow> flows) {
		
		releaseResourcesAssigned(flows);
		
		//in descending order
		for(Long key: flows.keySet()) {
			
			this.flowArrival(flows.get(key));
		}
		
		BestEffortTrafficMigration bf = new BestEffortTrafficMigration(cp, pt, vt, flows, listOfFlows, connectionDisruption);
		flows = bf.run();
		
		updateControlPlane(flows);
		
	}
	
	
	private void updateControlPlane(Map<Long, Flow> flows) {
		
		cp.updateControlPlane(pt, vt);
		
		for(Flow f: connectionDisruption)
		{
			if(!cp.blockFlow(f.getID(), true)) {
				System.out.println("Error while blocking");
			}
			else
			{
				for(Long key: flows.keySet()) {
					
					if(f.getID() == flows.get(key).getID()) {
						flows.remove(key);
						break;
					}
				}
				
				System.out.println("Connection blocked:: "+f);
			}
		}
		
		
		
//		for(Long key: flows.keySet()) {
//			
//			cp.reacceptFlow(flows.get(key).getID(), listOfFlows.get(flows.get(key)));
//		}
//		
		connectionDisruption.clear();
	}
	
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, int modulation, Flow flow) {
		
		if(links == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		long id = vt.createLightpath(links, slotList ,modulation);
		
		if (id >= 0) 
		{
			flow.setLinks(links);
			flow.setSlotList(slotList);
			flow.setModulationLevel(modulation);
			
			LightPath lps = vt.getLightpath(id);
			flow.setLinks(links);
			flow.setLightpathID(id);
			
			listOfFlows.put(flow, lps);
			
			for (int j = 0; j < links.length; j++) {
				
	            pt.getLink(links[j]).reserveSlots(slotList);
	        }
			
			//update cross-talk
			updateCrosstalk(links);
			
			System.out.println("Connection reaccepted:"+flow);
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
	
	protected void orderedMostfrequencyUsed(ArrayList<Slot> slots, Map<Long, Flow> flows, ArrayList<int[]> setOfPaths, Flow flow, ArrayList< ArrayList<Slot> > candidateSlotLists) {
		
		slots.sort((a,b) -> slotFrequency(flows, a) - slotFrequency(flows, b));
		int index = 0;
		
		for(ArrayList<Slot> s: candidateSlotLists) {
			
			if(s.contains(slots.get(0))) 
			{
				flow.setLinks(setOfPaths.get(index));
				flow.setSlotList(s);
				return;
			}
			
			index++;
		}
	}
	
	protected Slot mostfrequencyUsed(ArrayList<Slot> slots, Map<Long, Flow> flows) {
		
		slots.sort((a,b) -> slotFrequency(flows, b) - slotFrequency(flows, a));
		
		return slots.get(0);
	}

	protected ArrayList<Slot> canBeProvided(Flow flow, int[] links) {
		
		boolean [][]spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
//		System.out.println(flow.getSource() + " " + flow.getDestination()+" "+links.length);
		
		for(int i = 0; i < links.length; i++) {
			int src = pt.getLink(links[i]).getSource(), dst = pt.getLink(links[i]).getDestination();
			this.bitMap(pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
		}
		
		return preFitConnection(flow, spectrum, links);
	}
	
	public ArrayList<Slot> preFitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
				
		for (int i = spectrum.length-1; i >= 0; i--) {
			
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
		ArrayList<ArrayList<Slot>> candidateSlotLists = new ArrayList<ArrayList<Slot>>();

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
		
		this.orderedMostfrequencyUsed(candidateMostFrequencySlot, allflows, p, flow, candidateSlotLists);
		
		if(!this.establishConnection(flow.getLinks(), flow.getSlotList(), flow.getModulationLevel(), flow)) 
		{
//			System.out.println("Connection Disruption occurred "+flow);
			connectionDisruption.add(flow);
			this.nConnectionDisruption++;
		}
		
	}
	
	public int getConnectionDisruption() {
		return nConnectionDisruption;
	}
	
	@Override
	public void flowDeparture(Flow flow) {

		if(!flow.isAccepeted()) return;
	
		int []links = flow.getLinks();
		
		for(int i = 0; i < links.length; i++) {
			pt.getLink(links[i]).updateCrosstalk();
		}
	}

	@Override
	public void runDefragmentantion() {
		// TODO Auto-generated method stub
		
	}
}
