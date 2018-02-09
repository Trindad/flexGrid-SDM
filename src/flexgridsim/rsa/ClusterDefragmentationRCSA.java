package flexgridsim.rsa;

import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VirtualTopology;
import flexgridsim.util.KMeansResult;
import flexgridsim.util.PythonCaller;

/**
 * De-fragmentation approach using clustering (k-Means)
 * 
 * @author trindade
 *
 */
public class ClusterDefragmentationRCSA extends DefragmentationRCSA {

	protected Map<Integer, ArrayList<Flow> > clusters;
	protected int cores[];
	protected int k = 4;//number of clusters
	private int nConnectionDisruption = 0;
	protected double time;
	
	protected int nextLimit(int index, int key) {
		return (key > 0 ? (index - cores[key-1] ) : index-1);
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	protected boolean lastChanceToAllocating(ArrayList<Flow> flows) {
	
		boolean[][] spectrum = new boolean[this.pt.getCores()][this.pt.getNumSlots()];
		
//		flows.sort(Comparator.comparing(Flow::getRate));
//		Collections.reverse(flows);
//		 
		for(Flow flow: flows) {
				
			spectrum = initMatrix(spectrum, this.pt.getCores(),this.pt.getNumSlots());
			
			for(int i = 0; i < flow.getLinks().length; i++) {
				
				int src  = this.pt.getLink(flow.getLink(i)).getSource();
				int dst = this.pt.getLink(flow.getLink(i)).getDestination();
				
				bitMap(this.pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
			}
			
			ArrayList<Integer> sortFreeCore = new ArrayList<Integer>();
			int  []coreSlots = new int[this.pt.getCores()];
	
			for(int i = 0; i < this.pt.getCores(); i++) {
				sortFreeCore.add(i);
				int n = 0;
				
				for(int j = 0; j < spectrum[i].length; j++) {
					if(spectrum[i][j]) n++;
				}
				
				coreSlots[i] = n;
			}
			
			sortFreeCore.sort((a,b) -> coreSlots[a] - coreSlots[b]);

			for(int i = 0; i < this.pt.getCores(); i++) {

				if(fitConnection(flow, spectrum, flow.getLinks(), sortFreeCore.get(i), sortFreeCore.get(i) ) == true) 
				{
					flow.setAccepeted(true);
					break;
				}
			}
			
			if(!flow.isAccepeted()) 
			{
				flow.setConnectionDisruption(true);
				this.nConnectionDisruption++;	
			}
		}
		
		return this.nConnectionDisruption >= 1;
	}
	
	protected void releaseResourcesAssigned() {
		
		Map<Long, Flow> flows = new HashMap<Long, Flow>(cp.getActiveFlows());
		
		for(Long key: flows.keySet()) {
			
			cp.removeFlowFromPT(flows.get(key), this.vt.getLightpath(flows.get(key).getLightpathID()), this.pt, this.vt);	
			flows.get(key).getSlotList().clear();
		}
		
		this.nConnectionDisruption = 0;
	}
	
	@SuppressWarnings("unused")
	private void updateControlPlane(Map<Long, Flow> flows) {
		
		cp.updateControlPlane(this.pt, this.vt, flows);
		this.nConnectionDisruption = 0;
	}
	
	private boolean isInCorrectCore(int core, int a, int b) {
		
		for (; b >= a && b >= 0; b--) {
			
			if(b == core) return true;
		}
		
		return false;
	}
	
	@SuppressWarnings("unused")
	private void removeFlowsInCorrectCore(Map<Long, Flow> flows) {
		
		int index = this.pt.getCores();
		int next = index;
		
		for(Integer key: clusters.keySet()) {
			
			if(!clusters.get(key).isEmpty()) {
				
				index = nextLimit(index, key);
				next = (next - cores[key]);
				for(Flow flow: clusters.get(key)) {
					if(isInCorrectCore(flow.getCore(), next, index)) {
						updateData(flow, flow.getLinks(), flow.getSlotList(), flow.getModulationLevel());
						flows.remove(flow.getID());
					}
					
				}
			}
		}
	}
	
	public void runDefragmentantion() {
		
		Map<Long, Flow> flows = cp.getActiveFlows();
		ArrayList<Flow> secondChance = new ArrayList<Flow>();
		
		
		
		boolean[][] spectrum = new boolean[this.pt.getCores()][this.pt.getNumSlots()];
		int index = this.pt.getCores();
		int next = index;
		
		this.pt.resetAllSpectrum();
		this.runKMeans(this.k, flows);
		
		//re-assigned resources in the same link, but using clustering
		for(Integer key: clusters.keySet()) {
			
			if(!clusters.get(key).isEmpty()) {
				
				index = nextLimit(index, key);
				next = (next - cores[key]);
				
				for(Flow flow: clusters.get(key)) {

					spectrum = initMatrix(spectrum, this.pt.getCores(),this.pt.getNumSlots());
					
					for(int i = 0; i < flow.getLinks().length; i++) {
						
						int src  = this.pt.getLink(flow.getLink(i)).getSource();
						int dst = this.pt.getLink(flow.getLink(i)).getDestination();
						
						bitMap(this.pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
					}
					
					
					if(fitConnection(flow, spectrum, flow.getLinks(), next, index ) == false) {
						secondChance.add(flow);
					}
					
				}
			}
		}
		
		if(secondChance.size() >= 1)
		{
//			System.out.println(" "+flows.size()+" "+secondChance.size());
			if(lastChanceToAllocating(secondChance)) 
			{
				System.out.println("It's impossible to reallocate: "+this.nConnectionDisruption);
				this.nConnectionDisruption = 0;
			}
			
			secondChance.clear();
		}
		
		
		BestEffortTrafficMigration bf = new BestEffortTrafficMigration(cp, this.pt, this.vt, flows, cp.getActiveFlows());
		
		try {
			
			bf.runBestEffort();
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		
		clusters.clear();
	}
	
	protected void distributeCores() {
		
		this.cores = new int[this.clusters.size()];
		
		int []totalSlots = new int[this.clusters.size()];
		int []nLinks = new int[this.clusters.size()];
		int avgSlots = 0;
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		
		for(Integer k: clusters.keySet()) {
			
			int rate = 0;
			int links = 0;
			
			for(int i = 0; i < clusters.get(k).size(); i++) {
				rate += (int)(clusters.get(k).get(i).getRate());
				links += (clusters.get(k).get(i).getLinks().length);
			}
			
			totalSlots[k] = (int) Math.ceil( (double) rate / pt.getSlotCapacity() );
			avgSlots += rate;
			nLinks[k] = (int) Math.ceil((double)links/(double)clusters.get(k).size());
			indexes.add(k);
		}
		
		avgSlots =  (int) Math.ceil( (double)avgSlots / (double)clusters.size());
		indexes.sort((a,b) -> totalSlots[b] - totalSlots[a]);
		int nCores = pt.getCores();

		int diff = (int)Math.ceil( ( (double)avgSlots / ( (double)pt.getCores() * (double)pt.getNumSlots()) ) );
		
		for(int i = 0; i < cores.length; i++) {
			
			this.cores[i] = Math.abs( (int)Math.ceil( ( (double)totalSlots[i] / ( (double)pt.getCores() * (double)pt.getNumSlots()) ) ) - diff)/100;
//			System.out.println("cluster-"+i+": "+this.cores[i]+" rate: "+ totalSlots[i]);
			nCores -= this.cores[i];
		}
		
		if(nCores >= 1)
		{	
			int n = (int) Math.ceil( (double)nCores/ (double)cores.length);
			
			for(int i = 0; i < cores.length && nCores >= 1; i++) {
				
				this.cores[indexes.get(i)] = this.cores[indexes.get(i)] + n;
				nCores -= n;
				
				if(nCores < n) n = nCores;
			}
		}
	}
	
	protected boolean createNewLightpath(Flow flow) {
		
		long id = this.vt.createLightpath(flow.getLinks(), flow.getSlotList() ,flow.getModulationLevel());
		
		if (id >= 0) 
		{
			LightPath lps = this.vt.getLightpath(id);
			flow.setLightpathID(id);
			
			int []links = flow.getLinks();
			
			for (int j = 0; j < links.length; j++) {
				
	            this.pt.getLink(links[j]).reserveSlots(flow.getSlotList());
	            this.pt.getLink(links[j]).updateNoise(lps.getSlotList(), flow.getModulationLevel());
	        }
			
			//update cross-talk
			for(int i = 0; i < links.length; i++) {
				this.pt.getLink(links[i]).updateCrosstalk();
			}
			
			return true;
		}
		
		return false;
	}
	
	protected boolean updateLightpath(Flow flow) {
		
		
		if (flow.getLightpathID() >= 0) 
		{
			LightPath lps = this.vt.getLightpath(flow.getLightpathID());
			
			int []links = flow.getLinks();
			
			for (int j = 0; j < links.length; j++) {
				
	            this.pt.getLink(links[j]).reserveSlots(flow.getSlotList());
	            this.pt.getLink(links[j]).updateNoise(lps.getSlotList(), flow.getModulationLevel());
	            this.pt.getLink(links[j]).updateCrosstalk();
	        }
			
			return true;
		}
		
		return false;
	}

	/**
	 * 
	 * @param flow
	 * @param links
	 * @param fittedSlotList
	 * @param modulation
	 */
	protected void updateData(Flow flow, int []links, ArrayList<Slot> fittedSlotList, int modulation) {
		
		flow.setSlotList(fittedSlotList);
		flow.setModulationLevel(modulation);
		
		if(!updateLightpath(flow)) {
			System.out.println("Error while creating a new lightpath");
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
				
				xt = this.pt.getSumOfMeanCrosstalk(links, i);//returns the sum of cross-talk	
				
				if(xt == 0 || (xt < ModulationsMuticore.inBandXT[modulation]) ) {
	
					fittedSlotList = this.FirstFitPolicy(spectrum[i], i, links, demandInSlots);
					
					if(fittedSlotList.size() == demandInSlots) {
						
						if(fittedSlotList.size() == demandInSlots) {
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
			
			features[i][1] = flows.get(f).getDuration() - (time - flows.get(f).getTime());
			features[i][0] = flows.get(f).getRate();
			
			listOfFlows.add(flows.get(f));
			i++;
		}
		
		PythonCaller caller = new PythonCaller();
		KMeansResult result = caller.kmeans(features, k);
		String []labels = result.getLabels();
		double [][]centroids = result.getCentroids();
		
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
		
		this.distributeCores();
		ArrayList<Cluster> clustersStructure = new ArrayList<Cluster>();
		
		int index = this.pt.getCores();
		int next = index;

		for(int i = 0; i < centroids.length; i++) {
			
			Cluster c = new Cluster((int)centroids[i][0], (int)centroids[i][1]);
			
			int []temp = new int[cores[i]];
			index = nextLimit(index, i);
			next = (next - cores[i]);
			int k = 0;
			for (int j = index; j >= next && j >= 0; j--) {

				temp[k] = j;
				k++;
			}
			c.setCores(temp);
			clustersStructure.add(c);
		}
		
		cp.setClusters(clustersStructure);
	}
}
