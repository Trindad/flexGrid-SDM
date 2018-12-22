package flexgridsim.voncontroller;

import flexgridsim.util.KNearestNeighbors;

/**
 * 
 * @author trindade
 *
 */
public class Analyze {
	
	private String filenameBalanced = "knn_configuring_monitor.arff";
	private KNearestNeighbors knn;
	
	public Analyze() {
		this.knn = new KNearestNeighbors(filenameBalanced);
		
	}
	
	public void run(Symptom symptom) throws Exception {
		
//		if(symptom.type == SYMPTOM.PERFORMANCE) 
//		{
			this.knn.train();
//		}
	}

	/**
	 * Analizar o balanceamento de carga da rede
	 * Analizar o custo 
	 * Verificar se mudanças precisam ocorrer para passar para o Plan
	 */
	
	//k-Nearest neighbors
}
