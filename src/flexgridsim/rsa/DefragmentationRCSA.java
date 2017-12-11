package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.corba.se.impl.oa.poa.ActiveObjectMap.Key;
import com.sun.scenario.effect.light.Light;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.Slot;
import flexgridsim.util.JythonCaller;

import flexgridsim.util.JythonCaller;

/**
 * 
 * @author trindade
 *
 */
public class DefragmentationRCSA extends SCVCRCSA {

	private Map<Integer, ArrayList<Flow> > clusters;
	private static int k = 3;//number of clusters
	
	public void runDeFragmentation() {
		
		Map<Flow, LightPath> flows = cp.getMappedFlows();
		
		this.runKMeans(k, flows);
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		pt.resetAllSpectrum();

		//re-assigned resources in the same link, but using clustering
		for(Integer key: clusters.keySet()) {
			
			for(Flow flow: clusters.get(key)) {
				
				int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());
				
				spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
				
				for (int j = 0; j < (flow.getLinks().length - 1); j++) {
					
					bitMap(pt.getLink(flow.getLinks()[j], flow.getLinks()[j+1]).getSpectrum(), spectrum, spectrum);
				}
				
				ArrayList<Slot> slotList = fitConnection(spectrum, flow.getLinks(), demandInSlots, 0);
				
				if(establishConnection(flow.getLinks(), slotList, 0, flow)) {
					return;
				}
			}
		}
	}
	
	public ArrayList<Slot> fitConnection(boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		double xt = pt.getSumOfMeanCrosstalk(links);//returns the sum of cross-talk
		int []cores = this.getCoreOfCluster(demandInSlots);
		
		if(xt < 1) 
		{	
			for(int i = 0; i < cores.length; i++) {
				
				fittedSlotList = this.FirstFitPolicy(spectrum[i], i, links, demandInSlots);
				
				if(fittedSlotList.size() == demandInSlots) {
						break;
				}
				
				fittedSlotList.clear();
			}
		}
		
		return fittedSlotList;
	}
	
	private int[] getCoreOfCluster(int demandInSlots) {
		
		return null;
	}

	private double[][]getFeatures(Map<Flow, LightPath> flows) {
		
		double[][] features = new double[flows.size()][2];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Flow f: flows.keySet()) {
			
			features[i][0] = f.getDuration();
			features[i][1] = f.getRate();
			
			listOfFlows.add(f);
			i++;
		}
		
		return features;
	}

	private void runKMeans(int k, Map<Flow, LightPath> flows) {
		
		double[][] features = new double[flows.size()][2];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Flow f: flows.keySet()) {
			
			features[i][0] = f.getDuration();
			features[i][1] = f.getRate();
			
			listOfFlows.add(f);
			i++;
		}
		
		JythonCaller caller = new JythonCaller();
		String []labels = caller.kmeans(features, k);
		
		this.clusters = new HashMap<Integer, ArrayList<Flow> >();
		
		for(i = 0; i < k; i++) {
			
			clusters.put(i, null);
			
		}
		
		for(i = 0; i < labels.length; i++) {
			
			clusters.get(Integer.parseInt(labels[i])).add(listOfFlows.get(i));
		}
	}

}
