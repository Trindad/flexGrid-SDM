package flexgridsim.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.DenseInstance;
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
	
	

	public ArrayList<String> run(ArrayList< ArrayList<Double> > data) {
		ArrayList<String> results = new ArrayList<>();
		
		final Attribute attrA = new Attribute("a"); 
		final Attribute attrB = new Attribute("b"); 
		final Attribute attrC = new Attribute("c"); 
		
		final List<String> classes = new ArrayList<String>() {
			
			private static final long serialVersionUID = 1L;

			{
				add("high");
				add("medium");
				add("low");
			}
		};
		
		@SuppressWarnings("serial")
		ArrayList<Attribute> attributeSet = new ArrayList<Attribute>(2) {
			{
				add(attrA);
				add(attrB);
				add(attrC);
				Attribute attributeClass = new Attribute("@@class@@", classes);
				add(attributeClass);
				
			}
		};
		
		Instances dataUnpredicted = new Instances("Instances", attributeSet, 1);
		
		dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1);
		
		for (ArrayList<Double> row : data) {
			DenseInstance newInstance = new DenseInstance(dataUnpredicted.numAttributes()) {
				
				private static final long serialVersionUID = 1L;

				{
					setValue(attrA, row.get(0));
					setValue(attrB, row.get(1));
					setValue(attrC, row.get(2));
				}
			};
			
			newInstance.setDataset(dataUnpredicted);
			
			try {
				double result = classifier.classifyInstance(newInstance);
				results.add(classes.get(new Double(result).intValue()));
		
			}
			catch (Exception e) {
				results.add(null);
				e.printStackTrace();
			}
		}
		
		return results;
	}
}
