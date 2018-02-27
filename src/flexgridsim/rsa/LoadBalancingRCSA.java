package flexgridsim.rsa;

import java.util.ArrayList;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;
//import flexgridsim.ModulationsMuticore;
//import flexgridsim.Slot;

public class LoadBalancingRCSA extends SCVCRCSA{
	
	protected boolean runRCSA(Flow flow) {
		
		kPaths = 3;
		setkShortestPaths(flow);
		ArrayList<Integer> indices = orderByClosenessCentralityAndFragmentationIndex(getkShortestPaths());

		for(Integer i: indices) {
			
			if(fitConnection(flow, bitMapAll(getkShortestPaths().get( i )), getkShortestPaths().get( i ))) 
			{
				getkShortestPaths().clear();
				return true;
			}
		}
		
		getkShortestPaths().clear();
		return false;
	}
	
	
//	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
//		
//		for(int i = 0; i < spectrum.length ; i++) {
//			
//			ArrayList<Slot> temp = new ArrayList<Slot>();
//			for(int j = 0; j < spectrum[i].length; j++) {	
//				
//				if(spectrum[i][j] == true) 
//				{
//					temp.add( new Slot(i,j) );
//				}
//				else {
//					
//					temp.clear();
//					if(Math.abs(spectrum[i].length-j) < demandInSlots) return new ArrayList<Slot>();
//				}
//				
//				if(temp.size() == demandInSlots) {
//					
//					if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[flow.getModulationLevel()])) {
//
//						return temp;
//					}
//					
//					temp.remove(0);
//				}
//			}
//		}
//  
//		return new ArrayList<Slot>();
//	}
//	


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
    		
    		for(int index : links) {
    			double cci = cc.getVertexScore(pt.getLink(index).getDestination()) + cc.getVertexScore(pt.getLink(index).getSource());
    			sumRisc[i] += (fi[index] + cci);
    		}
    		
    		indices.add(i);
    		i++;
    	}
    	
    	indices.sort((a,b) -> (int)(sumRisc[a] * 100) - (int)(sumRisc[b] * 100) );
    	
    	return indices;
    }
}

