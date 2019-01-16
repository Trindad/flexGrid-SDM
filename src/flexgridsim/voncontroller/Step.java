package flexgridsim.voncontroller;

public class Step {
	public enum ACTIONS { 
			BLOCK_COSTLY_NODE,
			BLOCK_BALANCED_LINK,
			BLOCK_BALANCED_NODE,
			BLOCK_OVERLOADED_LINK,
			RECONFIGURATION_PERFORMANCE_LINK, 
			LIMIT_COSTLY_NODE, 
			LIMIT_BALANCED_LINK, 
			LIMIT_OVERLOAD_LINK, 
			REDIRECT_TRAFFIC, 
			LIMIT_OVERLOADED_LINK, 
			LIMIT_NON_BALANCED_LINK, 
			LIMIT_PERFORMANCE_LINK
		};
	
	
	public ACTIONS action;
	public String target;
	public int target_id;
	
	public Step(ACTIONS action, String target, int target_id) {
		super();
		this.action = action;
		this.target = target;
		this.target_id = target_id;
	}
	
	public Step(ACTIONS action, String target) {
		super();
		this.action = action;
		this.target = target;
	}
}

/**
 * new Step(ACTIONS.BLOCK_COSTLY_NODE, "node", 2)
 * new Step(ACTIONS.BLOCK_COSTLY_NODE, "link", 2)
 * new Step(ACTIONS.BLOCK_COSTLY_NODE, "all_nodes")
 */
