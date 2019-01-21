package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;

public class LimitCostlyNodeFilter {

	private int targetNode;
	
	public LimitCostlyNodeFilter(int id) {
		
		this.targetNode = id;
	}
	
	public boolean filter(int node) {
		return node != targetNode;
	}
	
	public boolean isDone(PhysicalTopology pt) {
		
		
		if(!check(pt)) 
		{	
			ShapedPlanRF.updateValue(new GridState(23,1), "down", -1);
			return false;
		}
		
		
		ShapedPlanRF.updateValue(new GridState(23,1), "down", 1);
		
		return true;
	}

	public boolean check(PhysicalTopology pt) {
		
		double total = pt.getNumNodes() * pt.transponders;
		double meanTransponders = (double)( total - Database.getInstance().totalTransponders ) / total;
		
		if(pt.getNode(targetNode).getTransponders() > meanTransponders) 
		{	
			
			return false;
		}
		
		double meanComputeResource = (double)Database.getInstance().totalComputeResource/ (double)pt.getNumNodes();
		
		if(pt.getNode(targetNode).getComputeResource() > meanComputeResource) 
		{
			
			return false;
		}	
		
		
		return true;
	}
}
