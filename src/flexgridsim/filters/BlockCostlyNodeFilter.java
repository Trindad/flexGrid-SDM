package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;

public class BlockCostlyNodeFilter {
	private int targetNode;
	
	public BlockCostlyNodeFilter(int target_id) {
		this.targetNode = target_id;
	}
	
	public boolean filter(int node) {
		return node != targetNode;
	}

	public boolean isDone(PhysicalTopology pt)
	{
		double meanTransponders = Database.getInstance().totalTransponders / (pt.getNumNodes() * 5);
		
		if(pt.getNode(targetNode).getTransponders() > meanTransponders) 
		{	
			return false;
		}
		
		double meanComputeResource = Database.getInstance().totalComputeResource/ pt.getNumNodes();
		
		if(pt.getNode(targetNode).getComputeResource() > meanComputeResource) {
			return false;
		}
		
		return true;
	}
}
