package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;

public class LimitingNonBalancedLinkFilter {
	
	private int targetLink;
	
	public LimitingNonBalancedLinkFilter(int id) {
		
		this.targetLink = id;
	}

	public boolean filter(int link) {
		return link != targetLink;
	}
	
	public boolean isDone(PhysicalTopology pt) {
	
		if(!check(pt)) 
		{
			ShapedPlanRF.updateValue(new GridState(5,1), "right", -1);
			ShapedPlanRF.updateValue(new GridState(4,2), "down", -1);
			return false;
		}
		
		
		ShapedPlanRF.updateValue(new GridState(5,1), "right", 1);
		ShapedPlanRF.updateValue(new GridState(4,2), "down", 1);
		return true;
	}

	public boolean check(PhysicalTopology pt) {

		if(Database.getInstance().bbrPerPair[targetLink] > 0.5) 
		{
			return false;
		}
		
		double ratio = ((double)Database.getInstance().slotsAvailablePerLink[targetLink]/(double) (pt.getNumSlots() * pt.getCores()));
		
		if(ratio < 0.35) 
		{
			return false;
		}
		
		if(Database.getInstance().xtLinks[targetLink] > Database.getInstance().meanCrosstalk) 
		{
			return false;
		}
		
		return true;
	}

}
