package flexgridsim.voncontroller;

import java.util.ArrayList;

import flexgridsim.voncontroller.Symptom.SYMPTOM;


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
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			analyzer = new Analyze();
			plan = new Planner();
			execute = new Execute();
			knowledge = new Knowledge();
		
	}

	public void run() {
		
		try {
			ArrayList<Symptom> symptoms = monitor.run();
			
			if(symptoms.get(0).type == SYMPTOM.PERFECT) return;
			
			if(symptoms.isEmpty()) return;
			
			if(knowledge == null) System.out.println("ERRROR");
			
			for(Symptom symptom : symptoms) {
				knowledge.symptoms.add(symptom);
			}
			
			ArrayList<String> classification = analyzer.run(symptoms.get(0));
			
			if(!classification.isEmpty()) 
			{
				knowledge.classifications.add(classification);
				
				Plan executionPlan = plan.run(classification, symptoms.get(0) );
				knowledge.plans.add(executionPlan);
				
				execute.run(executionPlan);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}
}
