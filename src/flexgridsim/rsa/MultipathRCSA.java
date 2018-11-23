package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class MultipathRCSA extends XTFFRCSA {
	
	private int TH = 100;//limit to use multipath
	
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
		//execute Multipath algorithm
		if(flow.getRate() >= TH)
		{
//			System.out.println("Connection accepted using multipath: "+flow);
			flow.setMultipath(true);
			if(multipathEstablishConnection(flow)) 
			{
				this.paths.clear();
//				System.out.println("Connection accepted using multipath: "+flow);
				return;
			}
		}
		
//		for(int []links : getkShortestPaths()) printSpectrum(bitMapAll(links));
//		System.out.println("blocked:"+ flow);
		this.paths.clear();
		cp.blockFlow(flow.getID());
		
	}
	
	
	
	protected boolean multipathEstablishConnection(Flow flow) {

	
		setkShortestPaths(flow);
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
	
	protected int getDemandInSlots(int t) {
		
		double requestedBandwidthInGHz = ( (double)t / (double)decreaseModulation(5, t) );
		double requiredBandwidthInGHz = requestedBandwidthInGHz;
		double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
		int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
		demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
		demandInSlots++;
		
		return demandInSlots;
	}
	
	
	private ArrayList< ArrayList<Slot> > getSetOfSlotsAvailableInEachPath(ArrayList<int[]> paths, Flow flow, ArrayList<int[]> lightpathsAvailable) {

		ArrayList<int[]> temp = new ArrayList<int[]>();
		temp = getPathsCandidates(paths, getDemandInSlots( (int)Math.ceil( (double)flow.getRate()/4.0) ) );
		
		if(temp.size() <= 1) {
			return new ArrayList< ArrayList<Slot> >();
		}
		
		
//		System.out.println(flow + " nPaths: "+lightpathsAvailable.size());
		for(int i = 1; i < temp.size(); i++) {
			
			int n = 2;
			while(n <= 2) 
			{
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
					
//					System.out.println("::"+lightpathsAvailable.size());
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

	
	public ArrayList<Slot> FirstFitPolicyModified(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {

//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 5, 4, 3, 2 , 1 , 0));
		for(int c = 0; c < priorityCores.size() ; c++) {
			int i = priorityCores.get(c);
			
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

						return temp;
					}
					
					temp.clear();
				}
			}
		}
		
	    
		return new ArrayList<Slot>();
	}


	private ArrayList<Slot> getSlotsAvailable(int []links, Flow flow, int rate) {
		
		return tryToFit(flow, bitMapAll(links), rate, links);
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
//		System.out.println(mpaths.size());
		ArrayList<Long> ids = new ArrayList<Long>();
		int i = 0;
		int pathLength = 0;
		for(int []links : mpaths) {
			
			long id = vt.createLightpath(links, slotList.get(i),flow.getModulationLevel(i));
		
			if(id < 0) {
				System.out.println("Error: invalid ID: "+id+ " " + i);
				return false;
			}
			
			pathLength += getPathLength(links);
			i++;
			ids.add(id);
		}
			
		if (ids.size() == mpaths.size()) 
		{
			flow.setPathLength(pathLength);
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
