package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;

public class BlockOverloadedLinkFilter {
	private int targetLink;
	
	public BlockOverloadedLinkFilter(int id) {
		
		this.targetLink = id;
	}
	
	public boolean filter(int link) {
		return link != targetLink;
	}
	
	public boolean isDone(PhysicalTopology pt) {
		
		if(Database.getInstance().bbrPerPair[targetLink] > (Database.getInstance().bbr * 0.75))
		{
			ShapedPlanRF.updateValue(new GridState(30,1), "right", -1);
			return false;
		}
		
		double total = 0;
		for(Long link : Database.getInstance().slotsAvailable.keySet()) {
			int n = Database.getInstance().slotsAvailable.get(link);
			
			total += n;
		}
		
		double ratio = ((double)Database.getInstance().slotsAvailablePerLink[targetLink]/(double) (pt.getNumSlots() * pt.getCores()));
		
		if(ratio < (total/(double)pt.getNumLinks())) 
		{
			ShapedPlanRF.updateValue(new GridState(30,1), "right", -1);
			return false;
		}
		
		ShapedPlanRF.updateValue(new GridState(30,1), "right", 1);
		
		return true;
	}

}
