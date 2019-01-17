package flexgridsim;

import java.util.ArrayList;

import flexgridsim.filters.BlockCostlyNodeFilter;
import flexgridsim.filters.BlockNonBalancedLinkFilter;
import flexgridsim.filters.BlockOverloadedLinkFilter;
import flexgridsim.filters.LimitCostlyNodeFilter;
import flexgridsim.filters.LimitingNonBalancedLinkFilter;
import flexgridsim.filters.LimitingOverloadLinkFilter;
import flexgridsim.filters.LimitingPerformanceLinkFilter;
import flexgridsim.filters.ReconfigurationPerfomanceFilter;
import flexgridsim.filters.RedirectingLightpathFilter;
import vne.VirtualNetworkEmbedding;

public class Hooks {
	
	public static ArrayList<BlockCostlyNodeFilter> blockCostlyNodeFilters;
	public static ArrayList<BlockNonBalancedLinkFilter> blockNonBalancedLinkFilters;
	public static ArrayList<LimitingOverloadLinkFilter> limitingOverloadLinkFilters;
	public static ArrayList<LimitingPerformanceLinkFilter> limitingPerformanceLinkFilters;
	public static ArrayList<LimitingNonBalancedLinkFilter> limitingNonBalancedLinkFilters;
	public static ArrayList<LimitCostlyNodeFilter> limitingCostlyNodeFilters;
	public static ArrayList<BlockOverloadedLinkFilter> blockOverloadedLinkFilters;
	
	public static ArrayList<RedirectingLightpathFilter> redirectFilters;
	public static ReconfigurationPerfomanceFilter reconfigurationFilter;

	public static void init() {
		
		blockCostlyNodeFilters = new ArrayList<>();
		blockNonBalancedLinkFilters = new ArrayList<>();
		limitingOverloadLinkFilters = new ArrayList<>();
		limitingPerformanceLinkFilters = new ArrayList<>();
		limitingNonBalancedLinkFilters = new ArrayList<>();
		redirectFilters = new ArrayList<>();
		blockOverloadedLinkFilters = new ArrayList<>();
		limitingCostlyNodeFilters = new ArrayList<>();
	}
	
	public static void reset() {
		
		blockCostlyNodeFilters.clear();
		blockNonBalancedLinkFilters.clear();
		limitingOverloadLinkFilters.clear();
		limitingNonBalancedLinkFilters.clear();
		limitingPerformanceLinkFilters.clear();
		redirectFilters.clear();
		blockOverloadedLinkFilters.clear();
		limitingCostlyNodeFilters.clear();
	}
	
	public static void runPendingReconfiguration(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		if(reconfigurationFilter == null) return;

		reconfigurationFilter.run(pt, cp, vne);
		reconfigurationFilter = null;		
	}
	
	public static void runPendingRedirectingLightpath(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		for(RedirectingLightpathFilter filter : redirectFilters) {
			filter.run(pt, cp, vne);
		}
		
		redirectFilters.clear();
	}
	
	public static boolean runBlockCostlyNodeFilter(int node, PhysicalTopology pt) {
		boolean b = true;

		for (BlockCostlyNodeFilter f : blockCostlyNodeFilters) {
			if (!f.filter(node) && !f.check(pt)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static boolean runLimitCostlyNodeFilter(int node, PhysicalTopology pt) {
		boolean b = true;

		for (LimitCostlyNodeFilter f : limitingCostlyNodeFilters) {
			if (!f.filter(node) && !f.check(pt)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static boolean runBlockNonBalancedLinkFilter(int link, PhysicalTopology pt) {
		boolean b = true;

		for (BlockNonBalancedLinkFilter f : blockNonBalancedLinkFilters) {
			if (!f.filter(link) && !f.check(pt)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static boolean runBlockOverloadedLinkFilter(int link, PhysicalTopology pt) {
		boolean b = true;

		for (BlockOverloadedLinkFilter f : blockOverloadedLinkFilters) {
			if (!f.filter(link)  && !f.check(pt)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static boolean runLimitingOverloadLinkFilters(int link, PhysicalTopology pt) {
		boolean b = true;

		for (LimitingOverloadLinkFilter f : limitingOverloadLinkFilters) {
			if (!f.filter(link) && !f.check(pt)) {
				b = false;
			}
		}
		
		return b;
	}

	
	public static boolean runLimitingPerformanceLinkFilters(int link, PhysicalTopology pt) {
		boolean b = true;

		for (LimitingPerformanceLinkFilter f : limitingPerformanceLinkFilters) {
			if (!f.filter(link) && !f.check(pt)) {
				b = false;
			}
		}
		
		return b;
	}

	
	public static boolean runLimitingNonBalancedLinkFilters(int link, PhysicalTopology pt) {
		boolean b = true;

		for (LimitingNonBalancedLinkFilter f : limitingNonBalancedLinkFilters) {
			if (!f.filter(link) && !f.check(pt)) {
				b = false;
			}
		}
		
		return b;
	}
	
	public static void checkDone(PhysicalTopology pt) {
		
		ArrayList<BlockCostlyNodeFilter> done1 = new ArrayList<>();
		ArrayList<BlockNonBalancedLinkFilter> done2 = new ArrayList<>();
		ArrayList<LimitingOverloadLinkFilter> done3 = new ArrayList<>();
		ArrayList<LimitingNonBalancedLinkFilter> done5 = new ArrayList<>();
		ArrayList<LimitingPerformanceLinkFilter> done6 = new ArrayList<>();
		ArrayList<LimitCostlyNodeFilter> done4 = new ArrayList<>();
		
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
		
		for (LimitingPerformanceLinkFilter f : limitingPerformanceLinkFilters) {
			if (f.isDone(pt)) {
				done6.add(f);
			}
		}
		
		for (LimitingNonBalancedLinkFilter f : limitingNonBalancedLinkFilters) {
			if (f.isDone(pt)) {
				done5.add(f);
			}
		}
		
		for (LimitCostlyNodeFilter f : limitingCostlyNodeFilters) {
			if (f.isDone(pt)) {
				done4.add(f);
			}
		}
		
		blockCostlyNodeFilters.removeAll(done1);
		blockNonBalancedLinkFilters.removeAll(done2);
		limitingOverloadLinkFilters.removeAll(done3);
		limitingNonBalancedLinkFilters.removeAll(done5);
		limitingPerformanceLinkFilters.removeAll(done6);
		limitingCostlyNodeFilters.removeAll(done4);
	}

}
