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
	
	public ArrayList<Symptom> symptoms;
	
	public Knowledge() {
		
		this.symptoms = new ArrayList<Symptom>();
	}
	
	public void addNewSymptom(Symptom symptom) {
		this.symptoms.add(symptom);
	}

}
