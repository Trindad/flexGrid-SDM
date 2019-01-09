package flexgridsim.voncontroller;

import java.util.ArrayList;

import flexgridsim.voncontroller.Step.ACTIONS;
import flexgridsim.voncontroller.Symptom.SYMPTOM;

/**
 * 
 * @author trindade
 *
 */
public class Planner {

	public Plan run(ArrayList<String> classification, Symptom symptom) {
		Plan plan = new Plan();
		
		for (int i = 0; i < classification.size(); i++) {
			
			if (classification.get(i).equals("high") && symptom.type == SYMPTOM.COSTLY) {
				Step step = new Step(ACTIONS.BLOCK_COSTLY_NODE, "node", i);
				plan.addStep(step);
			}
			else if (classification.get(i).equals("high") && symptom.type == SYMPTOM.PERFORMANCE) {
				
				Step step = new Step(ACTIONS.RECONFIGURATION_PERFORMANCE_LINK, "link", i);
				plan.addStep(step);
			}
			else if (classification.get(i).equals("high") && symptom.type == SYMPTOM.NONBALANCED) {
				
				Step step = new Step(ACTIONS.BLOCK_BALANCED_LINK, "link", i);
				plan.addStep(step);
			}
			else if (classification.get(i).equals("medium") && symptom.type == SYMPTOM.COSTLY) {
				Step step = new Step(ACTIONS.LIMIT_COSTLY_NODE, "node", i);
				plan.addStep(step);
			}
			else if (classification.get(i).equals("medium") && symptom.type == SYMPTOM.PERFORMANCE) {
				
				Step step = new Step(ACTIONS.RECONFIGURATION_PERFORMANCE_LINK, "link", i);
				plan.addStep(step);
			}
			else if (classification.get(i).equals("medium") && symptom.type == SYMPTOM.NONBALANCED) {
				
				Step step = new Step(ACTIONS.LIMIT_BALANCED_LINK, "link", i);
				plan.addStep(step);
			}
		}
		
		return plan;
	}
}
