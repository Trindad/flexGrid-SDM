package flexgridsim.voncontroller;
import flexgridsim.Hooks;
import flexgridsim.filters.BlockCostlyNodeFilter;
import flexgridsim.voncontroller.Step.ACTIONS;

import flexgridsim.util.ReinforcementLearning;
/**
 * 
 * @author trindade
 *
 */
public class Execute {

	public void run(Plan plan) {
		for (Step step : plan.getSteps()) {
			if (step.action == ACTIONS.BLOCK_COSTLY_NODE) {
				BlockCostlyNodeFilter filter = new BlockCostlyNodeFilter(step.target_id);
				Hooks.blockCostlyNodeFilters.add(filter);
			}
			
		}
	}
}
