package flexgridsim.voncontroller;

import java.util.ArrayList;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.LightPath;

/**
 * 
 * @author trindade
 *
 */
public class Knowledge {
	
	public ArrayList<Symptom> symptoms;//monitor
	public ArrayList< ArrayList<String> > classifications;//analyze
	
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

}
