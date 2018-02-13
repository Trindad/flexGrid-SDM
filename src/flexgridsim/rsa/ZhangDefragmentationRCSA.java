package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VirtualTopology;

/**
 * 
 * @author trindade
 *
 */

public class ZhangDefragmentationRCSA extends DefragmentationRCSA{
	
	public int nConnectionDisruption = 0;
	public ArrayList<Flow> connectionDisruption = new ArrayList<Flow>();
	
	public Map<Long, Flow> allflows;
	public Map<Long, Flow> allFlowsToReroute;
	public Map<Flow, LightPath> accepted = new HashMap<Flow, LightPath>();

	
	public void copyStrutures(PhysicalTopology pt, VirtualTopology vt) {

		this.pt = new PhysicalTopology(pt);
		this.vt = new VirtualTopology(vt);
		this.graph = this.pt.getWeightedGraph();
	}
	
	protected void releaseResourcesAssigned(Map<Long, Flow> flows) {
		
		this.allflows = new HashMap<Long, Flow>(cp.getActiveFlows());
		this.allFlowsToReroute = new HashMap<Long, Flow>();
		
		for(Long key: flows.keySet()) {
			
			this.allFlowsToReroute.put(key, flows.get(key));
			this.allflows.remove(key);
			cp.removeFlowFromPT(flows.get(key), this.vt.getLightpath(flows.get(key).getLightpathID()), this.pt, this.vt);	
		}
		
		this.nConnectionDisruption = 0;
	}
	
	@SuppressWarnings("unused")
	private boolean isEqual(int []a, int []b) {
		
		int n = 0;
		
		for(int i = 0; i < a.length; i++) {
			
			for(int j = 0; j < b.length; j++) {
				
				if(a[i] == b[j]) {
					n++;
					break;
				}
			}
		}
		
		return n == a.length;
	}
	
	protected ArrayList<int[]>findKPaths(Flow flow) {
		
		this.setkShortestPaths(flow);
		
		return this.paths;
	}
	
	/**
	 * De-fragmentation based routing, core and spectrum assignment
	 */
	public void runDefragmentantion(Map<Long, Flow> flowsToReroute) {
		
		try {
			
			releaseResourcesAssigned(flowsToReroute);
			ArrayList<Flow> flows = new ArrayList<Flow>();
			
			for(Long key: flowsToReroute.keySet()) flows.add(flowsToReroute.get(key));
			
			flows.sort(Comparator.comparing(Flow::getRate));
			Collections.reverse(flows);
			
			//in descending (rate) order
			for(Flow f: flows) {
				
				this.flowArrival(f);
				this.changeFlowStatus(f, flowsToReroute);
			}
			
			BestEffortTrafficMigration bf = new BestEffortTrafficMigration(cp, this.pt, this.vt, flowsToReroute, this.allFlowsToReroute);
			Map<Long, Flow> flowsNotDependent = bf.runBestEffort();
			if(!flowsNotDependent.isEmpty()) updateControlPlane(flowsNotDependent);
		}
		catch(Exception e) {
			e.printStackTrace();
		}	
		
//		if(this.nConnectionDisruption >= 1) System.out.println(" disruption: "+this.nConnectionDisruption);
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
		this.nConnectionDisruption = 0;
		this.connectionDisruption.clear();
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
			
//			System.out.println("Connection reaccepted: "+flow);
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
	
	protected int smallestUsedSlotIndex(ArrayList<Slot> slots) {
		
		ArrayList<Integer> indices = new ArrayList<Integer>();
		
		for(int i = 0; i < slots.size(); i++) {
			indices.add(i);
		}
		
		indices.sort((a,b) -> slots.get(a).s - slots.get(b).s);
		
		return indices.get(0);
	}
	
	protected Slot maximumUsedSlotIndex(ArrayList<Slot> slots) {
		
		return slots.get(slots.size()-1);
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
		ArrayList<Slot> candidateMaximumUsedSlotIndex = new ArrayList<Slot>();
		ArrayList<ArrayList<Slot>> candidateSlotLists = new ArrayList< ArrayList<Slot> >();

		for(int[] pi: kPaths)
		{
			ArrayList<Slot> slotList = canBeProvided(flow, pi); 
			
			if(!slotList.isEmpty()) 
			{
				candidateMaximumUsedSlotIndex.add(maximumUsedSlotIndex(slotList));
				candidateSlotLists.add(slotList); 
				p.add(pi);
			}
		}
		if(!candidateMaximumUsedSlotIndex.isEmpty()) 
		{	
			int index = smallestUsedSlotIndex(candidateMaximumUsedSlotIndex);
			
			//pre-allocation
			if(establishConnection(p.get(index), candidateSlotLists.get(index), flow.getModulationLevel(), flow)) 
			{
				return;
			}
		}
		
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
		return;
	}

	@Override
	public void setTime(double time) {
		return;
	}
}
