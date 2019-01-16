package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;

public class BlockNonBalancedLinkFilter {

	private int targetLink;
	
	public BlockNonBalancedLinkFilter(int id) {
		
		this.targetLink = id;
	}
	
	public boolean filter(int node) {
		return node != targetLink;
	}
	
	public boolean isDone(PhysicalTopology pt) {
		
		if(!check(pt)) 
		{	
			ShapedPlanRF.updateValue(new GridState(1,1), "right", -1);
			
			return false;
		}
		
		ShapedPlanRF.updateValue(new GridState(1,1), "right", 1);
		
		return true;
	}
	
	
	public boolean check(PhysicalTopology pt) {
		
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
