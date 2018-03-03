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
				this.paths.clear();
				return true;
			}
		}
		
		this.paths.clear();
//		System.out.println("Connection blocked: "+flow);
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
	
	
	protected boolean fitConnection(Flow flow, int []links) {
		
		int modulationFormat = chooseModulationFormat(flow.getRate(), links);
		
		if (modulationFormat >= 0) {
			
			int demandInSlots  = getNumberOfSlots(flow, links, modulationFormat);
			flow.setModulationLevel(modulationFormat);
			
			if(demandInSlots < 0 ) {
				return false;
			}
			
			int n = 0, i = 0;
			boolean [][]spectrum = bitMapAll(links);
			boolean [][]tempSpectrum = new boolean[pt.getCores()][pt.getNumSlots()];
			
			for(int c = 0; c < spectrum.length; c++ ) {
				for(int s = 0; s < spectrum[i].length; s++ ) {
					tempSpectrum[c][s] = false;
				}
			}
			
//			ArrayList<Integer> cores = new ArrayList<Integer>(Arrays.asList(6, 3, 2, 5, 0, 4, 1));//15.88
//			ArrayList<Integer> cores = new ArrayList<Integer>(Arrays.asList(6, 3, 2, 5, 0, 1, 4));//15.65
			ArrayList<Integer> cores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 4, 0, 2, 5));//15.45
//			ArrayList<Integer> cores = new ArrayList<Integer>(Arrays.asList(4, 1, 6, 3, 0, 5, 2));//15.65
//			ArrayList<Integer> cores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 4, 2, 5, 0));
//			ArrayList<Integer> cores = new ArrayList<Integer>(Arrays.asList(6, 5, 4, 3, 2, 1, 0));
			
			ArrayList<Integer> listOfCores = new ArrayList<Integer>();
			while(n < totalSlotsAvailable) {
				
				int t = 0;
				if(!listOfCores.contains(cores.get(i))) listOfCores.add(cores.get(i));
				if(!listOfCores.contains(cores.get(i+1))) listOfCores.add(cores.get(i+1));
				
				for(int c : listOfCores) {
				
					for(int s = 0; s < spectrum[c].length; s++) {
						if(spectrum[c][s]) {
							tempSpectrum[c][s] = true;
							t++;
						}
					}
				
				}
				
				int nVisited = 0;
	//			System.out.println(n+" "+t+" "+i+" "+(i+1));
				if(t >= 1 && (t-n) >= demandInSlots) 
				{
					FloodFill8 ff  = new FloodFill8(tempSpectrum.clone());
					
					while (nVisited < t) {
						
						int []coreAndSlot = getSeed(listOfCores, ff.getFillMap());
						if(coreAndSlot != null) {
							nVisited += ff.runFloodFill(coreAndSlot[0], coreAndSlot[1]);
						}
					}
					
					if(allocateConnection(flow, ff.getFillingArea(), links, demandInSlots)) 
					{
						return true;
					}
				}
				
				n = t; 
				
				if(i < 4) {
					i+= 2;
				}
				else {
					i++;
				}
			}
			
			modulationFormat--;
		}
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
//			return core >= 1 ? getSetOfCandidatesRight(slots,core,demandInSlots, links, flow): getSetOfCandidatesLeft(slots,core,demandInSlots, links, flow);
			
			return getSetOfCandidatesRight(slots,core,demandInSlots, links, flow);
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
		ArrayList<Integer> coreIndex = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 4, 0, 2, 5));//15.45
//		ArrayList<Integer> coreIndex = new ArrayList<Integer>(Arrays.asList(4, 1, 6, 3, 0, 5, 2));//15.65
//		ArrayList<Integer> coreIndex = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 4, 2, 5, 0));
		
		ArrayList<ArrayList<Slot>> candidates = new ArrayList<ArrayList<Slot>>();
		
//		ArrayList<Integer> coreIndex = new ArrayList<Integer>(spectrumAvailable.keySet());
		for(Integer key: coreIndex) {
			
			if(spectrumAvailable.containsKey(key)) 
			{ 
				ArrayList<Slot> candidate = getSetOfCandidates(spectrumAvailable.get(key), key, demandInSlots, links, flow);
				if(!candidate.isEmpty()) candidates.add(new ArrayList<Slot>(candidate));
			}
		}
		
		
		if(!candidates.isEmpty()) {
			
			candidates.sort( (a , b) -> a.get(0).s - b.get(0).s);
			
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
