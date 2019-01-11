package flexgridsim.voncontroller;

import java.util.ArrayList;

import flexgridsim.util.KNearestNeighbors;
import flexgridsim.voncontroller.Symptom.SYMPTOM;

/**
 * 
 * @author trindade
 *
 */
public class Analyze {
	
	private String filenameNonBalanced = "knn_configuring_analyze_nb.arff";
	private String filenameCostly = "knn_configuring_analyze_c.arff";
	private String filenamePerformance = "knn_configuring_analyze_p.arff";
	private String filenameOverload = "knn_configuring_analyze_o.arff";
	
	private KNearestNeighbors knnNonBalanced;
	private KNearestNeighbors knnCostly;
	private KNearestNeighbors knnPerformance;
	private KNearestNeighbors knnOverload;
	
	public Analyze() {
		
		this.knnNonBalanced = new KNearestNeighbors(filenameNonBalanced);
		this.knnCostly = new KNearestNeighbors(filenameCostly);
		this.knnPerformance = new KNearestNeighbors(filenamePerformance);
		this.knnOverload = new KNearestNeighbors(filenameOverload);
		
		try {
			knnPerformance.train();
			knnNonBalanced.train();
			knnCostly.train();
			knnOverload.train();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> run(Symptom symptom) throws Exception {
		
		ArrayList< ArrayList<Double> > data = symptom.dataset;
		ArrayList<String> classification = null;
		
		if(symptom.type == SYMPTOM.PERFORMANCE) 
		{
			classification = knnPerformance.run(data);
		}
		else if(symptom.type == SYMPTOM.NONBALANCED)
		{
			classification = knnNonBalanced.run(data);
		}
		else if(symptom.type == SYMPTOM.COSTLY) 
		{
			classification = knnCostly.run(data);
		}
		else if(symptom.type == SYMPTOM.OVERLOADED) {
			classification = knnOverload.run(data);
		}
		else 
		{
			System.err.println("This problem doesn't exist...");
		}
		
		return classification;
		
	}
}
