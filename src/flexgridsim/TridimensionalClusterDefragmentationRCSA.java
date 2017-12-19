package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.rsa.ClusterDefragmentationRCSA;
import flexgridsim.util.PythonCaller;

/**
 * Tridimensional Cluster Defragmentation RCSA
 * @author trindade
 *
 */
public class TridimensionalClusterDefragmentationRCSA extends ClusterDefragmentationRCSA {
	
	protected void runKMeans(int k , Map<Long, Flow> flows) {
		
		double[][] features = new double[flows.size()][3];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Long f: flows.keySet()) {
			
			features[i][0] = flows.get(f).getDuration();
			features[i][1] = flows.get(f).getRate();
			features[i][2] = flows.get(f).getModulationLevel();
			
			listOfFlows.add(flows.get(f));
			i++;
		}
		
		PythonCaller caller = new PythonCaller();
		String []labels = caller.kmeans(features, k);
		
//		System.out.println("------");
//		for( i = 0; i < labels.length; i++) System.out.println(labels[i]);
//		System.out.println("------");
		
		this.clusters = new HashMap<Integer, ArrayList<Flow> >();
		
		for(i = 0; i < k; i++) {
			
			clusters.put(i, new ArrayList<Flow>());
			
		}
		
		for(i = 0; i < labels.length; i++) {
			clusters.get(Integer.parseInt(labels[i])).add(listOfFlows.get(i));
		}
		
	}
	
	
	

	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @return
	 */
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links, int i) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		double xt = 0.0f;
				
		for (; i < spectrum.length; i+=2) {
			
			int modulation = flow.getModulationLevel();
			double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
			int demandInSlots = (int) Math.ceil(flow.getRate() / subcarrierCapacity);
			
			xt = pt.getSumOfMeanCrosstalk(links, i);//returns the sum of cross-talk	
			
			if(xt == 0 || (xt < ModulationsMuticore.inBandXT[modulation]) ) {

				fittedSlotList = this.FirstFitPolicy(spectrum[i], i, links, demandInSlots);
				
				if(fittedSlotList.size() == demandInSlots) {
					
					if(fittedSlotList.size() == demandInSlots) {
						System.out.println(" Re-accepted "+flow+ " core: "+i+" "+fittedSlotList);
						
						this.updateData(flow, links, fittedSlotList, modulation);
						return true;
					}
				}
				
				fittedSlotList.clear();
			}
			
		}
		
		return false;
	}

}
