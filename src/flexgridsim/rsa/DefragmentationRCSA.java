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
public class DefragmentationRCSA extends SCVCRCSA{

	Map<Long, ArrayList<Flow>> clusters;
	
	public void runDeFragmentation() {
		
		ArrayList<Flow> flows = this.filteringRequest();
		int k = 3;
		
		this.runKMeans(this.getFeatures(flows), k);
		
		this.getClusters();
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		pt.resetAllSpectrum();

		//re-assigned resources in the same link, but using clustering
		for(Long key: clusters.keySet()) {
			
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

	private void getClusters() {
		this.clusters = new HashMap<Long, ArrayList<Flow>>();
		
	}

	private ArrayList<Flow> filteringRequest() {
		
		ArrayList<Flow> flows = new ArrayList<Flow>();
		
		return flows;
	}
	
	private double[][]getFeatures(ArrayList<Flow> flows) {
		
		double[][] features = new double[flows.size()][2];
		int i = 0;
		
		for(Flow f: flows) {
			
			features[i][0] = f.getDuration();
			features[i][1] = f.getRate();
			
			i++;
		}
		
		return features;
	}

	private void runKMeans(double [][]features, int k) {
		
		JythonCaller caller = new JythonCaller();
		caller.kmeans(features, k);
	}

}
