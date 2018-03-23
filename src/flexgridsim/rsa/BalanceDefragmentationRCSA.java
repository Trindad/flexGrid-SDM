package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.LinkedList;

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
		
		kPaths = 5;
		ArrayList<int[]> kPaths = findKPaths(flow);//find K-Shortest paths using Dijkstra
		ArrayList<Integer> indices = orderKPaths(kPaths);//sort paths by the fragmentation index from each link

		for(int i: indices)
		{
			int []links = kPaths.get(i);
			boolean [][]spectrum = bitMapAll(links);
			
			if(fitConnection(flow, spectrum, links)) {
				this.activeFlows.put(flow.getID(), flow);
				updateCrosstalk();
				paths.clear();
				return;
			}
		}
		
		paths.clear();
//		System.out.println(flow.getRate());
		
		this.connectionDisruption.add(flow);
		
		flow.setConnectionDisruption(true);
		
		this.nConnectionDisruption++;
	}
	
	private ArrayList<Slot> getMatchingSlots(ArrayList<Slot> s1, ArrayList<Slot> s2, LinkedList<Integer> adjacents) {
		
		boolean isAdjacent = false;
		for (int i : adjacents) {
			
			if(i == s2.get(0).c) {
				isAdjacent = true;	
			}
		}
		
		if (isAdjacent) {
			ArrayList<Slot> slots = new ArrayList<Slot>();
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
		
		return new ArrayList<Slot>();
	}
	
	private ArrayList<Integer>getMatchingLinks(int []l1, int []l2) {
		
		ArrayList<Integer> links = new ArrayList<Integer>();
		for(int i: l1) {
			
			for(int j: l2) {
				
				if(i == j) {
					links.add(i);
				}
				
			}
		}
		
		return links;
	}

	public boolean CrosstalkIsAcceptable(Flow flow, int[] links, ArrayList<Slot> slotList, double db) {
		
		double xt = 0;
		xt += this.pt.canAcceptCrosstalk(links, slotList, db);
		
		for(Long key: this.activeFlows.keySet()) {
		
			if(key == flow.getID()) continue;
			
			ArrayList<Integer> match = getMatchingLinks(links, activeFlows.get(key).getLinks() );
			
			if( !match.isEmpty() ) {
				
				int c = slotList.get(0).c;
				LinkedList<Integer> adj = pt.getLink(0).getAdjacentCores(c);
				ArrayList<Slot> t = getMatchingSlots(slotList, this.activeFlows.get(key).getSlotList(), adj);
				
				if(!t.isEmpty()) 
				{
					
					xt += this.pt.canAcceptInterCrosstalk(this.activeFlows.get(key), match, slotList, t);
				}
				else
				{
					xt += pt.canAcceptInterCrosstalk(this.activeFlows.get(key), match, this.activeFlows.get(key).getSlotList());
				}
			}
			
		}
		xt = xt > 0 ? ( 10.0f * Math.log10(xt)/Math.log10(10) ) : 0.0f;//db
		return xt == 0 || xt <= db;
	}
	
	@SuppressWarnings("unused")
	private int getDemandInSlots(Flow flow, int []links) {
		
		int modulation = chooseModulationFormat(flow.getRate(), links);
	
		if(modulation <= -1) {
			return -1;
		}
		
		flow.setModulationLevel(modulation);
		double requestedBandwidthInGHz = ( ((double)flow.getRate()) / ((double)modulation + 1) );
		double requiredBandwidthInGHz = requestedBandwidthInGHz;
		double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
		int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
		
		demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
		demandInSlots++;
		
		return demandInSlots;
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
	
	private ArrayList<Slot> FirstFitPolicy(boolean [][]spectrum, int demandInSlots, int []links, Flow flow, int modulation) {

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
					
					if(CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {
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
			
			double requestedBandwidthInGHz = ( ((double)rate) / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
			fittedSlotList = FirstFitPolicy(spectrum, demandInSlots, links, flow,modulation);
			
			if(fittedSlotList.size() == demandInSlots) {
					
				flow.setModulationLevel(modulation);
				
				return fittedSlotList;
				
			}
			
			modulation--;
		}
		
		
		return new ArrayList<Slot>();
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
