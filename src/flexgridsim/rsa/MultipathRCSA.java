package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class MultipathRCSA extends SCVCRCSA {
	
	private int TH = 400;//1250
	
	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {

		setkShortestPaths(flow);

		for(int []links: this.paths) {
			
			if(fitConnection(flow, bitMapAll(links), links)) {
					this.paths.clear();
					return;
			}
		}
		
		if(flow.getRate() >= TH)
		{
			flow.setMultipath(true);
			if(multipathEstablishConnection(flow)) 
			{
				this.paths.clear();
//				System.out.println("Connection accepted using multipath: "+flow);
				return;
			}
//			else {
//				System.out.println("Multipath COULD NOT allocate " + flow);
//			}
		}
//		else {
//			System.out.println("Flow too small for multipath " + flow);
//		}
		
//		for(int []links : getkShortestPaths()) printSpectrum(bitMapAll(links));
//		System.out.println("blocked:"+ flow);
		this.paths.clear();
		cp.blockFlow(flow.getID());
		
	}
	
	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<ArrayList<Slot>> setOfSlots = new ArrayList<ArrayList<Slot>> ();
//		printSpectrum(spectrum);
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
	
	
	
	protected boolean multipathEstablishConnection(Flow flow) {

//		kPaths = 4;
//		this.setkShortestPaths(flow);
		
		if(getkShortestPaths().size() <= 1)
		{
			return false;
		}
		
		ArrayList<int[]> lightpaths = new ArrayList<int[]>();
		ArrayList<ArrayList<Slot>> slotList = getSetOfSlotsAvailableInEachPath(getkShortestPaths(), flow, lightpaths);
		
		
		if(!slotList.isEmpty()) {
//			System.out.println(lightpaths.size());
			if(this.establishConnection(lightpaths, slotList, flow.getModulationLevels(), flow))
			{
				return true;
			}
		}
		
		
		return false;
	}
	
	private ArrayList<Integer> getMatchingLinksInEachLightPath(ArrayList<int[]> lightpaths, int []l2) {
		
		int index = 0;
		ArrayList<Integer> match = new ArrayList<Integer>();
		
		for(int []l1: lightpaths) {
			
			for(int i: l1) {
				
				for(int j: l2) {
					
					if(i == j) {
						match.add(index);
					}
					
				}
			}
			
			index++;
		}
		
		
		return match;
	}
	
	private ArrayList<Slot> getSetOfSlotsAvailableModified(int[] links, ArrayList<int[]> candidatesLP, ArrayList< ArrayList<Slot> > slotList, Flow flow, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int modulation = chooseModulationFormat(rate, links);
		ArrayList< ArrayList<Slot> > set = new ArrayList< ArrayList<Slot> >();
		
		ArrayList<Integer> match = getMatchingLinksInEachLightPath(candidatesLP, links);
		
		for(int i : match) {
			set.add(slotList.get(i));
		}
		
		while(modulation >= 0) {
			
			double requestedBandwidthInGHz = ( ( (double)rate ) / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
//			System.out.println(demandInSlots);
			fittedSlotList = FirstFitPolicyModified(flow, bitMapAll(links), links, demandInSlots, modulation, set);
			
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
	
	private boolean getMatchingSlots(ArrayList<ArrayList<Slot>> slots, ArrayList<Slot> s2) {

		for(ArrayList<Slot> s1: slots) {
			
			if(s1.get(0).c != s2.get(0).c) continue;
			
			for(Slot i: s1) {
				for(Slot j: s2) 
				{
					if(i.s == j.s) {
						return true;
					}
				}
			}
		}
		
			
		return false;
	}
	
	private ArrayList<Slot> FirstFitPolicyModified(Flow flow, boolean[][] spectrum, int[] links, int demandInSlots, int modulation, ArrayList< ArrayList<Slot> > slots) {
		
		ArrayList<ArrayList<Slot>> setOfSlots = new ArrayList<ArrayList<Slot>> ();
//		printSpectrum(spectrum);
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
					
					if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation]) && !getMatchingSlots(slots, temp)) {
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


	private ArrayList<Slot> getSetOfSlotsAvailable(int[] lightpath, Flow flow, int rate ) {
		
		ArrayList<Slot> setOfSlots = getSlotsAvailable(lightpath, flow, rate);
		
		if(!setOfSlots.isEmpty()) 
		{
			return new ArrayList<Slot>(setOfSlots);
		}
		
		
		return new ArrayList<Slot>();
		
	}
	

	
	private ArrayList<int[]>  getPathsCandidates(ArrayList<int[]> paths, int demandInSlots) {

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
	
	private int getMatchingLinks(ArrayList<int[]> lightpaths, int []l2) {
		
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
	
	
	private ArrayList<ArrayList<Slot>> getSetOfSlotsAvailableInEachPath(ArrayList<int[]> paths, Flow flow, ArrayList<int[]> lightpathsAvailable) {

		ArrayList<int[]> lightpaths = getPathsCandidates(paths, fakeDemandInSlots(flow));
		
		if(lightpaths.size() <= 1) {
			return new ArrayList<ArrayList<Slot>>();
		}
		
		for(int i = 1; i < lightpaths.size(); i++) {
			
			int n = 2;
			int rate = (int) Math.ceil((double)flow.getRate()/(double)(n));//rate for each lightpath
			int totalRate = flow.getRate();
			
			ArrayList<int[]> lightpathsAvailableTemp = new ArrayList<int[]>();
			ArrayList<ArrayList<Slot>> slotList = new ArrayList<ArrayList<Slot>>();
			
			for(int j = 0; j <= i; j++) {
				
				ArrayList<Slot> slots = new ArrayList<Slot>();
				int index = getMatchingLinks(lightpathsAvailableTemp, lightpaths.get(j));
				if(index >= 0) {
					
					slots = getSetOfSlotsAvailableModified(lightpaths.get(j), lightpathsAvailableTemp, slotList, flow, rate);
				}
				else {
					slots = getSetOfSlotsAvailable(lightpaths.get(j), flow, rate);
				}
				
				if(!slots.isEmpty()) {
					
					slotList.add(new ArrayList<Slot>(slots));
					lightpathsAvailableTemp.add( lightpaths.get(j) );
//					System.out.println(totalRate);
					totalRate -= rate;
					
					if( totalRate < rate && totalRate >= 1) 
					{
						rate = totalRate;
					}
				}
				
				if(slotList.size() == 2) break;
			}
			
			if(slotList.size() == n) {
//				System.out.println(totalRate);
				lightpathsAvailable.addAll( new ArrayList<int[]>(lightpathsAvailableTemp) );
				
				return slotList;
			}
			
			flow.getModulationLevels().clear();
			
		}
		
		
		return new ArrayList<ArrayList<Slot>>();
	}


	private int fakeDemandInSlots(Flow flow) {
		
		int rate = Math.floorDiv(flow.getRate(), getkShortestPaths().size()) + 1;
		
		return decreaseModulation(5, rate);
	}




	private ArrayList<Slot> getSlotsAvailable(int []links, Flow flow, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int modulation = chooseModulationFormat(rate, links);
		
		while(modulation >= 0) {
			
		
			double requestedBandwidthInGHz = ( (double)rate / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
//			System.out.println(demandInSlots);
			fittedSlotList = FirstFitPolicy(flow, bitMapAll(links), links, demandInSlots, modulation);
			
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




	@SuppressWarnings("unused")
	private double getNewRate(int modulation, int n) {
		
		return (ModulationsMuticore.subcarriersCapacity[modulation] * n);
	}




	protected void updateData(Flow flow, ArrayList<int []> mpaths, ArrayList<Long> ids,  ArrayList< ArrayList<Slot> > fittedSlotList, ArrayList<Integer> modulation) {
		
		
		flow.setLinks(mpaths);
		flow.setSlotListMultipath(fittedSlotList);
		flow.setModulationLevels(modulation);
		flow.setLightpathsID(ids);
		
		ArrayList<LightPath> lps = new ArrayList<LightPath>();
		
		for(long id: ids) {
			lps.add(vt.getLightpath(id));
		}
		
		if(!cp.acceptFlow(flow.getID(), lps)) {
			throw (new IllegalArgumentException());
		}
	}
	
	
	
	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @return
	 */
	public ArrayList<Slot>fitConnection(Flow flow, boolean [][]spectrum, int []links, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		flow.setMultipath(true);
		fittedSlotList  = canBeFitConnection(flow, links, spectrum, rate);
			
		if(fittedSlotList.size() >= 1) {
			return fittedSlotList;
		}
			
		return new ArrayList<Slot>();
	}
	
	
	/**
	 * 
	 * @param links
	 * @param slotList
	 * @param modulation
	 * @param flow
	 * @return
	 */
	public boolean establishConnection(ArrayList<int []> mpaths, ArrayList< ArrayList<Slot> > slotList, ArrayList<Integer> modulation, Flow flow) {
		
		if(mpaths == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		ArrayList<Long> ids = new ArrayList<Long>();
		int i = 0;
		
		for(int []links : mpaths) {
			
			long id = vt.createLightpath(links, slotList.get(i),flow.getModulationLevel(i));
		
			if(id < 0) {
				//TODO
//				for(int []l : mpaths) System.out.println(Arrays.toString(l));
				System.out.println("Error: invalid ID: "+id+ " " + i);
			
				
				return false;
			}
			
//			System.out.println(id);
			
			i++;
			ids.add(id);
		}
			
		if (ids.size() == mpaths.size()) 
		{
			this.updateData(flow, mpaths, ids, slotList, modulation);
			return true;
		} 
		
		return false;
	}
	
	
	protected void removeCrosstalkInMultipaths(int[] links, ArrayList<Slot> slotList) {
		
		for(int l : links) {
			this.pt.getLink(l).resetCrosstalk(slotList);
        }
	}
	

	@Override
	public void flowDeparture(Flow flow) {
		
		if(!flow.isAccepeted()) {
			return;
		}
	
		if(flow.isMultipath()) {
			
			int n = flow.getMultiSlotList().size();
			
			for(int j = 0; j < n; j++) {
				removeCrosstalkInMultipaths(flow.getLinks(j), flow.getMultiSlotList().get(j));
			}
		}
		else 
		{
			if(!flow.isAccepeted()) return;
			
			removeCrosstalk(flow.getLinks(), flow);
		}
	}
}
