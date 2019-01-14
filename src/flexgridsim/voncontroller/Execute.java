package flexgridsim.voncontroller;
import flexgridsim.Hooks;
import flexgridsim.filters.BlockCostlyNodeFilter;
import flexgridsim.filters.BlockNonBalancedLinkFilter;
import flexgridsim.filters.LimitingOverloadLinkFilter;
import flexgridsim.filters.ReconfigurationPerfomanceFilter;
import flexgridsim.filters.RedirectingLightpathFilter;
import flexgridsim.voncontroller.Step.ACTIONS;

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
			if (step.action == ACTIONS.BLOCK_BALANCED_LINK) {
				BlockNonBalancedLinkFilter filter = new BlockNonBalancedLinkFilter(step.target_id);
				Hooks.blockNonBalancedLinkFilters.add(filter);
			}
			if (step.action == ACTIONS.LIMIT_OVERLOAD_LINK) {
				LimitingOverloadLinkFilter filter = new LimitingOverloadLinkFilter(step.target_id);
				Hooks.limitingOverloadLinkFilters.add(filter);
			}
			if (step.action == ACTIONS.RECONFIGURATION_PERFORMANCE_LINK) {
				ReconfigurationPerfomanceFilter filter = new ReconfigurationPerfomanceFilter();
				Hooks.reconfigurationFilter = filter;
			}
			if(step.action == ACTIONS.REDIRECT_TRAFFIC) {
				RedirectingLightpathFilter filter = new RedirectingLightpathFilter(step.target_id);
				Hooks.redirectFilters.add(filter);
			}
			if(step.action == ACTIONS.LIMIT_OVERLOADED_LINKS) {
				LimitingOverloadLinkFilter filter = new LimitingOverloadLinkFilter(step.target_id);
				Hooks.limitingOverloadLinkFilters.add(filter);
			}
		}
	}
}
