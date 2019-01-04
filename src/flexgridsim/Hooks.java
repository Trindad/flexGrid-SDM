package flexgridsim;

import java.util.ArrayList;

import flexgridsim.filters.BlockCostlyNodeFilter;

public class Hooks {
	public static ArrayList<BlockCostlyNodeFilter> blockCostlyNodeFilters;
	
	public static void init() {
		blockCostlyNodeFilters = new ArrayList<>();
	}
	
	public static void reset() {
		blockCostlyNodeFilters.clear();
	}
	
	public static boolean runBlockCostlyNodeFilter(int node) {
		boolean b = true;
		
//		System.out.println("FILTERS: " + blockCostlyNodeFilters.size());
		
		for (BlockCostlyNodeFilter f : blockCostlyNodeFilters) {
			if (!f.filter(node)) {
				b = false;
			}
		}
		
		return b;
	}
}
