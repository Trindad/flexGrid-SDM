package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

/**
 * RCSA algorithm using load balancing based on a set of fairness axioms
 * 
 * Axiom of Saturation: Fairness value of equal allocation
 * and equal weights is independent of number of users as
 * the number of users becomes large
 * 
 * using numerical examples, from typical networking functionalities: congestion 
 * control, routing, power control, and spectrum management
 * 
 * @author trindade
 *
 */

public class LBJainFairnessRCSA extends XTFFRCSA{

	protected boolean runRCSA(Flow flow) {
		
		kPaths = 4;
		setkShortestPaths(flow);
	
		ArrayList<boolean [][]> spectrum = new ArrayList<boolean[][]>();
		for(int i = 0; i < kPaths; i++) {
			
			spectrum.add(bitMapAll(paths.get(i)));
		}
		
		ArrayList<Integer> indices = new ArrayList<Integer>();
//		indices.add(0); indices.add(1); indices.add(2); indices.add(3); 
//		indices.add(4);
		indices = fitness(paths, spectrum);
//		System.out.println(indices.size());
//		System.out.println(indices);
		for(int i : indices) {
			int []links = this.paths.get(i);
			if(fitConnection(flow, bitMapAll(links), links)) {
				this.paths.clear();
				return true;
			}
		}
		
		getkShortestPaths().clear();
		return false;
	}
	
	
	
	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6,5,4,3,2,1,0));

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

	
	
	protected int getDemandInSlots(int t) {
		
		double requestedBandwidthInGHz = ( (double)t / (double)decreaseModulation(5, t) );
		double requiredBandwidthInGHz = requestedBandwidthInGHz;
		double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
		int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
		
		demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
		demandInSlots++;
		
		return demandInSlots;
	}
	
	private int maximumModulationFormat(int []links) {
		int totalLength = 0;
		
		for(int i : links) {
			
			totalLength += (pt.getLink(i).getDistance()); 
		}
		
		//invalid path
		if(totalLength > ModulationsMuticore.maxDistance[0]) {
			return -1;
		}
		
		int modulationLevel =  ModulationsMuticore.getModulationByDistance(totalLength);
		
		return modulationLevel;
	}
	
	public ArrayList<Integer> fitness(ArrayList<int[]> p, ArrayList<boolean [][]> spectrum ) {
    
    	double []fitness = new double[p.size()];
    	ArrayList<Integer> indices = new ArrayList<Integer>();
    	
 
    	for(int i = 0; i < p.size(); i++) {
    		
    		fitness[i] += ((double)maximumModulationFormat(p.get(i)) + 1)/ModulationsMuticore.numberOfModulations();
    		fitness[i] += saturationAxiom(p.get(i), spectrum.get(i));
//    		fitness[i] += minBandwidth(p.get(i), spectrum.get(i));
    		
//    		double totalLength = 0;
//    		for(int link : p.get(i)) {
//    			
//    			totalLength += (pt.getLink(link).getDistance()); 
//    		}
//    		System.out.println(fitness[i]);
//    		fitness[i] += totalLength;
    		fitness[i] += ( 1.0 - ((double)p.get(i).length/(double)pt.getNumLinks()));
//    		System.out.println(fitness[i]);
    		indices.add(i);
    	}
    	
    	indices.sort((a,b) -> {
//    		System.out.println(fitness[a]+" - "+fitness[b]);
    		return (int)( (fitness[b] - fitness[a]) * 100.0f);
    		
    	});
//    	System.out.println(indices);
    	return indices;
    }
	
	
	
	private double saturationAxiom(int[] path, boolean [][]spectrum) {
		
		double saturation = 0;
        
		double availableSlots = 0;
		
	    for(int i = 0; i < spectrum.length; i++) {
	    	for(int j = 0; j < spectrum[i].length; j++) {
	    		
	    		availableSlots += spectrum[i][j] == true ? 0 : 1;
		    }
	    }
	    
	    saturation = availableSlots / (pt.getCores() * pt.getNumSlots());
//	    System.out.println(saturation);
	    return saturation;
	}
	
	private int minBandwidth(int[] path, boolean [][]spectrum) {
		int min = pt.getNumSlots();
		int temp = 0;
		
	    for(int i = 0; i < spectrum.length; i++) {
	    	for(int j = 0; j < spectrum[i].length; j++) {
	    		
	    		temp += spectrum[i][j] == true ? 0 : 1;
	    		
	    		if(temp < min) {
	    			min = temp;
	    		}
		    }
	    	
	    	temp = 0;
	    }
	    
	    return min;
	}
	
	
	
	
	/**
	 * sBest ← s0
//		bestCandidate ← s0
//		tabuList ← []
//		tabuList.push(s0)
//		while (not stoppingCondition())
//			sNeighborhood ← getNeighbors(bestCandidate)
//			for (sCandidate in sNeighborhood)
//				if ( (not tabuList.contains(sCandidate)) and (fitness(sCandidate) > fitness(bestCandidate)) )
//					bestCandidate ← sCandidate
//				end
//			end
//			if (fitness(bestCandidate) > fitness(sBest))
//				sBest ← bestCandidate
//			end
//			tabuList.push(bestCandidate)
//			if (tabuList.size > maxTabuSize)
//				tabuList.removeFirst()
//			end
//		end
//		return sBest
	 */
	private boolean tabuSearch() {
		
		return false;
	}
}
