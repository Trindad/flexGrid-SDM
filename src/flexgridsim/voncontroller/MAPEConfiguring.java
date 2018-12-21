package flexgridsim.voncontroller;

import java.util.ArrayList;

import weka.gui.SysErrLog;

/**
 * 
 * @author trindade
 *
 */
public class MAPEConfiguring {

	private Monitor monitor;
	private Analyze analyzer;
	private Plan plan;
	private Execute execute;
	private Knowledge knowledge;
	
	public MAPEConfiguring() {
		
		monitor = new Monitor();
		analyzer = new Analyze();
		plan = new Plan();
		execute = new Execute();
		knowledge = new Knowledge();
	}

	public void run() {
		
		try {
			ArrayList<Symptom> symptoms = monitor.run();
			
			if(symptoms.size() <= 0) return;
			
			for(Symptom symptom : symptoms) {
				knowledge.symptoms.add(symptom);
			}
			
			analyzer.run();
			plan.run();
			execute.run();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}
}
