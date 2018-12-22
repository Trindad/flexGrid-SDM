package flexgridsim.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class KNearestNeighbors {
	
	private String trainingFilename;
	
	private IBk classifier;

	
	public KNearestNeighbors(String filename) {
		System.out.println("K-Nearest Neighbors running...");
		
		this.trainingFilename = filename;
	}
	
	
	public void train() throws Exception {
		System.out.println("K-Nearest Neighbors trainning...");
		
		Instances trainingDataset = getDataset(this.trainingFilename);

		classifier = new IBk(3);
		classifier.buildClassifier(trainingDataset);
	}

	public Instances getDataset(String filename) throws IOException {
		
		ArffLoader loader = new ArffLoader();
		System.out.println(filename);
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
		@SuppressWarnings("serial")
		final List<String> classes = new ArrayList<String>() {
			{
				add("non-balanced");
				add("overloaded");
				add("perfect");
				add("high-xt");
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
			}
		};
		
		newInstance.setDataset(dataUnpredicted);
		
		try {
			double result = classifier.classifyInstance(newInstance);
			String className = classes.get(new Double(result).intValue());
			
			classifier.updateClassifier(newInstance);
			
			return className;
	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
