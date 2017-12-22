package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;
import flexgridsim.util.PythonCaller;

/**
 * Defragmentation approach using clustering (k-Means)
 * 
 * @author trindade
 *
 */
public class ClusterDefragmentationRCSA extends DefragmentationRCSA {

	protected Map<Integer, ArrayList<Flow> > clusters;
	protected int k = 4;//number of clusters
	protected int it = 2;
	
	public void runDefragmentantion() {
		
		Map<Long, Flow> flows = cp.getActiveFlows();
		pt.resetAllSpectrum();
		System.out.println("Number of Flow:"+flows.size());
		this.runKMeans(k, flows);
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		int index = 1;

		//re-assigned resources in the same link, but using clustering
		for(Integer key: clusters.keySet()) {
			
			if(clusters.get(key).size() >= 1) {
			
				clusters.get(key).sort(Comparator.comparing(Flow::getDuration));
				
				for(int c = (clusters.get(key).size()-1); c >= 0; c--) {
					
					Flow flow = clusters.get(key).get(c);
					spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
					
					for(int i = 0; i < flow.getLinks().length; i++) {
						int src  = pt.getLink(flow.getLink(i)).getSource();
						int dst = pt.getLink(flow.getLink(i)).getDestination();
						
						bitMap(pt.getLink(pt.getLink(src, dst).getDestination()).getSpectrum(), spectrum, spectrum);
					}
					
					if(fitConnection(flow, spectrum, flow.getLinks(), index) == false) System.out.println("Something bad happened");
				}
				
				
				System.out.println("*************************");
			}
			
			index+=it;
		}
	}
	
	protected void updateData(Flow flow, int []links, ArrayList<Slot> fittedSlotList, int modulation) {
		
		flow.setLinks(links);
		flow.setSlotList(fittedSlotList);
		flow.setModulationLevel(modulation);
		
		LightPath lps = vt.getLightpath(flow.getLightpathID());
		cp.reacceptFlow(flow.getID(), lps);
		
		for (int j = 0; j < links.length; j++) {
			
            pt.getLink(links[j]).reserveSlots(fittedSlotList);
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
				
		for (; i < spectrum.length; i+=it) {
			
			int modulation = chooseModulationFormat(flow, links);
			
			while(modulation >= 0)
			{
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
				
				modulation--;
			}
		}
		
		return false;
	}

	protected void runKMeans(int k, Map<Long, Flow> flows) {
		
		double[][] features = new double[flows.size()][2];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Long f: flows.keySet()) {
			
			features[i][0] = flows.get(f).getDuration();
			features[i][1] = flows.get(f).getRate();
			
			listOfFlows.add(flows.get(f));
			i++;
		}
		
		PythonCaller caller = new PythonCaller();
		String []labels = caller.kmeans(features, k);
		double[][] centroids = caller.getCentroids();//two dimension
		
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

		this.createClusters(centroids);
		
	}
	
	/**
	 * 
	 * @param centroids
	 */
	protected void createClusters(double [][]centroids) {
		
		ArrayList<Cluster> clustersStructure = new ArrayList<Cluster>(centroids.length);
		
		for(int i = 0; i < clustersStructure.size(); i++) {
			
			Cluster c = new Cluster(1, (int)centroids[i][0], centroids[i][1]);
			clustersStructure.add(c);
		}
		
		cp.setClusters(clustersStructure);
		
	}
}
