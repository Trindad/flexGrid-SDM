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
		
		double total = pt.getNumNodes() * 5.0;
		double meanTransponders = (double)( total - Database.getInstance().totalTransponders ) / total;
		
		if(pt.getNode(targetNode).getTransponders() > meanTransponders) 
		{	
			return false;
		}
		
		double meanComputeResource = (double)Database.getInstance().totalComputeResource/ (double)pt.getNumNodes();
		
		if(pt.getNode(targetNode).getComputeResource() > meanComputeResource) {
			return false;
		}
		
		
		return true;
	}
}
