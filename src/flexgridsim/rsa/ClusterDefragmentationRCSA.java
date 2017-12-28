package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;
import flexgridsim.util.KMeansResult;
import flexgridsim.util.PythonCaller;

/**
 * Defragmentation approach using clustering (k-Means)
 * 
 * @author trindade
 *
 */
public class ClusterDefragmentationRCSA extends DefragmentationRCSA {

	protected Map<Integer, ArrayList<Flow> > clusters;
	protected int cores[];
	protected int k = 7;//number of clusters
	
	protected int nextLimit(int index, int key) {
		return (key > 0 ? (index - cores[key-1] ) : index-1);
	}
	
	public boolean fitConnectionDynamicModulation(Flow flow, boolean [][]spectrum, int[] links, int n , int i) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		double xt = 0.0f;
				
		for (; i >= n && i >= 0; i--) {

			int modulation = chooseModulationFormat(flow, links);

			while(modulation >= 0) {
				
				double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
				int demandInSlots = (int) Math.ceil((double)flow.getRate() / subcarrierCapacity);
				
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
				
				modulation --;
			}
			
			
		}
		
		
		return false;
	}
	
	protected void lastChanceToAllocating(ArrayList<Flow> flows) {
	
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		
		flows.sort(Comparator.comparing(Flow::getRate));
//		Collections.reverse(flows);
		 
		for(Flow flow: flows) {
				
			spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
			
			for(int i = 0; i < flow.getLinks().length; i++) {
				
				int src  = pt.getLink(flow.getLink(i)).getSource();
				int dst = pt.getLink(flow.getLink(i)).getDestination();
				
				bitMap(pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
			}
			
			ArrayList<Integer> sortFreeCore = new ArrayList<>();
			int  []coreSlots = new int[pt.getCores()];
	
			for(int i = 0; i < pt.getCores(); i++) {
				sortFreeCore.add(i);
				int n = 0;
				
				for(int j = 0; j < spectrum[i].length; j++) {
					if(spectrum[i][j]) n++;
				}
				
				coreSlots[i] = n;
				
			}
			
			sortFreeCore.sort((a,b) -> coreSlots[a] - coreSlots[b]);
			
			
//			for(int i = 0; i < spectrum.length; i++) System.out.println(sortFreeCore.get(i));
			for(int i = 0; i < pt.getCores(); i++) {

				if( fitConnection(flow, spectrum, flow.getLinks(), sortFreeCore.get(i), sortFreeCore.get(i) )) return;
			}
			
			System.out.println("Flow "+flow+" modulation: "+flow.getModulationLevel());
			this.printSpectrum(spectrum);
		}
	}
	
	public void runDefragmentantion() {
		
		Map<Long, Flow> flows = cp.getActiveFlows();
		ArrayList<Flow> secondChance = new ArrayList<Flow>();
		pt.resetAllSpectrum();
		System.out.println("Number of Flow:"+flows.size());
		this.runKMeans(this.k, flows);
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		int index = pt.getCores();
		
		this.distributeCores();
		int next = index;

		//re-assigned resources in the same link, but using clustering
		for(Integer key: clusters.keySet()) {
			
			if(!clusters.get(key).isEmpty()) {
				
				index = nextLimit(index, key);
				next = (next - cores[key]);
				
				System.out.println("key:  "+key +" next: "+next+" Index: "+index+" n: "+clusters.get(key).size()+" "+cores[key]);				
//				clusters.get(key).sort( Comparator.comparing(Flow::getDuration) );
//				Collections.reverse(clusters.get(key));
				
				for(Flow flow: clusters.get(key)) {

					spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
					
					for(int i = 0; i < flow.getLinks().length; i++) {
						
						int src  = pt.getLink(flow.getLink(i)).getSource();
						int dst = pt.getLink(flow.getLink(i)).getDestination();
						
						bitMap(pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
					}
					
					
					if(fitConnection(flow, spectrum, flow.getLinks(), next, index ) == false) {
						secondChance.add(flow);
					}
					
				}
				
				System.out.println("*************************");
			}
		}
		
		if(secondChance.size() >= 1)
		{
			System.out.println("Something bad happened "+secondChance.size());
			lastChanceToAllocating(secondChance);
			secondChance.clear();
		}
		
		clusters.clear();
	}
	
	private void distributeCores() {
		
		this.cores = new int[this.clusters.size()];
		
		int []nRequest = new int[this.clusters.size()];

		for(Integer key: clusters.keySet()) {
			
			int slots = 0;
			
			for(int i = 0; i < clusters.get(key).size(); i++) {
				slots += (int)(clusters.get(key).get(i).getSlotListSize() * 2);
			}
			
			nRequest[key] = slots;
		}
		
		int nCores = pt.getCores();

		for(int i = 0; i < cores.length; i++) {
			
			this.cores[i] = (int)Math.ceil( ( (double)nRequest[i] / ( (double)pt.getCores()* (double)pt.getNumSlots() )) /(double)pt.getNumLinks() );
			nCores -= this.cores[i];
//			System.out.println("nCores*:" +cores[i]+" - "+nRequest[i]);
		}
		
		if(nCores >= 1)
		{
			
			int n = (int) Math.ceil( (double)nCores/cores.length);
			
			for(int i = 0; i < cores.length && nCores >= 1; i++) {
				
				this.cores[i] = this.cores[i] + n;
				nCores -= n;
				
				if(nCores < n) n = nCores;
			}
		}
		for(int i = 0; i < cores.length; i ++ ) System.out.println("nCores:" +cores[i]);
		
	}

	/**
	 * 
	 * @param flow
	 * @param links
	 * @param fittedSlotList
	 * @param modulation
	 */
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
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links, int n , int i) {
		
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		double xt = 0.0f;
				
		for (; i >= n && i >= 0; i--) {

			int modulation = chooseModulationFormat(flow, links);

			while(modulation >= 0) {
				
				double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
				int demandInSlots = (int) Math.ceil((double)flow.getRate() / subcarrierCapacity);
				
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
				modulation --;
			}
		}
		
		
		return false;
	}

	protected void runKMeans(int k, Map<Long, Flow> flows) {
		
		double[][] features = new double[flows.size()][2];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Long f: flows.keySet()) {
			
			features[i][0] = flows.get(f).getLinks().length;
			features[i][1] = flows.get(f).getRate();
			
			listOfFlows.add(flows.get(f));
			i++;
		}
		
		PythonCaller caller = new PythonCaller();
		KMeansResult result = caller.kmeans(features, k);
		String []labels = result.getLabels();
		double [][]centroids = result.getCentroids();
		
//		System.out.println("------");
//		for( i = 0; i < labels.length; i++) System.out.println(labels[i]);
//		System.out.println("------");
		
		this.clusters = new HashMap<Integer, ArrayList<Flow> >();
		
		for(i = 0; i < this.k; i++) {
			
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
