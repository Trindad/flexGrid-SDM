package flexgridsim.voncontroller;

import java.util.ArrayList;

import flexgridsim.voncontroller.Step.ACTIONS;

/**
 * 
 * @author trindade
 *
 */
public class Planner {

	public Plan run(ArrayList<String> classification, Symptom symptom) {
		Plan plan = new Plan();
		
		for (int i = 0; i < classification.size(); i++) {
//			System.out.println("LOOOL: " + classification.get(i));
			if (classification.get(i).equals("high")) {
				Step blockNode = new Step(ACTIONS.BLOCK_COSTLY_NODE, "node", i);
				plan.addStep(blockNode);
			}
		}
		
		return plan;
	}

	
	//árvore de decisão
	//TD algoritmo
}
