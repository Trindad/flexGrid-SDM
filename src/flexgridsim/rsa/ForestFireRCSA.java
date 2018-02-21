package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;

public class ForestFireRCSA extends FloodFillRCSA{
	
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
			
		int modulationLevel = chooseModulationFormat(flow, links);
		int demandInSlots = (int) Math.ceil((double)flow.getRate() / ModulationsMuticore.subcarriersCapacity[modulationLevel]);
		boolean [][]spectrum = bitMapAll(links);

		if(this.totalSlotsAvailable >= demandInSlots) 
		{
			ForestFire ff = new ForestFire(spectrum);

			while(modulationLevel >= 0) 
			{
				flow.addModulationLevel(modulationLevel);
				demandInSlots = (int) Math.ceil((double)flow.getRate() / ModulationsMuticore.subcarriersCapacity[modulationLevel]);
				
				if(totalSlotsAvailable >= demandInSlots) 
				{
					if(allocateConnection(flow, ff.getFillingArea(), links, demandInSlots)) 
					{
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

	class ForestFire {

		private HashMap<Integer, ArrayList<Integer> > fired;
		private boolean [][]matrix;
		
		public ForestFire(boolean[][] m) {
			this.matrix = m.clone();
			
			runForestFire(matrix[0], 0, 0);
		}
		
		public HashMap<Integer, ArrayList<Integer> > getFillingArea() {
			
			return fired;
		}

		public void runForestFire(boolean []grid, int w, int h) {
			int j = 0;
			while( j < (w * h) ) 
			{
				if(grid[j] == true) 
				{
					int y = j/w;
					int x =j%w;
					
					runForestFire(grid, x, y-1);// up
					runForestFire(grid, x, y+1);// down
					runForestFire(grid, x-1, y);// left
					runForestFire(grid, x+1, y);// right
					
				}	
				j += 1;
			}
			
			j = 0;
			while(j < (w * h) ) {
				if(grid[j] == true) {// burning --> out
					grid[j] = false;
				}
				if( grid[j] == false) {// ignited --> burn
					grid[j] = true;
					
				}
				
				j += 1;
			}
		}
		
		
	}

}
