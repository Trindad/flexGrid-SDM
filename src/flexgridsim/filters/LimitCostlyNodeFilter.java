package flexgridsim.filters;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;

public class LimitCostlyNodeFilter {

	private int targetNode;
	
	public LimitCostlyNodeFilter(int id) {
		
		this.targetNode = id;
	}
	
	public boolean filter(int node) {
		return node != targetNode;
	}
	
	public boolean isDone(PhysicalTopology pt) {
		
		double total = pt.getNumNodes() * 5.0;
		double meanTransponders = (double)( total - Database.getInstance().totalTransponders ) / total;
		
		if(pt.getNode(targetNode).getTransponders() > (meanTransponders + meanTransponders * 0.25)) 
		{	
			return false;
		}
		
		double meanComputeResource = (double)Database.getInstance().totalComputeResource/ (double)pt.getNumNodes();
		
		if(pt.getNode(targetNode).getComputeResource() > (meanComputeResource + meanComputeResource * 0.25)) {
			return false;
		}	
		
		return true;
	}
}
