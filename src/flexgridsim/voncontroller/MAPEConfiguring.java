package flexgridsim.voncontroller;

import java.util.ArrayList;


/**
 * 
 * @author trindade
 *
 */
public class MAPEConfiguring {

	private Monitor monitor;
	private Analyze analyzer;
	private Planner plan;
	private Execute execute;
	private Knowledge knowledge;
	
	public MAPEConfiguring() {
		
		try {
			monitor = new Monitor();
			analyzer = new Analyze();
			plan = new Planner();
			execute = new Execute();
			knowledge = new Knowledge();
		}
		catch (Exception e) {
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
			
			ArrayList<String> classification = analyzer.run(symptoms.get(0));
			
			if(!classification.isEmpty()) {
				knowledge.classifications.add(classification);
				
				Plan executionPlan = plan.run(classification, symptoms.get(0) );
				execute.run(executionPlan);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}
}
