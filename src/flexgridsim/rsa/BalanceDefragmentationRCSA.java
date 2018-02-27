package flexgridsim.rsa;

import java.util.ArrayList;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;
import flexgridsim.rsa.ZhangDefragmentationRCSA;

public class BalanceDefragmentationRCSA extends ZhangDefragmentationRCSA{

	protected double []fi;

	public void setFragmentationIndexOfEachLink(double []fi) {
		
		this.fi = fi;
	}
	
	public void flowArrival(Flow flow) {
		
		ArrayList<int[]> kPaths = findKPaths(flow);//find K-Shortest paths using Dijkstra
		ArrayList<Integer> indices = orderKPaths(kPaths);//sort paths by the fragmentation index from each link

		for(int i: indices)
		{
			int []links = kPaths.get(i);
			boolean [][]spectrum = bitMapAll(links);
			
			if(fitConnection(flow, spectrum, links)) {
				this.activeFlows.put(flow.getID(), flow);
				updateCrosstalk();
				return;
			}
		}
		System.out.println(flow.getRate());
		this.connectionDisruption.add(flow);
		flow.setConnectionDisruption(true);
		this.nConnectionDisruption++;
	}
	
	private ArrayList<Slot> getMatchSlots(ArrayList<Slot> s1, ArrayList<Slot> s2) {
		
		ArrayList<Slot> slots = new ArrayList<Slot>();
		if(s1.get(0).c != s2.get(0).c) return slots;
		
		for(Slot i: s1) 
		{
			for(Slot j: s2) 
			{
				if(i.s == j.s && !slots.contains(i)) {
					slots.add(i);
				}
			}
		}
		
		return slots;
	}

	public boolean CrosstalkIsAcceptable(Flow flow, int[] links, ArrayList<Slot> slotList, double db) {
		
		if(!this.pt.canAcceptCrosstalk(links, slotList, db)) return false;
		
		for(Long key: this.activeFlows.keySet()) {
		
			if(key == flow.getID()) continue;
			
			
			ArrayList<Slot> t = getMatchSlots(slotList, this.activeFlows.get(key).getSlotList());
			if(!t.isEmpty()) 
			{
				if(!this.pt.canAcceptInterCrosstalk(this.activeFlows.get(key), slotList, t)) return false;
			}
			
			
		}
		
		return true;
	}
	
	@SuppressWarnings("unused")
	private int getDemandInSlots(Flow flow, int []links) {
		
		int modulation = chooseModulationFormat(flow.getRate(), links);
		flow.setModulationLevel(modulation);
		double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
		return ( (int) Math.ceil((double)flow.getRate() / subcarrierCapacity) + 1 );
	}
	
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int []links) {
		
		ArrayList<Slot> fittedSlotList  = canBeFitConnection(flow, links, spectrum, flow.getRate());
		
		if(!fittedSlotList.isEmpty()) {
			if(establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void getSetOfCandidates(ArrayList<ArrayList<Slot>>  slotList, boolean [][]spectrum, int demandInSlots, int []links, Flow flow, ArrayList<Integer> candidates) {
		
		ArrayList<Integer> xt = new ArrayList<Integer>();
		int count = 0;
		ArrayList<Slot> temp = new ArrayList<Slot>();
		for(int i = 0; i < spectrum.length; i++) {
			
			for(int j = 0; j < spectrum[i].length; j++) {
				
				if(i == 0 && spectrum[i][j] == true) 
				{
					temp.add(new Slot(i, j));
				}
				else if( temp.size() >= 1) {
					if(Math.abs(i - temp.get(temp.size()-1).s) == 1 && spectrum[i][j] == true) {
						temp.add(new Slot(i, j));
					}
				}
				else 
				{
					temp = new ArrayList<Slot>();
					if(Math.abs(spectrum[i].length-i) < demandInSlots) {
						break;
					}
					
					temp.add(new Slot(i, j));
				}
				
				if(temp.size() == demandInSlots) {
					
					slotList.add(new ArrayList<Slot>(temp));
					candidates.add(count);
					
					xt.add( (int)(this.pt.sumOfInterCoreCrosstalk(links, temp, ModulationsMuticore.inBandXT[flow.getModulationLevel()]) * (-100.0) ) );
					
					temp.remove(0);
					count++;
				}
			}
		}
		
		candidates.sort( (a,b) -> xt.get(a) - xt.get(b));	
	}

	
	public boolean fitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<Integer> candidates = new ArrayList<Integer>();
		ArrayList<ArrayList<Slot>> slotList = new ArrayList<ArrayList<Slot>> ();
		
		getSetOfCandidates(slotList, spectrum, demandInSlots, links, flow, candidates);
		
		if(slotList.size() <= 0) return false;
		
		for(int index: candidates) {
			
			ArrayList<Slot> slotsCandidates = slotList.get(index);
//			System.out.println(Arrays.toString(slotsCandidates.toArray()) + demandInSlots);
			if(slotsCandidates.size() == demandInSlots) 
			{
				if(cp.CrosstalkIsAcceptable(flow, links, slotsCandidates, ModulationsMuticore.inBandXT[flow.getModulationLevel()])) {
					
					if(establishConnection(links, slotsCandidates, flow.getModulationLevel(), flow)) {
						return true;
					}
				}
			}
		}

		return false;
	}
	
	private double []getFragmentationRatio() {
    	
    	int nLinks = this.pt.getNumLinks();
    	double []fi = new double[nLinks];
    	double nSlots = (double)(this.pt.getNumSlots() * this.pt.getCores());
    	
    	for(int i = 0; i < nLinks; i++) {
    		fi[i] =  (double)(nSlots - (double)this.pt.getLink(i).getSlotsAvailable()) / nSlots;
    	}
    	
    	return fi;
	}

	private ArrayList<Integer>orderKPaths(ArrayList<int[]> p) {
		
		ClosenessCentrality<Integer,DefaultWeightedEdge> cc = new ClosenessCentrality<Integer,DefaultWeightedEdge>(pt.getGraph());
	    
    	double []sumRisc = new double[p.size()];
    	ArrayList<Integer> indices = new ArrayList<Integer>();
    	
    	double []fi = getFragmentationRatio();
    	int i = 0;
    	for(int []links: p) {
    		
    		sumRisc[i] = 0;
    		
    		for(int index : links) {
    			double cci = cc.getVertexScore(this.pt.getLink(index).getDestination()) + cc.getVertexScore(this.pt.getLink(index).getSource());
    			sumRisc[i] += (fi[index] + cci);
    		}
    		
    		indices.add(i);
    		i++;
    	}
    	
    	indices.sort((a,b) -> (int)(sumRisc[a] * 100) - (int)(sumRisc[b] * 100) );
    	
    	return indices;
	}
	
}
