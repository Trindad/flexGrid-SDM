package flexgridsim.voncontroller;

public class Step {
	public enum ACTIONS { BLOCK_COSTLY_NODE };
	
	
	public ACTIONS action;
	public String target;
	public int target_id;
}

/**
 * new Step(ACTIONS.BLOCK_COSTLY_NODE, "node", 2)
 * new Step(ACTIONS.BLOCK_COSTLY_NODE, "link", 2)
 * new Step(ACTIONS.BLOCK_COSTLY_NODE, "all_nodes")
 */
