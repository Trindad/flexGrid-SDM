package flexgridsim.filters;

public class BlockCostlyNodeFilter extends BaseFilter {
	private int targetNode;
	
	public BlockCostlyNodeFilter(int target_id) {
		this.targetNode = target_id;
	}
	
	public boolean filter(int node) {
		return node != targetNode;
	}

	public boolean isDone()
	{
		return false;
	}
}
