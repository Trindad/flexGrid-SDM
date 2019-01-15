package flexgridsim;

import java.util.ArrayList;

import flexgridsim.filters.BlockCostlyNodeFilter;
import flexgridsim.filters.BlockNonBalancedLinkFilter;
import flexgridsim.filters.BlockOverloadedLinkFilter;
import flexgridsim.filters.LimitingOverloadLinkFilter;
import flexgridsim.filters.ReconfigurationPerfomanceFilter;
import flexgridsim.filters.RedirectingLightpathFilter;
import vne.VirtualNetworkEmbedding;

public class Hooks {
	public static ArrayList<BlockCostlyNodeFilter> blockCostlyNodeFilters;
	public static ArrayList<BlockNonBalancedLinkFilter> blockNonBalancedLinkFilters;
	public static ArrayList<LimitingOverloadLinkFilter> limitingOverloadLinkFilters;
	public static ArrayList<RedirectingLightpathFilter> redirectFilters;
	public static ArrayList<BlockOverloadedLinkFilter> blockOverloadedLinkFilters;
	public static ReconfigurationPerfomanceFilter reconfigurationFilter;

	public static void init() {
		blockCostlyNodeFilters = new ArrayList<>();
		blockNonBalancedLinkFilters = new ArrayList<>();
		limitingOverloadLinkFilters = new ArrayList<>();
		redirectFilters = new ArrayList<>();
		blockOverloadedLinkFilters = new ArrayList<>();
	}
	
	public static void reset() {
		blockCostlyNodeFilters.clear();
		blockNonBalancedLinkFilters.clear();
		limitingOverloadLinkFilters.clear();
		redirectFilters.clear();
	}
	
	public static void runPendingReconfiguration(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		if(reconfigurationFilter == null) return;

		reconfigurationFilter.run(pt, cp, vne);
		reconfigurationFilter = null;		
	}
	
	public static boolean runBlockCostlyNodeFilter(int node) {
		boolean b = true;

		for (BlockCostlyNodeFilter f : blockCostlyNodeFilters) {
			if (!f.filter(node)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static boolean runBlockNonBalancedLinkFilter(int link) {
		boolean b = true;

		for (BlockNonBalancedLinkFilter f : blockNonBalancedLinkFilters) {
			if (!f.filter(link)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static boolean runBlockOverloadedLinkFilter(int link) {
		boolean b = true;

		for (BlockOverloadedLinkFilter f : blockOverloadedLinkFilters) {
			if (!f.filter(link)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static boolean runLimitingOverloadLinkFilters(int link) {
		boolean b = true;

		for (LimitingOverloadLinkFilter f : limitingOverloadLinkFilters) {
			if (!f.filter(link)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static void checkDone(PhysicalTopology pt) {
		ArrayList<BlockCostlyNodeFilter> done1 = new ArrayList<>();
		ArrayList<BlockNonBalancedLinkFilter> done2 = new ArrayList<>();
		ArrayList<LimitingOverloadLinkFilter> done3 = new ArrayList<>();
		
		for (BlockCostlyNodeFilter f : blockCostlyNodeFilters) {
			if (f.isDone(pt)) {
				done1.add(f);
			}
		}
		
		for (BlockNonBalancedLinkFilter f : blockNonBalancedLinkFilters) {
			if (f.isDone(pt)) {
				done2.add(f);
			}
		}
		
		for (LimitingOverloadLinkFilter f : limitingOverloadLinkFilters) {
			if (f.isDone(pt)) {
				done3.add(f);
			}
		}
		
		blockCostlyNodeFilters.removeAll(done1);
		blockNonBalancedLinkFilters.removeAll(done2);
		limitingOverloadLinkFilters.removeAll(done3);
	}

}
