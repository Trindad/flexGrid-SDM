package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class MultipathPartitioning extends MultipathRCSA {
	
	private ArrayList<Integer> cores;
	private int min = 0, max = 0;
	
	public void flowArrival(Flow flow) {
		
		kPaths = 4;
		setkShortestPaths(flow);
		double []fi = new double[paths.size()];
		
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for(int i = 0; i < paths.size(); i++) {
//    		
//			fi[i] = getFragmentationRatio(paths.get(i));
//			
    		indices.add(i);
//    		i++;
    	}
		
//		indices.sort((a,b) -> (int)(fi[a] - fi[b]) );
		
//		Collections.shuffle(this.paths);
		
		ArrayList<ArrayList<Integer>> priorities = new ArrayList<ArrayList<Integer>>();
		
		if(flow.getRate() <= 100) {
			
			if(flow.getRate() <= 50) {
				min = 0;
				max = (int) (pt.getNumSlots() * 0.4);
			}
			else
			{
				min = (int) (pt.getNumSlots()*0.4);
				max = pt.getNumSlots()-1;
			}
			priorities.add( new ArrayList<>(Arrays.asList(5,4,6)) );
		}
		else {
			min = 0;
			max = pt.getNumSlots()-1;
			priorities.add( new ArrayList<>(Arrays.asList(2,1,4)) );
		}
		priorities.add( new ArrayList<>(Arrays.asList(0)) );
		
		
		ArrayList<int[]> candidates = new ArrayList<int[]>();
		ArrayList<int[]> candidatesMultipaths = new ArrayList<int[]>();
		boolean check = false;
		for(ArrayList<Integer> area : priorities) {
			
			cores = area;
			for(Integer i : indices) {
				
				int []links = paths.get(i);
				ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
				
				fittedSlotList  = canBeFitConnection(flow, links, bitMapAll(links), flow.getRate());
				
				if(!fittedSlotList.isEmpty()) {
					
					if(establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow)) {
						return;
					}
				}
				
				candidates.add(links);
//				if(candidates.size() >= 2 || check == true) {
//					
//					for(int []route : candidates) {
//						flow.setMultipath(true);
//						if(multiCoresAllocation(flow, route)) return;
//						
//					}
//					check = true;
//					candidates.clear();
//				}

				candidatesMultipaths.add(links);

				flow.setMultipath(false);
			}
		}
		
//		indices.sort((a,b) -> (int)(fi[a] - fi[b]) );
		flow.setMultipath(true);
		if(multipathEstablishConnection(flow, candidatesMultipaths)) 
		{
			this.paths.clear();
//			System.out.println("Connection accepted using multipath: "+flow);
			return;
		}
//		for(int []links : getkShortestPaths()) printSpectrum(bitMapAll(links));
//		System.out.println("blocked:"+ flow);
		this.paths.clear();
		cp.blockFlow(flow.getID());
		
	}
	
	private double getFragmentationRatio(int []links) {
    	
    	int nLinks = pt.getNumLinks();
    	double fi = 0;
    	double nSlots = (pt.getNumSlots() * pt.getCores());
    	
    	for(int i = 0; i < nLinks; i++) {
    		
    		fi +=  ((double)(nSlots - (double)pt.getLink(i).getNumFreeSlots()) / nSlots) * 100;
    	}
    	
    	return fi;
	}
	
	public boolean multipathEstablishConnection(Flow flow, ArrayList<int[]> candidates) {
		
		setkShortestPaths(flow);
		ArrayList<int[]> lightpaths = new ArrayList<int[]>();
		ArrayList<ArrayList<Slot>> slotList = getSetOfSlotsAvailableInEachPath(candidates, flow, lightpaths);
		
		if(!slotList.isEmpty()) {
//			System.out.println(lightpaths.size());
			if(this.establishConnection(lightpaths, slotList, flow.getModulationLevels(), flow))
			{
				return true;
			}
		}
		
		return false;
	}
	
	protected ArrayList< ArrayList<Slot> > getSetOfSlotsAvailableInEachPath(ArrayList<int[]> paths, Flow flow, ArrayList<int[]> lightpathsAvailable) {
		
		if(flow.getRate() <= 50) return new ArrayList< ArrayList<Slot> >(); 
		
		min = 0;
		max = pt.getNumSlots()-1;
		
		cores.clear();
		if(flow.getRate() < 400) cores.addAll( new ArrayList<>(Arrays.asList(2,1,3)) );
		else if(flow.getRate() >= 400) cores.addAll( new ArrayList<>(Arrays.asList(5,4,6)) );
		
		ArrayList<int[]> temp = new ArrayList<int[]>();
		int a = flow.getRate() <= 100 ? 2 : 4;
		temp = getPathsCandidates(paths, getDemandInSlots( (int)Math.ceil( (double)flow.getRate()/a) ) );
		
		if(temp.size() <= 1) {
			return new ArrayList< ArrayList<Slot> >();
		}
		

		
		for(int i = 2; i < temp.size(); i++) {
		
			int lim = a;
			int n = 2;
			while(n <= lim) 
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
//						System.out.println("HERE: "+flow+" rate: "+rate+" "+slots.size());
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
	
	protected  boolean multiCoresAllocation(Flow flow, int []links) {
		
//		if(flow.getRate() <= 50) return false;
		
		ArrayList<int[]> lightpathsAvailableTemp = new ArrayList<int[]>();
		ArrayList<ArrayList<Slot>> slotList = new ArrayList<ArrayList<Slot>>();
		
		int n = 2;
		int limit = flow.getRate() <= 50 ? 2 : 3;
		int rate = flow.getRate();//rate for each lightpath
		while(n <= limit) {
			
//			System.out.println("print");
			rate = (int) Math.ceil((double)flow.getRate()/(double)(n));//rate for each lightpath
			int totalRate = flow.getRate();
			
			if(rate <= 100) {
				
				
				min = 0;
				max = pt.getNumSlots()-1;
				
				cores.clear();
				cores.addAll( new ArrayList<>(Arrays.asList(4,6,5)) );
			}
			else
			{
				min = 0;
				max = pt.getNumSlots()-1;
	
				cores.clear();
				cores.addAll( new ArrayList<>(Arrays.asList(1,3,2)) );
			}
			
			for(int i = 0; i < n; i++) {
				
				ArrayList<Slot> slots = new ArrayList<Slot>();
				int index = getMatchingLinks(lightpathsAvailableTemp, links);
				if(index >= 0 && !slotList.isEmpty()) {
					
					slots = getSetOfSlotsAvailableModified(links, lightpathsAvailableTemp, slotList, flow, rate);
					
				}
				else {
					slots = getSetOfSlotsAvailable(links, flow, rate);
				}
				
				if(!slots.isEmpty()) {
					
					slotList.add(new ArrayList<Slot>(slots));
					lightpathsAvailableTemp.add( links );
					totalRate -= rate;
					
					if( totalRate < rate && totalRate >= 1) 
					{
						rate = totalRate;
					}
				}
			}
			
			if(slotList.size() == n) {
				break;
			}
			
			n++;
		}	
			
		if(slotList.size() == n) {
			
			ArrayList<int []> lightpathsAvailable = new ArrayList<>();
			
			for(int k = 0; k < lightpathsAvailableTemp.size(); k++) {
				lightpathsAvailable.add(lightpathsAvailableTemp.get(k));
			}
			
			if(!slotList.isEmpty()) {
				
				if(this.establishConnection(lightpathsAvailableTemp, slotList, flow.getModulationLevels(), flow))
				{
					System.out.println("Multipath: "+flow);
					return true;
				}
			}
		}
		
		flow.getModulationLevels().clear();
		
	
		return false;
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
			
			fittedSlotList = FitPolicy(flow, spectrum, links, demandInSlots, modulation);
			
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
	
	public ArrayList<Slot> FirstFitPolicyModified(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {

//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
		
		for(int c = 0; c < cores.size() ; c++) {
			int i = cores.get(c);
			
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int j = min; j < max; j++) {	
				
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

	
	
	public ArrayList<Slot> FitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 5, 4, 3, 2 , 1 , 0));
		
		for(int c = 0; c < cores.size() ; c++) {
			int i = cores.get(c);
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int j = min; j <= max; j++) {	
				
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
					
					break;
				}
			}
		}
	    
		return new ArrayList<Slot>();
	}

}
