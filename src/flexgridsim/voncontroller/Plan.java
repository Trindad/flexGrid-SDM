package flexgridsim.voncontroller;

import java.util.ArrayList;

public class Plan {
	
	private ArrayList<Step> steps;
	private Symptom symptom;
	
	public Plan(Symptom symptom) {
		
		steps = new ArrayList<>();
		this.symptom = symptom;
	}
	
	public void addStep(Step step) {
		this.steps.add(step);
	}
	
	public ArrayList<Step> getSteps() {
		return steps;
	}
	
	public Symptom getSymptom() {
		return symptom;
	}
}
