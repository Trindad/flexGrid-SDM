package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class FloodFillRCSA extends LoadBalancingRCSA{

	protected boolean runRCSA(Flow flow) {
		
		setkShortestPaths(flow);
		
		for(int i = 0; i < paths.size(); i++) {
			
			if(fitConnection(flow, paths.get(i))) {
				this.paths.clear();
				return true;
			}
		}
		
		this.paths.clear();
//		System.out.println("Connection blocked: "+flow);
		return false;
	}
	
	
	private boolean fitConnection(Flow flow, int []links) {
		
		int modulationLevel = chooseModulationFormat(flow.getRate(), links);
		int demandInSlots = (int) Math.ceil((double)flow.getRate() / ModulationsMuticore.subcarriersCapacity[modulationLevel]);
		boolean [][]spectrum = bitMapAll(links);

		if(this.totalSlotsAvailable >= demandInSlots) 
		{
			
			FloodFill8 ff = new FloodFill8(spectrum, pt.getCores(), pt.getNumSlots());
			int n = 0;
			while(n < totalSlotsAvailable) 
			{
				int []coreAndSlot = getRandomNumbers(ff.getFillMap());
				if(coreAndSlot != null) 
				{
					n += ff.runFloodFill(coreAndSlot[0], coreAndSlot[1]);
				}
			}
			
			while(modulationLevel >= 0) 
			{
				flow.addModulationLevel(modulationLevel);
				demandInSlots = (int) Math.ceil((double)flow.getRate() / ModulationsMuticore.subcarriersCapacity[modulationLevel]);
				
				if(totalSlotsAvailable >= demandInSlots) 
				{
					if(allocateConnection(flow, ff.getFillingArea(), links, demandInSlots)) 
					{
//						System.out.println("Connection accepted: "+flow);
						return true;
					}
				}
				else 
				{
					return false;
				}
				
				modulationLevel--;
			}
		}
		
		return false;
	}

	private int[]getPositionNumbers(boolean[][] spectrum) {
		
		int []numbers = new int[2];
		
		for(int i = 0; i < spectrum.length; i++) {
//			System.out.println(Arrays.toString(spectrum[i]));
			for(int j = 0; j < spectrum[i].length; j++) {
				if(spectrum[i][j]) 
				{
					numbers[0] = i;
					numbers[1] = j;
					
//					System.out.println("position (" + i + ", " + j + ")");
					
					return numbers;
				}
			}
		}
		
		return null;
	}


	private int[]getRandomNumbers(boolean[][] spectrum) {
		
		int []numbers = new int[2];
		int i = 0;
		while(i < 5) {
			
			int k = (int)(Math.random() * spectrum.length + 1);
			int j = (int)(Math.random() * spectrum[k-1].length + 1);
			if(spectrum[k-1][j-1]) {
				
				numbers[0] = k-1;
				numbers[1] = j-1;
				return numbers;
			}
			
			i++;
		}
		
		return getPositionNumbers(spectrum);
	}


	protected boolean createSlotList(ArrayList<Integer> slots, int core, int demandInSlots, int []links, Flow flow) {
		
		ArrayList<Slot> slotList = new ArrayList<Slot>();
		Collections.sort(slots);
		
//		System.out.println(Arrays.toString(slots.toArray()));
		if(slots.size() >= demandInSlots) {
		
			for(int i = 0; i < slots.size(); i++) {
				
				if(i == 0) 
				{
					slotList.add(new Slot(core, slots.get(i)));
				}
				else if( Math.abs(slots.get(i) - slots.get(i-1)) == 1 ) {
					slotList.add(new Slot(core, slots.get(i)));
				}
				else 
				{
					slotList.clear();
					slotList.add(new Slot(core, slots.get(i)));
				}
				
				if(slotList.size() == demandInSlots) {
					
					if(cp.CrosstalkIsAcceptable(flow, links, slotList, ModulationsMuticore.inBandXT[flow.getModulationLevel()])) {
				
						if(establishConnection(links, slotList, flow.getModulationLevel(), flow)) 
						{
							return true;
						}
					}
					slotList.clear();
				}
			}
		}
		
		return false;
	}
	
	public boolean allocateConnection(Flow flow, HashMap<Integer, ArrayList<Integer>> spectrumAvailable, int[]links, int demandInSlots) {

		ArrayList<Integer> coreIndex = new ArrayList<Integer>();
		coreIndex.addAll(spectrumAvailable.keySet());
		
//		System.out.println(Arrays.toString(coreIndex.toArray()));
//		coreIndex.sort( (a,b) -> spectrumAvailable.get(b).size() - spectrumAvailable.get(a).size());
		
		for(Integer key: coreIndex) {
			
			if(createSlotList(spectrumAvailable.get(key), key, demandInSlots, links, flow)) {
				return true;
			}

		}
		
		return false;
	}

	private class FloodFill8 {
		
		private HashMap<Integer, ArrayList<Integer> > painted;
		private boolean [][]fillMap;
		private boolean [][]visited;
		private int m = 0;
		private int n = 0;
		
		public FloodFill8(boolean[][] matrix, int n, int m) {
			
			this.fillMap = matrix.clone();
			this.painted = new HashMap<Integer, ArrayList<Integer>>();
			this.m = m;
			this.n = n;
			
			this.visited = new boolean[n][m];
			for(int i = 0; i < n; i++) {
				for(int j = 0; j < m; j++) {
					this.visited[i][j] = false;
				}
			}
		}

		public HashMap<Integer, ArrayList<Integer>>  getFillingArea() {
			
			return painted;
		}
		
		public boolean[][]getFillMap() {
			
			return fillMap;
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
