package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class FloodFillRCSA extends SCVCRCSA{

	protected boolean runRCSA(Flow flow) {
		
		setkShortestPaths(flow);
	
		for(int []links : getkShortestPaths()) {
			
			if(fitConnection(flow, links)) {
//				System.out.println("Connection accepted: "+flow);
				this.paths.clear();
				return true;
			}
		}
		
		this.paths.clear();
		System.out.println("Connection blocked: "+flow);
		return false;
	}
	
	private int getNumberOfSlots(Flow flow, int []links, int modulationLevel) {
		
		flow.setModulationLevel(modulationLevel);
		double requestedBandwidthInGHz = ( (double)flow.getRate() / ((double)modulationLevel + 1) );
		double requiredBandwidthInGHz = requestedBandwidthInGHz;
		double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
		int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
		
		demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
		demandInSlots++;
		
		return demandInSlots;
	}
	
	private int bitMapLimited(boolean [][]update, boolean [][]spectrum, int []links, ArrayList<Integer> listOfCores, int start, int end) {
		
		int t = 0;
//		System.out.println(start + " "+ end);
		for(int c = 0; c < spectrum.length; c++ ) {
			for(int s = 0; s < spectrum[c].length; s++) {
				update[c][s] = false;
			}
		}
		
		
		for(int c : listOfCores) {
			for(int s = start; s <= end; s++) {
				if(spectrum[c][s]) {
					update[c][s] = true;
					t++;
				}
			}
			
//			System.out.println(Arrays.toString(update[c]));
		}
//		for(int c = 0; c < spectrum.length; c++ ) {
//			System.out.println(Arrays.toString(update[c]));
//		}
//		System.out.println("\n\n\n");
		return t;
	}
	
	private boolean tryToFitConnection(Flow flow, ArrayList<Integer> listOfCores, boolean [][]spectrum, int []links, int demandInSlots, int start, int end) {
		
		boolean [][]update = new boolean [pt.getCores()][pt.getNumSlots()];
			
		int t = bitMapLimited(update, spectrum, links, listOfCores, start, end);
		
		int nVisited = 0;
		FloodFill8 ff  = new FloodFill8(update.clone());
	
		while (nVisited < t) {
		
			int []coreAndSlot = getSeed(listOfCores, ff.getFillMap());
			if(coreAndSlot != null) 
			{
				nVisited += ff.runFloodFill(coreAndSlot[0], coreAndSlot[1]);
			}
		}
	
		if(allocateConnection(flow, ff.getFillingArea(), links, demandInSlots)) 
		{
			return true;
		}

		return false; 
	}
	
	protected boolean fitConnection(Flow flow, ArrayList<Integer> listOfCores, int []links, int start, int end) {
		
		int modulationFormat = chooseModulationFormat(flow.getRate(), links);
		
		while(modulationFormat >= 0) {
			boolean [][]spectrum = bitMapAll(links);
			int demandInSlots  = getNumberOfSlots(flow, links, modulationFormat);
			flow.setModulationLevel(modulationFormat);
			
			if(demandInSlots <= 0 ) {
				return false;
			}

			if(tryToFitConnection(flow, listOfCores, spectrum, links, demandInSlots, start, end)) {
				return true;
			}
			
			modulationFormat--;
			
		}
	
		return false;
	}
	
	
	private ArrayList<Integer> newListOfCores(int coreEnd, int endSlot) {
	
		ArrayList<Integer> cores = new ArrayList<Integer>();
		int k =  ( (pt.getNumSlots() / 2 )-1);
		//6...4
		if(coreEnd == ( pt.getCores() - 1 ) ) {
			
			int n = ( pt.getCores() - (pt.getCores()/2) ) - 1;
			for(int i = n; i >= 0 ; i--) cores.add(i);
		}
		//3...0 and 0..159
		else if(coreEnd == ( Math.floorDiv(pt.getCores(), 2) ) && endSlot == k ) {
			
			int n = (pt.getCores()/2);
			for(int i = (pt.getCores()-1); i > n; i--) cores.add(i);
		}
		
//		System.out.println(coreEnd + " "+ endSlot + " " + (coreEnd == (pt.getCores()/2) - 1 ) + " " + (endSlot == ( (pt.getNumSlots()/2)-1)) );
		return cores;
	}
	
	private ArrayList<Integer> newStartAndEndSlotIndex(int endCore, int endSlot) {
		
		if(endSlot == (pt.getNumSlots()-1) ) {
			return new ArrayList<Integer>(Arrays.asList( 0, ( ( pt.getNumSlots() / 2 ) -1) ) );
		}
		else if(endSlot == ( (pt.getNumSlots()/2)-1) )
		{
			return new ArrayList<Integer>(Arrays.asList(  (pt.getNumSlots() / 2 ), (pt.getNumSlots()-1) ) );
		}
		
		return new ArrayList<Integer>();
	}

	protected boolean fitConnection(Flow flow, int []links) {
	
		int start = 0;
		int end = (pt.getNumSlots()/2)-1;
		int coreEnd = pt.getCores()-1;
		int CoreStart = Math.floorDiv(pt.getCores(), 2);
		ArrayList<Integer> listOfCores = new ArrayList<Integer>();
		
		for(int i = coreEnd; i > CoreStart; i--) listOfCores.add(i);
		
		int it = 0;
		
		while(it <= 3) {
			
			it++;
//			System.out.println(start+" ... "+ end+ " "+Arrays.toString(listOfCores.toArray()));
			if(fitConnection(flow, listOfCores, links, start, end)) {
				return true;
			}

			ArrayList<Integer> p = newStartAndEndSlotIndex(coreEnd, end);
			
			ArrayList<Integer> temp = newListOfCores(coreEnd, end);
			
			if(!temp.isEmpty()) {
				coreEnd = temp.get(0);
				listOfCores = temp;
			}
			
			start = p.get(0);
			end = p.get(1);
			
		}
		
//		System.out.println("**********************************");
//		System.out.println(Arrays.toString(listOfCores.toArray()));
		return false;
	}

	private int[]getSeed(ArrayList<Integer> cores, boolean[][]spectrum) {
		
		for(int it : cores) {
			
			int []seed = getRandomNumbers(spectrum,it);
			
			if(seed != null) {
				return seed;
			}
		}
		
		return null;
	}

	private int[] getRandomNumbers(boolean[][] spectrum, Integer index) {

		int []numbers = new int[2];
		int i = 0;
		while(i < 3) {
			
			int j = (int)(Math.random() * spectrum[index].length + 1);
			if(spectrum[index][j-1]) {
				
				numbers[0] = index;
				numbers[1] = j-1;
				return numbers;
			}
			
			i++;
		}
		
		return getPositionNumbers(spectrum, index);
	}

	private int[]getPositionNumbers(boolean[][] spectrum, int index) {
		
		int []numbers = new int[2];
		for(int j = 0; j < spectrum[index].length; j++) {
			if(spectrum[index][j]) 
			{
				numbers[0] = index;
				numbers[1] = j;
				
				return numbers;
			}
		}
	
		return null;
	}

	private ArrayList<Slot> getSetOfCandidatesRight(ArrayList<Integer> slots, int core, int demandInSlots, int []links, Flow flow) {
		
		double db = ModulationsMuticore.inBandXT[flow.getModulationLevel()];
		if(slots.size() >= demandInSlots) {
			
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int i = 0; i < slots.size(); i++) {
				
				if(i == 0) 
				{
					temp.add(new Slot(core, slots.get(i)));
				}
				else if( Math.abs(slots.get(i) - slots.get(i-1)) == 1 ) {
					temp.add(new Slot(core, slots.get(i)));
				}
				else 
				{
					temp = new ArrayList<Slot>();
					if(Math.abs(slots.size()-i) < demandInSlots) {
						break;
					}
					
					temp.add(new Slot(core, slots.get(i)));
				}
				
				if(temp.size() == demandInSlots) {
					
					if(cp.CrosstalkIsAcceptable(flow, links, temp, db)) {
						
						return temp;
					}
					
					temp.remove(0);
				}
			}
		}   

		return new ArrayList<Slot>();
	}
	
	protected ArrayList<Slot> getSetOfCandidates(ArrayList<Integer> slots, int core, int demandInSlots, int []links, Flow flow) {
		
		if(slots.size() >= demandInSlots) {	
			
			Collections.sort(slots);
//			int n = (pt.getNumSlots()/2);
			
//			if(slots.get( (slots.size()-1)  ) >=  n) 
//			{
				return getSetOfCandidatesRight(slots,core,demandInSlots, links, flow);
//			}
//			else 
//			{
//				return getSetOfCandidatesLeft(slots,core,demandInSlots, links, flow);
//			}
			
		}
		
		return new ArrayList<Slot>();
	}
	
	private ArrayList<Slot> getSetOfCandidatesLeft(ArrayList<Integer> slots, int core, int demandInSlots, int[] links, Flow flow) {
		double db = ModulationsMuticore.inBandXT[flow.getModulationLevel()];
		if(slots.size() >= demandInSlots) {
			
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int i = slots.size()-1; i >= 0 ; i--) {
				
				if(i == (slots.size()-1) ) 
				{
					temp.add(new Slot(core, slots.get(i)));
				}
				else if( Math.abs(slots.get(i) - slots.get(i+1)) == 1 ) {
					temp.add(new Slot(core, slots.get(i)));
				}
				else 
				{
					temp = new ArrayList<Slot>();
					if(Math.abs(slots.size()-i) < demandInSlots) {
						break;
					}
					
					temp.add(new Slot(core, slots.get(i)));
				}
				
				if(temp.size() == demandInSlots) {
					
					if(cp.CrosstalkIsAcceptable(flow, links, temp, db)) {
						
						return temp;
					}
					
					temp.remove(0);
					
				}
			}
		}   

		return new ArrayList<Slot>();
	}

	public boolean allocateConnection(Flow flow, HashMap<Integer, ArrayList<Integer>> spectrumAvailable, int[]links, int demandInSlots) {

//		ArrayList<Integer> coreIndex = new ArrayList<Integer>(Arrays.asList(6, 3, 2, 5, 0, 4, 1));//15.88
//		ArrayList<Integer> coreIndex = new ArrayList<Integer>(Arrays.asList(6, 3, 2, 5, 0, 1, 4));//15.65
//		ArrayList<Integer> coreIndex = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 4, 0, 2, 5));//15.45
//		ArrayList<Integer> coreIndex = new ArrayList<Integer>(Arrays.asList(4, 1, 6, 3, 0, 5, 2));//15.65
		
		boolean right = false;
		ArrayList<Integer> coreIndex = new ArrayList<Integer>();//15.65
		if(spectrumAvailable.containsKey(3) || spectrumAvailable.containsKey(2) || spectrumAvailable.containsKey(1) || spectrumAvailable.containsKey(0)) {
			coreIndex = new ArrayList<Integer>(Arrays.asList(3, 2, 1, 0));
		}
		else
		{
			right = true;
			coreIndex = new ArrayList<Integer>(Arrays.asList(4, 5, 6));
		}
		
		
		ArrayList<ArrayList<Slot>> candidates = new ArrayList<ArrayList<Slot>>();
		for(Integer key: coreIndex) {
			
			if(spectrumAvailable.containsKey(key)) 
			{ 
				ArrayList<Slot> candidate = getSetOfCandidates(spectrumAvailable.get(key), key, demandInSlots, links, flow);
				if(!candidate.isEmpty()) candidates.add(new ArrayList<Slot>(candidate));
			}
		}
		
		
		if(!candidates.isEmpty()) {
			
			if(right) {
				
				candidates.sort( (a , b) -> {
					int diff = a.get(0).s - b.get(0).s;
					
					if(diff != 0) {
						return diff;
					}
					
					return ( b.get(0).c - a.get(0).c );
				});
			}
			else {
				
				candidates.sort( (a , b) -> {
					int diff = a.get(0).s - b.get(0).s;
					
					if(diff != 0) {
						return diff;
					}
					
					return ( b.get(0).c - a.get(0).c );
				});
			}
			
			return establishConnection(links, candidates.get(0), flow.getModulationLevel(), flow);
		}
		
		return false;
	}

	
	private class FloodFill8 {
		
		private HashMap<Integer, ArrayList<Integer> > painted;
		private boolean [][]fillMap;
		private boolean [][]visited;
		private int m = 0;
		private int n = 0;
		
		public FloodFill8(boolean[][] matrix) {
			
			this.fillMap = matrix.clone();
			this.painted = new HashMap<Integer, ArrayList<Integer>>();
			this.n = fillMap.length;
			this.m = fillMap[0].length;
			
			this.visited = new boolean[n][m];
			for(int i = 0; i < n; i++) {
				for(int j = 0; j < m; j++) {
					this.visited[i][j] = false;
				}
			}
		}

		public HashMap<Integer, ArrayList<Integer>>  getFillingArea() {
			
			return this.painted;
		}
		
		public boolean[][]getFillMap() {
			
			return this.fillMap;
		}
		
		@SuppressWarnings("unused")
		public void printFillMap() {
			printSpectrum(fillMap);
		}

		public int runFloodFill(int x, int y) {

			if( x < 0 || x >= n || y < 0 || y >= m  ) 
			{
				return 0;
			}	
			
			if(visited[x][y]) 
			{
				return 0;
			}
			visited[x][y] = true;
			
			if(!fillMap[x][y]) {
				return 0;
			}
			
			fillMap[x][y] = false;
			if(!painted.containsKey(x)) 
			{
				ArrayList<Integer> t = new ArrayList<Integer>();
				painted.putIfAbsent(x, t);
			}
			if(!painted.get(x).contains(y)) 
			{
				painted.get(x).add(y);
			}
			
			int sum = 1;
			sum += runFloodFill(x+1, y);
			sum += runFloodFill(x+1, y+1);
			sum += runFloodFill(x+1, y-1);
			sum += runFloodFill(x-1, y);
			sum += runFloodFill(x-1, y+1);
			sum += runFloodFill(x-1, y-1);
			sum += runFloodFill(x, y+1);
			sum += runFloodFill(x, y-1);
			
			return sum;
		}
	}
}
