package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class LoadBalancingRCSA extends XTFFRCSA {
	
	protected boolean runRCSA(Flow flow) {
		kPaths = 3;
		setkShortestPaths(flow);
		ArrayList<Integer> indices = orderByClosenessCentralityAndFragmentationIndex(getkShortestPaths());
		for(int i = 0; i < 3; i++) {
			int []links = this.paths.get(indices.get(i));
			if(fitConnection(flow, bitMapAll(links), links)) {
				this.paths.clear();
				return true;
			}
		}
		
		getkShortestPaths().clear();
		return false;
	}
	
	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 5, 4, 3, 2 , 1 , 0));
//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(5, 1, 3, 4, 6 , 2 , 0));
//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(1, 3, 5, 4, 6, 2, 0));
//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(1, 5, 3, 4, 6 , 2 , 0));
		for(int c = 0; c < priorityCores.size() ; c++) {
			int i = priorityCores.get(c);
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int j = 0; j < spectrum[i].length; j++) {	
				
				if(spectrum[i][j] == true) 
				{
					temp.add( new Slot(i,j) );
				}
				else {
					
					temp.clear();
					if(Math.abs(spectrum[i].length-j) < demandInSlots) break;
				}
				
				if(temp.size() == demandInSlots) {
					
					if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {

						return temp;
					}
					
					break;
				}
			}
		}
	    
		return new ArrayList<Slot>();
	}


	private double []getFragmentationRatio() {
    	
    	int nLinks = pt.getNumLinks();
    	double []fi = new double[nLinks];
    	double nSlots = (pt.getNumSlots() * pt.getCores());
    	
    	for(int i = 0; i < nLinks; i++) {
    		
    		fi[i] =  (double)(nSlots - (double)pt.getLink(i).getNumFreeSlots()) / nSlots;
    	}
    	
    	return fi;
	}
	
	
	public ArrayList<Integer> orderByClosenessCentralityAndFragmentationIndex(ArrayList<int[]> p) {
    	
    	ClosenessCentrality<Integer,DefaultWeightedEdge> cc = new ClosenessCentrality<Integer,DefaultWeightedEdge>(pt.getGraph());
    
    	double []sumRisc = new double[p.size()];
    	ArrayList<Integer> indices = new ArrayList<Integer>();
    	
    	double []fi = getFragmentationRatio();
    	int i = 0;
    	for(int []links: p) {
    		sumRisc[i] = 0;
    		double a = 0, b = 0;
    		for(int index : links) {
    			b += (cc.getVertexScore(pt.getLink(index).getDestination()) + cc.getVertexScore(pt.getLink(index).getSource()) * 2.0);
    			a += fi[index];
    		}
    		
    		double k1 = 1, k2 = 1;
    		a = 1 + Math.exp(k1 * (a - 0.8) );
    		b = 1 + Math.exp(k2 * ( b - 0.5) );
    		sumRisc[i] = ( ( 1.0 / a ) * ( 1.0 / b ) ) * 100;
    		indices.add(i);
    		i++;
    	}
    	
    	indices.sort((a,b) -> (int)(sumRisc[b] - sumRisc[a]) );
    	return indices;
    }
}

