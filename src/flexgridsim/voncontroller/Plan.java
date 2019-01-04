package flexgridsim.voncontroller;

import java.util.ArrayList;

public class Plan {
	
	private ArrayList<Step> steps;
	
	public Plan() {
		
		steps = new ArrayList<>();
	}
	
	public void addStep(Step step) {
		this.steps.add(step);
	}
	
	public ArrayList<Step> getSteps() {
		return steps;
	}
}
