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
	private String filenameTest = "dt_configuring_monitor_test.arff";
	
	public Monitor() {
		System.out.println("Initializing Monitor function");
	}
	
	public ArrayList<Symptom> run() throws Exception {
		
		Database db = Database.getInstance();
		ArrayList<Symptom> symptoms = new ArrayList<>();
		
		if(checkOverload(db)) {
			
			Symptom symptom = new Symptom(db.linkLoad, db.bbr, db.acceptance, db.totalTransponders, db.totalNumberOfTranspondersAvailable, db.cost);		
			symptoms.add(symptom);
		}	
		
		return symptoms;
	}

	public boolean checkOverload(Database db) throws Exception {
		System.out.println("Monitor");
		
//		double t =  ((double)db.totalNumberOfTranspondersAvailable)/(double)db.totalTransponders;
//		
//		
//		String[] test = {Double.toString(db.bbr),Double.toString(db.linkLoad), Double.toString(db.acceptance), Double.toString(0.0),
//				Double.toString(t), Double.toString(db.cost), "?"};
		
		
		DecisionTree dt = new DecisionTree(filename, filenameTest);
		dt.run();
		
		return false;
	}
}
