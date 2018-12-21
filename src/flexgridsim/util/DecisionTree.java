package flexgridsim.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class DecisionTree {
	String trainingFilename;
	
	public DecisionTree(String filename) {
		System.out.println("Decision tree running...");
		
		this.trainingFilename = filename;
	}
	
	public void run(String[] test) throws Exception {
		Instances trainingDataset = getDataset(this.trainingFilename);
		
		Classifier classifier = new J48();
		classifier.buildClassifier(trainingDataset);
		Evaluation eval = new Evaluation(trainingDataset);
		eval.evaluateModel(classifier, test);
		
		
		System.out.println("** Decision Tress Evaluation with Datasets **");
		System.out.println(eval.toSummaryString());
		System.out.print(" the expression for the input data as per alogorithm is ");
		System.out.println(classifier);
		System.out.println(eval.toMatrixString());
		System.out.println(eval.toClassDetailsString());
	}

	public Instances getDataset(String filename) throws IOException {
		
		ArffLoader loader = new ArffLoader();
		System.out.println(filename);
		loader.setSource(DecisionTree.class.getResourceAsStream("/" + filename));
		
		Instances dataset = loader.getDataSet();
		dataset.setClassIndex(dataset.numAttributes() - 1);
		
		return dataset;
	}
}
