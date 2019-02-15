package flexgridsim.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.HoeffdingTree;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class DecisionTree {
	private String trainingFilename;
	private String testFilename;
	
	private RandomForest classifier;

	
	public DecisionTree(String filename, String filenameTest) {
		System.out.println("Decision tree running...");
		
		this.trainingFilename = filename;
		this.testFilename = filenameTest;
	}
	
	
	public void train() throws Exception {
		
		Instances trainingDataset = getDataset(this.trainingFilename);

//		classifier = new J48();
		classifier = new RandomForest();
//		classifier.setBinarySplits(false);
//		classifier.setUnpruned(true);
//		classifier.setNumFolds(10);
//		classifier.setDoNotMakeSplitPointActualValue(false);
//		classifier.setNumDecimalPlaces(3);
//		classifier.setUseLaplace(true);
		classifier.buildClassifier(trainingDataset);
		
		Instances testingDataset = getDataset(this.testFilename);
		
//		System.out.println(testingDataset);
		
		Evaluation eval = new Evaluation(trainingDataset);
		eval.evaluateModel(classifier, testingDataset);
//		eval.crossValidateModel(classifier, testingDataset, 10, new Random());
		
//		System.out.println("** Decision Tress Evaluation with Datasets **");
		System.out.println(eval.toSummaryString());
		System.out.println(classifier.toString());
//		System.out.print(" the expression for the input data as per alogorithm is ");
//		System.out.println(classifier);
		System.out.println(eval.toMatrixString());
		System.out.println(eval.toClassDetailsString());
	}

	public Instances getDataset(String filename) throws IOException {
		
		ArffLoader loader = new ArffLoader();
//		System.out.println(filename);
		loader.setSource(DecisionTree.class.getResourceAsStream("/" + filename));
		
		Instances dataset = loader.getDataSet();
		dataset.setClassIndex(dataset.numAttributes() - 1);
		
		return dataset;
	}
	
	

	public String run(double[] data) {
		
		final Attribute bbr = new Attribute("bbr"); 
		final Attribute linkload = new Attribute("linkload"); 
		final Attribute acceptance = new Attribute("acceptance"); 
		final Attribute crosstalk = new Attribute("crosstalk"); 
		final Attribute transponders = new Attribute("transponders");
		final Attribute cost = new Attribute("cost"); 
		final Attribute fragementation = new Attribute("fragementation");
		final Attribute availableslots = new Attribute("availableslots");
		final Attribute availabletransponders = new Attribute("availabletransponders");
		@SuppressWarnings("serial")
		final List<String> classes = new ArrayList<String>() {
			{
				add("non-balanced");
				add("overloaded");
				add("perfect");
				add("performance");
				add("costly");
			}
		};
		
		@SuppressWarnings("serial")
		ArrayList<Attribute> attributeSet = new ArrayList<Attribute>(2) {
			{
				add(bbr);
				add(linkload);
				add(acceptance);
				add(crosstalk);
				add(transponders);
				add(cost);
				add(fragementation);
				add(availableslots);
				add(availabletransponders);
				Attribute attributeClass = new Attribute("@@class@@", classes);
				add(attributeClass);
				
			}
		};
		
		Instances dataUnpredicted = new Instances("Instances", attributeSet, 1);
		
		dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1);
		
		@SuppressWarnings("serial")
		DenseInstance newInstance = new DenseInstance(dataUnpredicted.numAttributes()) {
			{
				setValue(bbr, data[0]);
				setValue(linkload, data[1]);
				setValue(acceptance, data[2]);
				setValue(crosstalk, data[3]);
				setValue(transponders, data[4]);
				setValue(cost, data[5]);
				setValue(fragementation, data[6]);
				setValue(availableslots, data[7]);
				setValue(availabletransponders, data[8]);
			}
		};
		
		newInstance.setDataset(dataUnpredicted);
		
		try {
			double result = classifier.classifyInstance(newInstance);
			String className = classes.get(new Double(result).intValue());
			
//			classifier.updateClassifier(newInstance);
			
			return className;
	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
