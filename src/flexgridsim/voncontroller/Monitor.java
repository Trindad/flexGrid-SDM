package flexgridsim.voncontroller;

import java.util.ArrayList;

import flexgridsim.Database;
import flexgridsim.util.DecisionTree;

/**
 * 
 * @author trindade
 *
 */
public class Monitor {
	
	private String filename = "dt_configuring_monitor.arff";
	
	public Monitor() {
		System.out.println("Initializing Monitor function");
	}
	
	public ArrayList<Symptom> run() throws Exception {
		
		Database db = Database.getInstance();
		ArrayList<Symptom> symptoms = new ArrayList<>();
		
		if(checkOverload()) {
			
			Symptom symptom = new Symptom(db.linkLoad, db.bbr, db.acceptance, db.totalTransponders, db.totalNumberOfTranspondersAvailable, db.cost);		
			symptoms.add(symptom);
		}	
		
		return symptoms;
	}

	public boolean checkOverload() throws Exception {
		System.out.println("Monitor");
		String[] test = {"0.1", "0.1", "0.1", "0.1", "0.2", "0.5"};
		
		DecisionTree dt = new DecisionTree(filename);
		dt.run(test);
		
		return false;
	}
}
