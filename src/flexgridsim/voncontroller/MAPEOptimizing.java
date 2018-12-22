
package flexgridsim.voncontroller;

import java.util.ArrayList;

/**
 * 
 * @author trindade
 *
 */

public class MAPEOptimizing {

	private Monitor monitor;
	private Analyze analyzer;
	private Plan plan;
	private Execute execute;
	private Knowledge knowledge;
	
	public MAPEOptimizing() {
		
		try {
			monitor = new Monitor();
			analyzer = new Analyze();
			plan = new Plan();
			execute = new Execute();
			knowledge = new Knowledge();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void run() {
		
		try {
			ArrayList<Symptom> symptoms = monitor.run();
			
			if(symptoms.size() <= 0) return;
			
			for(Symptom symptom : symptoms) {
				knowledge.symptoms.add(symptom);
			}
			
			analyzer.run(symptoms.get(0));
			plan.run();
			execute.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
