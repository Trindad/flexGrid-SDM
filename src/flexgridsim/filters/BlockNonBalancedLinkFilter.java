package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;

public class BlockNonBalancedLinkFilter {

	private int targetLink;
	
	public BlockNonBalancedLinkFilter(int id) {
		
		this.targetLink = id;
	}
	
	public boolean filter(int node) {
		return node != targetLink;
	}
	
	public boolean isDone(PhysicalTopology pt) {
		
		if(Database.getInstance().bbrPerPair[targetLink] > Database.getInstance().bbr) {
			
			return false;
		}
		
		int total = 0;
		for(Long link : Database.getInstance().slotsAvailable.keySet()) {
			int n = Database.getInstance().slotsAvailable.get(link);
			
			total += n;
		}
		
		if(Database.getInstance().slotsAvailablePerLink[targetLink] < (total/pt.getNumLinks())) {
			return false;
		}
		
		return true;
	}
}
