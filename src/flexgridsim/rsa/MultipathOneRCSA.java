package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

/**
 * Multi-path RMCSA splitting connections in block of slots
 * 
 * @author trindade
 *
 */

public class MultipathOneRCSA extends MultipathRCSA {
	
	
	public void flowArrival(Flow flow) {
		
		setkShortestPaths(flow);

		flow.setMultipath(true);
		if(multipathEstablishConnection(flow)) 
		{
			this.paths.clear();
			return;
		}
	
		this.paths.clear();
		cp.blockFlow(flow.getID());
		
	}
	
	
	protected boolean multipathEstablishConnection(Flow flow) {

	
		setkShortestPaths(flow);
		ArrayList<int[]> lightpaths = new ArrayList<int[]>();
		ArrayList<ArrayList<Slot>> slotList = getSetOfSlotsAvailableInEachPath(getkShortestPaths(), flow, lightpaths);
		
		if(!slotList.isEmpty()) {

			if(this.establishConnection(lightpaths, slotList, flow.getModulationLevels(), flow))
			{
				return true;
			}
		}
		
		return false;
	}
	
	
	protected ArrayList<Slot> getSetOfSlotsAvailableModified(int[] links, ArrayList<int[]> lightpathsAvailableTemp , ArrayList< ArrayList< Slot > > slotList, Flow flow, int rate) {
		
		boolean [][]spectrum = bitMapAll(links);
		int index = 0;
		for(int []l: lightpathsAvailableTemp) {
			
			boolean match = getMatchingLinks(l, links);
			if(match) {
				for(Slot s: slotList.get(index)) {
					spectrum[s.c][s.s] = false;
				}
			}
			
			index++;
		}
		
		return tryToFit(flow, spectrum, rate, links);
	}


	protected ArrayList<Slot> getSetOfSlotsAvailable(int[] lightpath, Flow flow, int rate ) {
		
		return getSlotsAvailable(lightpath, flow, rate);
	}
	

	
	protected ArrayList<int[]>  getPathsCandidates(ArrayList<int[]> paths, int demandInSlots) {

		
		ArrayList<int[]> selectedPaths = new ArrayList<int[]>();
		for(int []links : paths) {
			
			bitMapAll(links);
			
			if(totalSlotsAvailable >= demandInSlots) 
			{
				selectedPaths.add(links);
			}
		}
		
		return selectedPaths;
	}
	
	protected int getMatchingLinks(ArrayList<int[]> lightpaths, int []l2) {
		
		if(lightpaths.isEmpty() || l2.length == 0) return -2;
		
		int index = 0;
		for(int []l1: lightpaths) {
			for(int i: l1) {
				for(int j: l2) {
					if(i == j) {
						return index;
					}	
				}
			}
			index++;
		}
		
		return -1;
	}
	
	
	protected boolean getMatchingLinks(int[] l1, int []l2) {
		for(int i: l1) {
			for(int j: l2) {
				if(i == j) {
					return true;
				}	
			}
		}
			
		
		return false;
	}
	
	private ArrayList< ArrayList<Slot> > getSetOfSlotsAvailableInEachPath(ArrayList<int[]> paths, Flow flow, ArrayList<int[]> lightpathsAvailable) {

		ArrayList<int[]> temp = new ArrayList<int[]>();
		int tam = paths.size() * 3;
		temp = getPathsCandidates(paths, getDemandInSlots( (int)Math.ceil( (double)flow.getRate()/tam) ) );
		
		if(temp.size() <= 1) {
			return new ArrayList< ArrayList<Slot> >();
		}
		
		int n = 1;
		
		while(n <= tam) 
		{
			for(int i = 1; i < temp.size(); i++) {
				
			int rate = (int) Math.ceil((double)flow.getRate()/(double)(n));//rate for each lightpath
			int totalRate = flow.getRate();
			
			ArrayList<int[]> lightpathsAvailableTemp = new ArrayList<int[]>();
			ArrayList<ArrayList<Slot>> slotList = new ArrayList<ArrayList<Slot>>();
			
			for(int j = 0; j <= i; j++) {
				
				ArrayList<Slot> slots = new ArrayList<Slot>();
				int index = getMatchingLinks(lightpathsAvailableTemp, temp.get(j));
				if(index >= 0 && !slotList.isEmpty()) {
					
					slots = getSetOfSlotsAvailableModified(temp.get(j), lightpathsAvailableTemp, slotList, flow, rate);
					
				}
				else {
					slots = getSetOfSlotsAvailable(temp.get(j), flow, rate);
				}
				
				if(!slots.isEmpty()) {
					
					slotList.add(new ArrayList<Slot>(slots));
					lightpathsAvailableTemp.add( temp.get(j) );
//						System.out.println(lightpathsAvailableTemp.size() +" "+flow+ " "+slots);
					totalRate -= rate;
					
					if( totalRate < rate && totalRate >= 1) 
					{
						rate = totalRate;
					}
					
					j--;
				}
				
				if(slotList.size() == n) {
					break;
				}
			}
			
			if(slotList.size() == n) {
				
				lightpathsAvailable.clear();
				
				for(int k = 0; k < lightpathsAvailableTemp.size(); k++) {
					lightpathsAvailable.add(lightpathsAvailableTemp.get(k));
				}
				
//				System.out.println("::"+lightpathsAvailable.size());
				return slotList;
			}
			
			flow.getModulationLevels().clear();
			n++;//allow to distribute the bw into upt to 3 cores or 3 paths
		}
		
	}

		return new ArrayList<ArrayList<Slot>>();
	}

	protected ArrayList<Slot> tryToFit(Flow flow, boolean [][]spectrum, int rate, int []links) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int modulation = chooseModulationFormat(rate, links);
		
		while(modulation >= 0) {
			
			double requestedBandwidthInGHz = ( (double)rate / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
			fittedSlotList = FirstFitPolicyModified(flow, spectrum, links, demandInSlots, modulation);
			
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
		
		return fittedSlotList;
	}


	private ArrayList<Slot> getSlotsAvailable(int []links, Flow flow, int rate) {
		
		return tryToFit(flow, bitMapAll(links), rate, links);
	}
	
}

