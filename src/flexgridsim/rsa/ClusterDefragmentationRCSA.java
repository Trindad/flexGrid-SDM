package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;
import flexgridsim.util.KMeansResult;
import flexgridsim.util.PythonCaller;

/**
 * De-fragmentation approach using clustering (k-Means)
 * @author trindade
 *
 */
public class ClusterDefragmentationRCSA extends DefragmentationRCSA {

	protected Map<Integer, ArrayList<Flow> > clusters;
	protected int cores[];
	protected int k = 5;//number of clusters
	private int nConnectionDisruption = 0;
	protected double time;
	private Map<Long, Flow>  activeFlows;
	
	protected int nextLimit(int index, int key) {
		return (key > 0 ? (index - cores[key-1] ) : index-1);
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	protected boolean lastChanceToAllocating(ArrayList<Flow> flows) {
	
		boolean[][] spectrum = new boolean[this.pt.getCores()][this.pt.getNumSlots()];
		
		flows.sort(Comparator.comparing(Flow::getRate));
		Collections.reverse(flows);
	 
		for(Flow flow: flows) {
				
			spectrum = bitMapAll(flow.getLinks());
			
			ArrayList<Integer> sortFreeCore = new ArrayList<Integer>();
			int  []coreSlots = new int[this.pt.getCores()];
	
			for(int i = 0; i < spectrum.length; i++) {
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
					activeFlows.put(flow.getID(), flow);
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
		
		return (this.nConnectionDisruption >= 1);
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
	
	private boolean isInCorrectCore(int core, int min, int max) {
		
		return (core >= min && core <= max);
	}
	
	@SuppressWarnings("unused")
	private void printAllLinksStatus() {

		int n = this.pt.getNumNodes();
		for(int u = 0; u < n; u++) {
			for(int v = 0; v < n; v++) {
				if(u == v) {
					continue;
				}
				else if(this.pt.hasLink(u, v)) {
					printSpectrum(this.pt.getLink(u, v).getSpectrum());
				}
			}
		}
	}
	
	
	@SuppressWarnings("unused")
	private boolean removeFlowsInCorrectCore(Map<Long, Flow> flows) {
		
		int index = this.pt.getCores();
		int next = index;
		int n = flows.size();
//		printAllLinksStatus();
		for(Integer key: clusters.keySet()) {
			
			if(!clusters.get(key).isEmpty()) 
			{
				index = nextLimit(index, key);
				next = (next - cores[key]);
				for(Flow flow: clusters.get(key)) {
					if(isInCorrectCore(flow.getCore(), next, index)) {
						updateData(flow, flow.getLinks(), flow.getSlotList(), flow.getModulationLevel());
						flows.remove(flow.getID());
						this.activeFlows.put(flow.getID(), flow);
						n--;
					}		
				}
			}
		}
		
		return n == 0;

	}
	
	
	private void updateXT() {
		for(int i = 0; i < this.pt.getNumLinks(); i++) {
			this.pt.getLink(i).updateCrosstalk();
		}
	}
	
	public void runDefragmentantion() {
		
		Map<Long, Flow> flows = cp.getActiveFlows();
		ArrayList<Flow> secondChance = new ArrayList<Flow>();
		int index = this.pt.getCores();
		int next = index;
		
		//data are not good to clustering in this moment
		if(!this.runKMeans(this.k, flows)) {
			return;
		}
		
		this.activeFlows = new HashMap<Long, Flow>(); 
		this.pt.resetAllSpectrum();
		
		

		if(removeFlowsInCorrectCore(flows)) {
			return;
		}
		
//		System.out.println("nFlows before: "+flows.size());

		//re-assigned resources in the same link, but using clustering
		for(Integer key: clusters.keySet()) {
			
			if(!clusters.get(key).isEmpty()) {
				
				index = nextLimit(index, key);
				next = (next - cores[key]);
//				System.out.println(index + " "+next);
				clusters.get(key).sort((a, b) -> b.getRate() - a.getRate());
				for(Flow flow: clusters.get(key)) {
					
					if(flows.containsKey(flow.getID())) {
	
						if(!fitConnection(flow, bitMapAllLimited(flow.getLinks(), index, next), flow.getLinks(), next, index )) {
							secondChance.add(flow);
						}
						else
						{
							activeFlows.put(flow.getID(), flow);
//							System.out.println(" "+flow);
						}
					}
				}
				
				updateXT();
			}
		}
		
		if(secondChance.size() >= 1)
		{
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
	
	private boolean[][] bitMapAllLimited(int[] links, int a, int b) {
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		boolean[][] spectrumOriginal = bitMapAll(links);
		
		for(int i = 0; i < spectrum.length; i++) {	
			for(int j = 0; j < spectrum[i].length; j++) {
				spectrum[i][j] = false;
			}
		}
		
		for(int i = a; i <= b; i++) {
			spectrum[i] = spectrumOriginal[i];
		}
		
		return spectrum;
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
			
			int n = Math.abs( (int)Math.ceil( ( (double)totalSlots[i] / ( (double)pt.getCores() * (double)pt.getNumSlots()) ) ) - diff)/100;
			this.cores[i] = n <= 0? 1 : n;
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

	
	protected boolean updateLightpath(Flow flow) {
		
		
		if (flow.getLightpathID() >= 0) 
		{
			int []links = flow.getLinks();
			
			for (int l: links) {
				
	            this.pt.getLink(l).reserveSlots(flow.getSlotList());
	            this.pt.getLink(l).updateNoise(flow.getSlotList(), flow.getModulationLevel());
	            this.pt.getLink(l).updateCrosstalk();
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
	protected boolean updateData(Flow flow, int []links, ArrayList<Slot> fittedSlotList, int modulation) {
		
		flow.setSlotList(fittedSlotList);
		flow.setModulationLevel(modulation);
		
		if(!updateLightpath(flow)) {
			System.out.println("Error while creating a new lightpath");
			return false;
		}
		
		return true;
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

		fittedSlotList = FirstFitPolicy(flow, spectrum, links, flow.getSlotListSize(), flow.getModulationLevel());
			
		if(fittedSlotList.size() == flow.getSlotListSize()) 
		{
			return updateData(flow, links, fittedSlotList, flow.getModulationLevel());
		}
		
		return false;
	}
	
	private ArrayList<Slot> getMatchSlots(ArrayList<Slot> s1, ArrayList<Slot> s2) {
		
		ArrayList<Slot> slots = new ArrayList<Slot>();
		if(s1.get(0).c != s2.get(0).c) return slots;
		
		for(Slot i: s1) 
		{
			for(Slot j: s2) 
			{
				if(i.s == j.s && !slots.contains(i)) {
					slots.add(i);
				}
			}
		}
		
		return slots;
	}
	
	public boolean CrosstalkIsAcceptable(Flow flow, int[] links, ArrayList<Slot> slotList, double db) {
		
		if(!pt.canAcceptCrosstalk(links, slotList, db)) {
			return false;
		}
		
		for(Long key: this.activeFlows.keySet()) {
			
			if(key == flow.getID()) {
				continue;
			}
			else if(this.activeFlows.get(key).isMultipath()) 
			{
				int i = 0;
				for(ArrayList<Slot> s: this.activeFlows.get(key).getMultiSlotList()) {
					
					ArrayList<Slot> t = getMatchSlots(slotList, s);
					if(!t.isEmpty()) 
					{
						if(!pt.canAcceptInterCrosstalk(this.activeFlows.get(key),this.activeFlows.get(key).getLinks(i), s, t)) return false;
					}
					
					i++;
				}
			}
			else 
			{
//				System.out.println(Arrays.toString(slotList.toArray()) + "\n" +this.activeFlows.get(key).getSlotList());
				ArrayList<Slot> t = getMatchSlots(slotList, this.activeFlows.get(key).getSlotList());
				if(!t.isEmpty()) 
				{
					if(!pt.canAcceptInterCrosstalk(this.activeFlows.get(key), this.activeFlows.get(key).getSlotList(), t)) return false;
				}
			}
		}
		
		return true;
	}
	
	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<ArrayList<Slot>> setOfSlots = new ArrayList<ArrayList<Slot>> ();
		
		for(int i = 0; i < spectrum.length ; i++) {
			
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
					
					if(CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {
						setOfSlots.add(new ArrayList<Slot>(temp));
						break;
					}
					
					temp.remove(0);
				}
			}
		}
		
		
		if(!setOfSlots.isEmpty()) {
			
			setOfSlots.sort( (a , b) -> {
				int diff = a.get(0).s - b.get(0).s;
				
				if(diff != 0) {
					return diff;
				}
				
				return ( b.get(0).c - a.get(0).c );
			});
			
			return setOfSlots.get(0);		
		}
	    
		return new ArrayList<Slot>();
	}

	protected boolean runKMeans(int k, Map<Long, Flow> flows) {
		
		double[][] features = new double[flows.size()][2];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Long f: flows.keySet()) {
			
			features[i][1] = (flows.get(f).getLinks().length * 100);
			features[i][0] = flows.get(f).getRate();
			
			listOfFlows.add(flows.get(f));
			i++;
		}
		
		PythonCaller caller = new PythonCaller();
		KMeansResult result = caller.kmeans(features, k);
		
		System.out.println(result.getSilhouette());
		if(result.getSilhouette() < 0.7) {
			
			cp.setClusters(new ArrayList<Cluster>());
			return false;
		}
		
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
		
		return true;
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
