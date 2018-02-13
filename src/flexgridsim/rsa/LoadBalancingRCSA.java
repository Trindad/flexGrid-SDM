package flexgridsim.rsa;

import java.util.ArrayList;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;

public class LoadBalancingRCSA extends SCVCRCSA{
	
	protected boolean runRCSA(Flow flow) {
		
		setkShortestPaths(flow);
		ArrayList<Integer> indices = closenessCentrality();
		for(int i = 0; i < indices.size(); i++) {
			
			if(fitConnection(flow, bitMapAll(this.paths.get( indices.get(i) )), this.paths.get( indices.get(i) ))) {
					return true;
			}
		}
		
		this.paths.clear();
		
		return false;
	}
	
	private ArrayList<Integer> closenessCentrality() {
    	
    	ClosenessCentrality<Integer,DefaultWeightedEdge> cc = new ClosenessCentrality<Integer,DefaultWeightedEdge>(pt.getGraph());
    
    	double []avgClosenessCentrality = new double[this.paths.size()];
    	ArrayList<Integer> indices = new ArrayList<Integer>();
    	for(int i = 0; i < this.paths.size(); i++) {
    		
    		int []links = this.paths.get(i);
    		avgClosenessCentrality[i] = 0;
    		
    		for(int j = 0; j < links.length; j++) {
    			
    			avgClosenessCentrality[i] += cc.getVertexScore(pt.getLink(links[j]).getDestination());
    			avgClosenessCentrality[i] += cc.getVertexScore(pt.getLink(links[j]).getSource());		
    		}
    		indices.add(i);
    	}
    	
    	indices.sort((a,b) -> (int)(avgClosenessCentrality[a] * 1000) - (int)(avgClosenessCentrality[b] * 1000) );
    	
    	return indices;
    }
}

