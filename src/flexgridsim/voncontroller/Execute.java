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
				System.out.println("ACTION - block node");
				BlockCostlyNodeFilter filter = new BlockCostlyNodeFilter(step.target_id);
				Hooks.blockCostlyNodeFilters.add(filter);
			}
			if (step.action == ACTIONS.BLOCK_BALANCED_LINK) {
				System.out.println("ACTION - block link");
				BlockNonBalancedLinkFilter filter = new BlockNonBalancedLinkFilter(step.target_id);
				Hooks.blockNonBalancedLinkFilters.add(filter);
			}
			if (step.action == ACTIONS.LIMIT_OVERLOAD_LINK) {
				System.out.println("ACTION - limiting link");
				LimitingOverloadLinkFilter filter = new LimitingOverloadLinkFilter(step.target_id);
				Hooks.limitingOverloadLinkFilters.add(filter);
			}
			if (step.action == ACTIONS.RECONFIGURATION_PERFORMANCE_LINK) {
				System.out.println("ACTION - reconfiguration");
				ReconfigurationPerfomanceFilter filter = new ReconfigurationPerfomanceFilter();
				Hooks.reconfigurationFilter = filter;
			}
			if(step.action == ACTIONS.REDIRECT_TRAFFIC) {
				System.out.println("ACTION - redicteting vlink");
				RedirectingLightpathFilter filter = new RedirectingLightpathFilter(step.target_id);
				Hooks.redirectFilters.add(filter);
			}
			if(step.action == ACTIONS.LIMIT_OVERLOADED_LINKS) {
//				System.out.println("ACTION - limiting links");
//				for() {
//					LimitingOverloadLinkFilter filter = new LimitingOverloadLinkFilter();
//					Hooks.limitingOverloadLinkFilters.add(filter);
//				}
			}
		}
	}
}
