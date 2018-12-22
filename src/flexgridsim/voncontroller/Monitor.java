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
	private DecisionTree dt;
	
	public Monitor() throws Exception {
		System.out.println("Initializing Monitor function");
		
		this.dt = new DecisionTree(filename, filenameTest);
		this.dt.train();
	}
	
	public ArrayList<Symptom> run() {
		
		Database db = Database.getInstance();
		ArrayList<Symptom> symptoms = new ArrayList<>();
		
		if(checkOverload(db)) {
			
			Symptom symptom = new Symptom(db.linkLoad, db.bbr, db.acceptance, db.totalTransponders, db.totalNumberOfTranspondersAvailable, db.cost);		
			symptoms.add(symptom);
		}	
		
		return symptoms;
	}

	public boolean checkOverload(Database db) {
		System.out.println("Monitor");
		
		double t =  ((double)db.totalNumberOfTranspondersAvailable)/(double)db.totalTransponders;
//		
//		
		double[] data = {db.bbr, db.linkLoad, db.acceptance, 0.0, t, db.cost};
		
		
		System.out.println(dt.run(data));
		
		return false;
	}
}
