package flexgridsim.voncontroller;

import java.util.ArrayList;

import flexgridsim.Database;
import flexgridsim.util.DecisionTree;
import flexgridsim.voncontroller.Symptom.SYMPTOM;

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
		
		String problem = checkOverload(db);
		System.out.println(db.bbr);
	
		if(!problem.equals("perfect")) 
		{
			System.out.println("SYMPTOM TYPE: "+problem);
			Symptom symptom = new Symptom(db.pt);
			
			symptom.type = SYMPTOM.valueOf(problem.toUpperCase().replace("-", ""));
//			symptom.type = SYMPTOM.COSTLY;
			
			symptom.setDataset(db);
			symptoms.add(symptom);
		}
		else {
			System.out.println("No problems "+problem);
		}
		
		return symptoms;
	}

	public String checkOverload(Database db) {
		
		double[] data = {db.bbr, db.linkLoad, db.acceptance, db.meanCrosstalk, db.totalTransponders, db.cost};
		System.out.println("bbr: "+db.bbr+" linkload: "+ db.linkLoad+" acceptance: "+ db.acceptance+" xt: "+ db.meanCrosstalk+" meantransponders: "+ db.totalTransponders+" cost: "+ db.cost);
		return dt.run(data);
	}
}
