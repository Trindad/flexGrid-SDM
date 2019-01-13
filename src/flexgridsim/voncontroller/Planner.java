package flexgridsim.voncontroller;

import java.util.ArrayList;

import flexgridsim.voncontroller.Step.ACTIONS;
import flexgridsim.voncontroller.Symptom.SYMPTOM;

import flexgridsim.util.ReinforcementLearning;

/**
 * 
 * @author trindade
 *
 */
public class Planner {
	
	private ReinforcementLearning rl;
	
	public Planner() {
		
		rl = new ReinforcementLearning();
		rl.QLearningExecute();
	}
	
	public Plan run(ArrayList<String> classification, Symptom symptom) {
		Plan plan = new Plan(symptom);
		
		for(int i = 0; i < classification.size(); i++) {
			String c = classification.get(i);
			
			String str = symptom.type.toString().toLowerCase() + "_" + c.toLowerCase();
			
			ArrayList<String> actions = rl.valueIteration(str);
			ArrayList<Step> steps = convertActionsToSteps(actions, symptom, i);
			
			for (Step e : steps) {
				plan.addStep(e);
			}
		}
		
		return plan;
	}

	private ArrayList<Step> convertActionsToSteps(ArrayList<String> actions, Symptom symptom, int i) {
		ArrayList<Step> steps = new ArrayList<>();
		
		for (String a : actions) {
			if (a.equals("block_node")) {
				Step step = new Step(ACTIONS.BLOCK_COSTLY_NODE, "node", i);
				steps.add(step);
			}
			else if (a.equals("defragment_network")) {
				
				Step step = new Step(ACTIONS.RECONFIGURATION_PERFORMANCE_LINK, "network");
				steps.add(step);
			}
			else if (a.equals("block_link")) {
				
				Step step = new Step(ACTIONS.BLOCK_BALANCED_LINK, "link", i);
				steps.add(step);
			}
			else if (a.equals("limit_link")) {
				
				Step step = new Step(ACTIONS.LIMIT_OVERLOAD_LINK, "link", i);
				steps.add(step);
			}
			else if (a.equals("limit_node")) {
				Step step = new Step(ACTIONS.LIMIT_COSTLY_NODE, "node", i);
				steps.add(step);
			}
			else if (a.equals("redirect_traffic")) {
				
				Step step = new Step(ACTIONS.REDIRECT_TRAFFIC, "link", i);
				steps.add(step);
			}
			else if (a.equals("limit_links")) {
				
				Step step = new Step(ACTIONS.LIMIT_OVERLOADED_LINKS, "links");
				steps.add(step);
			}
		}
		
		return steps;
	}
}
