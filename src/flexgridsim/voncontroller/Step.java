package flexgridsim.voncontroller;

public class Step {
	public enum ACTIONS { BLOCK_COSTLY_NODE };
	
	
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
