package flexgridsim.rsa;

import java.util.ArrayList;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;

public class LoadBalancingRCSA extends SCVCRCSA{
	
	protected boolean runRCSA(Flow flow) {
		
		setkShortestPaths(flow);
		ArrayList<Integer> indices = closenessCentrality();
		kPaths = 6;
//		System.out.println(flow);
		for(Integer i: indices) {
			
			if(fitConnection(flow, bitMapAll(this.paths.get( i )), this.paths.get( i ))) 
			{
				return true;
			}
		}
		
		return false;
	}
	
	public ArrayList<Integer> closenessCentrality() {
    	
    	ClosenessCentrality<Integer,DefaultWeightedEdge> cc = new ClosenessCentrality<Integer,DefaultWeightedEdge>(pt.getGraph());
    
    	double []avgClosenessCentrality = new double[this.paths.size()];
    	ArrayList<Integer> indices = new ArrayList<Integer>();
    	int i = 0;
    	for(int []links: this.paths) {
    		
    		avgClosenessCentrality[i] = 0;
    		
    		for(int j = 0; j < links.length; j++) {
    			int index = links[j];
    			avgClosenessCentrality[i] += cc.getVertexScore(pt.getLink(index).getDestination()) > avgClosenessCentrality[i] ? 
    					cc.getVertexScore(pt.getLink(index).getDestination()) : 0;
    		}
    		
    		indices.add(i);
    		i++;
    	}
    	
    	indices.sort((a,b) -> (int)(avgClosenessCentrality[a] * 100) - (int)(avgClosenessCentrality[b] * 100) );
    	
    	return indices;
    }
}

