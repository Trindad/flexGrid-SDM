package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;

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
		
		if(ratio < 0.4) 
		{
			return false;
		}
		
		if(Database.getInstance().xtLinks[targetLink] > Database.getInstance().meanCrosstalk) {
			return false;
		}
		
		return true;
	}

}
