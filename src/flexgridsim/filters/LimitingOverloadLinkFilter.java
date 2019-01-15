package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;

public class LimitingOverloadLinkFilter {
	
	private int targetLink;
	
	public LimitingOverloadLinkFilter(int id) {
		
		this.targetLink = id;
	}

	public boolean filter(int link) {
		return link != targetLink;
	}
	
	public boolean isDone(PhysicalTopology pt) {
		
		
		if(Database.getInstance().bbrPerPair[targetLink] > 0.5) 
		{
			
			return false;
		}
		
		double ratio = ((double)Database.getInstance().slotsAvailablePerLink[targetLink]/(double) (pt.getNumSlots() * pt.getCores()));
		
		if(ratio < 0.35) 
		{
			ShapedPlanRF.updateValue(new GridState(30,1), "right", -1);
			return false;
		}
		
		if(Database.getInstance().xtLinks[targetLink] > Database.getInstance().meanCrosstalk) {
			ShapedPlanRF.updateValue(new GridState(30,1), "right", -1);
			return false;
		}
		
		return true;
	}

}
