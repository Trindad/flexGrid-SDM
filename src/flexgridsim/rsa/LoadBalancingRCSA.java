package flexgridsim.rsa;

import java.util.ArrayList;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class LoadBalancingRCSA extends XTFFRCSA {
	
	protected boolean runRCSA(Flow flow) {
		
		setkShortestPaths(flow);
		ArrayList<Integer> indices = orderByClosenessCentralityAndFragmentationIndex(getkShortestPaths());

		for(Integer i: indices) {
			
			if(fitConnection(flow, bitMapAll(this.paths.get(i)), this.paths.get(i))) {
				this.paths.clear();
//				System.out.println("ACCEPTED: "+flow);
				return true;
			}
		}
		
		getkShortestPaths().clear();
		return false;
	}
	
	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		for(int i = (spectrum.length-1); i >= 0 ; i--) {
			
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
					
					temp.clear();
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
    		
    		fi[i] =  (double)(nSlots - (double)pt.getLink(i).getSlotsAvailable()) / nSlots;
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
    			double cci = cc.getVertexScore(pt.getLink(index).getDestination()) + cc.getVertexScore(pt.getLink(index).getSource());
    			a += fi[index];
    			b += cci;
    		}
//    		a = 1 + Math.exp(a);
//    		b = 1 + Math.exp(b);
    		
    		double k1 = 50, k2 = 50;
//    		System.out.println("fi: "+a + " cc: "+ b);
    		a = 1 + Math.exp(k1 * (1 - a) );
    		a = 1 + Math.exp(k2 * (1 - b) );
    		
//    		sumRisc[i] = ( 1.0 / a ) + ( 1.0 / b ) ;
    		sumRisc[i] = ( 1.0 / a ) * ( 1.0 / b ) ;
//    		System.out.println("risc: "+sumRisc[i]);
    		indices.add(i);
    		i++;
    	}
    	
    	indices.sort((a,b) -> (int)(sumRisc[a]) - (int)(sumRisc[b]) );
    	
    	return indices;
    }
	

}

