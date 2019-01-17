package flexgridsim.voncontroller;

import java.util.ArrayList;
import java.util.Collections;

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
		
		if(classification.contains("high") && symptom.type == SYMPTOM.PERFORMANCE) {
			
			int count = Collections.frequency(classification, "high");
			
			if(count < ( (double)classification.size()*0.3) ) 
			{
				Collections.replaceAll(classification, "high", "medium");
			}
			else {
				System.out.println("Performance aborted");
			}
		}
		
		for(int i = 0; i < classification.size(); i++) {
			String c = classification.get(i);
			
			String str = symptom.type.toString().toLowerCase() + "_" + c.toLowerCase();
			
			ArrayList<String> actions = rl.valueIteration(str);
			ArrayList<Step> steps = convertActionsToSteps(actions, symptom, i);
			
			for (Step e : steps) {
				plan.addStep(e);
			}
			
			if(!plan.getSteps().isEmpty() && plan.getSteps().get(0).action.equals(ACTIONS.RECONFIGURATION_PERFORMANCE_LINK) )
			{
				break;
			}
			
		}
		
		
		return plan;
	}

	private ArrayList<Step> convertActionsToSteps(ArrayList<String> actions, Symptom symptom, int i) {
		ArrayList<Step> steps = new ArrayList<>();
		
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 27));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 11));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 13));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 9));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 30));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 32));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 2));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 3));
		steps.add(new Step(ACTIONS.REDIRECT_TRAFFIC, "link", 4));
		
		return steps;
		
//		if (actions.contains("defragment_network")) {
//			Step step = new Step(ACTIONS.RECONFIGURATION_PERFORMANCE_LINK, "network");
//			steps.add(step);
//			return steps;
//		}
//		
//		for (String a : actions) {
//			if (a.equals("block_node")) {
//				Step step = new Step(ACTIONS.BLOCK_COSTLY_NODE, "node", i);
//				steps.add(step);
//			}
//			else if (a.equals("block_link")) {
//				
//				Step step = new Step(ACTIONS.BLOCK_BALANCED_LINK, "link", i);
//				steps.add(step);
//			}
//			else if (a.equals("block_link_overloaded")) {
//				
//				Step step = new Step(ACTIONS.BLOCK_OVERLOADED_LINK, "link", i);
//				steps.add(step);
//			}
//			else if (a.equals("limit_overloaded_link")) {
//				
//				Step step = new Step(ACTIONS.LIMIT_OVERLOAD_LINK, "link", i);
//				steps.add(step);
//			}
//			else if (a.equals("limit_non_balanced_link")) {
//				
//				Step step = new Step(ACTIONS.LIMIT_NON_BALANCED_LINK, "link", i);
//				steps.add(step);
//			}
//			else if (a.equals("limit_performance_link")) {
//				
//				Step step = new Step(ACTIONS.LIMIT_PERFORMANCE_LINK, "link", i);
//				steps.add(step);
//			}
//			else if (a.equals("limit_node")) {
//				Step step = new Step(ACTIONS.LIMIT_COSTLY_NODE, "node", i);
//				steps.add(step);
//			}
//			else if (a.equals("redirect_traffic")) {
//				
//				Step step = new Step(ACTIONS.REDIRECT_TRAFFIC, "link", i);
//				steps.add(step);
//			}
//				
//		}
//		
//		return steps;
	}
}
