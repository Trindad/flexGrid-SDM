package flexgridsim.voncontroller;

import java.util.ArrayList;

/**
 * 
 * @author trindade
 *
 */
public class Knowledge {
	
	public ArrayList<Symptom> symptoms;//monitor
	public ArrayList< ArrayList<String> > classifications;//analyze
	public ArrayList<Plan> plans;
	
	public Knowledge() {
		
		this.symptoms = new ArrayList<Symptom>();
		classifications = new ArrayList< ArrayList<String> >();
	}
	
	public void addNewSymptom(Symptom symptom) {
		this.symptoms.add(symptom);
	}
	
	public void addNewClassification(ArrayList<String> classication) {
		classifications.add(classication);
	}

	public void addNewPlanner(Plan plan) {
		plans.add(plan);
	}

}
