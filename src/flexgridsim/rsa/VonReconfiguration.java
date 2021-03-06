package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VonControlPlane;
import flexgridsim.util.KMeansResult;
import flexgridsim.util.PythonCaller;
import vne.VirtualNetworkEmbedding;

/**
 * De-fragmentation approach using clustering (k-Means)
 * @author trindade
 *
 */
public class VonReconfiguration extends DefragmentationRCSA {

	protected Map<Integer, ArrayList<Flow> > clusters;
	protected int cores[];
	protected int k = 3;//number of clusters
	private int nConnectionDisruption = 0;
	protected double time;
	private Map<Long, Flow>  activeFlows;
	
	private VonControlPlane cp;
	@SuppressWarnings("unused")
	private VirtualNetworkEmbedding vne;
	
	protected int nextLimit(int index, int key) {
		return (key > 0 ? (index - cores[key-1] ) : index-1);
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	protected boolean lastChanceToAllocating(ArrayList<Flow> flows) {
		this.nConnectionDisruption = 0;
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
			flow.setAccepeted(false);
			ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
			for(Integer i: priorityCores) {

				if(fitConnection(flow, spectrum, flow.getLinks() )) 
				{
					this.activeFlows.put(flow.getID(), flow);
					
					flow.setAccepeted(true);
					flow.setCore(sortFreeCore.get(i));
					break;
				}
			}
			
			
			if(!this.activeFlows.containsKey(flow)) this.activeFlows.put(flow.getID(), flow);
			
		}
		
		
		return (this.nConnectionDisruption >= 1);
	}
	
	protected void releaseResourcesAssigned() {
		
		Map<Long, Flow> flows = new HashMap<Long, Flow>(cp.getActiveFlows());
		
		for(Long key: flows.keySet()) {
			
			cp.removeFlowFromPT(flows.get(key), this.pt);	
			flows.get(key).getSlotList().clear();
		}
		
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

		for(Integer key: clusters.keySet()) {
			
			if(!clusters.get(key).isEmpty()) 
			{
				clusters.get(key).sort((a, b) -> b.getRate() - a.getRate());
				index = nextLimit(index, key);
				next = (next - cores[key]);
				for(Flow flow: clusters.get(key)) {
					if(isInCorrectCore(flow.getCore(), next, index)) {
						
						int modulation = chooseModulationFormat(flow.getRate(), flow.getLinks());
						
						if(modulation == flow.getModulationLevel()) 
						{
							updateData(flow, flow.getLinks(), flow.getSlotList(), flow.getModulationLevel());
						}
						else 
						{
							ArrayList<Slot> temp = new ArrayList<Slot>();

							double requestedBandwidthInGHz = ( ((double)flow.getRate()) / ((double)modulation + 1) );
							double requiredBandwidthInGHz = requestedBandwidthInGHz;
							double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
							int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
							
							demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
							demandInSlots++;//adding guardband
							
							for(int i = 0; i < demandInSlots; i++) {
								temp.add(flow.getSlotList().get(i));
							}
							
							updateData(flow, flow.getLinks(), temp, flow.getModulationLevel());
						}
						flows.remove(flow.getID());
						this.activeFlows.put(flow.getID(), flow);
						n--;
					}		
				}
			}
		}
		
		return (n == 0);

	}
	
	
	private void updateXT() {
		for(int i = 0; i < this.pt.getNumLinks(); i++) {
			this.pt.getLink(i).updateCrosstalk();
		}
	}
	
	
	public boolean[][]bitMapAll(int []links) {
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
		
		for(int i : links) {
			bitMap(pt.getLink(i).getSpectrum(), spectrum, spectrum);
		}
		
		this.totalSlotsAvailable = 0;
		for(int i = 0; i < spectrum.length; i++) {
			for(int j = 0; j < spectrum[i].length; j++) {
				
				if(spectrum[i][j]) {
					this.totalSlotsAvailable++;
				}
				
			}
		}
		
		return spectrum;
	}
	
	public void runDefragmentantion() {
		
		Map<Long, Flow> flows = cp.getActiveFlows();
		ArrayList<Flow> secondChance = new ArrayList<Flow>();

		if(flows.size() <= 10 || !this.runKMeans(this.k, flows)) {
			
			return;
		}
		
		System.out.println("NUMBER OF FLOWS: "+flows.size());
		this.activeFlows = new HashMap<Long, Flow>(); 
		this.pt.resetAllSpectrum();

//		this.distributeCores();
		
		//re-assigned resources in the same link, but using clustering
		for(Integer key: clusters.keySet()) {
			if(!clusters.get(key).isEmpty()) {
				
				clusters.get(key).sort((a, b) -> b.getRate() - a.getRate());
				for(Flow flow: clusters.get(key)) {	
					
					if(flows.containsKey(flow.getID())) {
						
						if(!fitConnection(flow, bitMapAll( flow.getLinks() ), flow.getLinks() ))
						{
							secondChance.add(flow);
						}
						else 
						{
							this.activeFlows.put(flow.getID(), flow);
							
							flow.setCore(flow.getSlotList().get(0).c);
						}
					}
				}
				
				updateXT();
				
			}
		}

		if(secondChance.size() >= 1)
		{
			System.out.println("DISRUPTIONS: "+secondChance.size());
			if(lastChanceToAllocating(secondChance)) 
			{
				this.nConnectionDisruption = 0;
			}
			
			secondChance.clear();
		}
		
		clusters.clear();
	}

	protected void distributeCores() {
		
		this.cores = new int[this.clusters.size()];
		
		int []totalSlots = new int[this.clusters.size()];
		int []nLinks = new int[this.clusters.size()];
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		
		int nCores = pt.getCores();
		for(Integer k: clusters.keySet()) {
			
			int slots = 0;
			int links = 0;
			
			for(int i = 0; i < clusters.get(k).size(); i++) {
				Flow flow = clusters.get(k).get(i);
				slots +=  Math.ceil((double)flow.getRate() / ModulationsMuticore.subcarriersCapacity[0]) ;
				links += (flow.getLinks().length);
			}
			
			totalSlots[k] = slots / (int)Math.ceil((double)links/(double)clusters.get(k).size());
			nLinks[k] = (int) Math.ceil((double)links/(double)clusters.get(k).size());
			indexes.add(k);
		}
		
		for(int i = 0; i < cores.length; i++) {
			
			int n = (int)Math.ceil( (double)totalSlots[i] / (double)pt.getNumSlots() );
			this.cores[i] = n <= 0 || n >= pt.getCores() ? 1 : n;
			this.cores[i] = this.cores[i] >= 3 ? 2 :  this.cores[i];
//			System.out.println("cluster-"+i+": "+n+" nslots: "+ totalSlots[i]);
			nCores -= this.cores[i];
		}
		
		indexes.sort((a,b) -> totalSlots[b] - totalSlots[a]);
		if(nCores >= 1)
		{	
			int n = (int) Math.ceil( (double)nCores/ (double)cores.length);
			
			for(int i = 0; i < cores.length && nCores >= 1; i++) {
				
				this.cores[indexes.get(i)] = this.cores[indexes.get(i)] + n;
				nCores -= n;
				
				if(nCores < n) {
					n = nCores;
				}
			}
		}
		
	}

	
	protected boolean updateLightpath(Flow flow) {
		if (flow.getLightpathID() >= 0) {
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
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int modulation = chooseModulationFormat(flow.getRate(), flow.getLinks());
		
		while(modulation >= 0) {
			
			double requestedBandwidthInGHz = ( ((double)flow.getRate()) / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			fittedSlotList = FirstFitPolicy(flow, spectrum, links, demandInSlots, modulation);
			
			if(fittedSlotList.size() == flow.getSlotListSize()) 
			{
				return updateData(flow, links, fittedSlotList, flow.getModulationLevel());
				
			}
			
			modulation--;
		}
		
		return false;
	}
	
	private ArrayList<Slot> getMatchingSlots(ArrayList<Slot> s1, ArrayList<Slot> s2, LinkedList<Integer> adjacents) {
		
		boolean isAdjacent = false;
		for (int i : adjacents) {
			
			if(i == s2.get(0).c) {
				isAdjacent = true;	
			}
		}
		
		if (isAdjacent) {
			ArrayList<Slot> slots = new ArrayList<Slot>();
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
		
		return new ArrayList<Slot>();
	}
	
	private ArrayList<Integer>getMatchingLinks(int []l1, int []l2) {
		
		ArrayList<Integer> links = new ArrayList<Integer>();
		for(int i: l1) {
			
			for(int j: l2) {
				
				if(i == j) {
					links.add(i);
				}
				
			}
		}
		
		return links;
	}
	
	public boolean CrosstalkIsAcceptable(Flow flow, int[] links, ArrayList<Slot> slotList, double db) {
		
		double xt = 0;
		
		xt += pt.canAcceptCrosstalk(links, slotList, db);
		
		for(Long key: this.activeFlows.keySet()) {
			
			if(key == flow.getID()) {
				continue;
			}
			
			ArrayList<Integer> l = getMatchingLinks(this.activeFlows.get(key).getLinks(), links);
			if(!l.isEmpty()) {
				ArrayList<Slot> t = getMatchingSlots(slotList, this.activeFlows.get(key).getSlotList(), pt.getLink(0).getAdjacentCores(slotList.get(0).c) );
				if(!t.isEmpty()) 
				{
					xt += pt.canAcceptInterCrosstalk(this.activeFlows.get(key), l, this.activeFlows.get(key).getSlotList(), t);
				}
				else
				{
					xt += pt.canAcceptInterCrosstalk(this.activeFlows.get(key), l, this.activeFlows.get(key).getSlotList());
				}
			}
		}
		
		xt = xt > 0 ? ( 10.0f * Math.log10(xt)/Math.log10(10) ) : -80f;//db
		
		return xt <= db;
	}
	
	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<ArrayList<Slot>> setOfSlots = new ArrayList<ArrayList<Slot>> ();
		
		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
		for(Integer i: priorityCores) {
			
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int j = 0; j < spectrum[i].length; j++) {	
				
				if(spectrum[i][j] == true) {
					temp.add( new Slot(i,j) );
				}
				else {
					
					if(!temp.isEmpty())temp.clear();
					if(Math.abs(spectrum[i].length-j) < demandInSlots) {
						break;
					}
				}
				
				if(temp.size() == demandInSlots) {
					
					if(CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {
						
						setOfSlots.add(new ArrayList<Slot>(temp));
					}
					
					break;
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
			
			for(ArrayList<Slot> candidate: setOfSlots) {
				
				if(CrosstalkIsAcceptable(flow, links, candidate, ModulationsMuticore.inBandXT[modulation])) {
					return candidate;	
				}
			}
			
				
		}
	    
		return new ArrayList<Slot>();
	}

	protected boolean runKMeans(int k, Map<Long, Flow> flows) {
		
		double[][] features = new double[flows.size()][5];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Long f: flows.keySet()) {
			
			double length = 0;
			for(int l : flows.get(f).getLinks()) {
				length += pt.getLink(l).getDistance();
			}
			
			features[i][0] = length;
			features[i][1] = flows.get(f).getRate();
			features[i][2] = flows.get(f).getLinks().length;//number of hops
			features[i][3] = flows.get(f).getModulationLevel();
			features[i][4] = Math.abs(flows.get(f).getDuration() - cp.time) * 100;
			
			listOfFlows.add(flows.get(f));
			i++;
		}
		
		PythonCaller caller = new PythonCaller();
		KMeansResult result = caller.kmeans(features, k);
		
		System.out.println("silhouette: "+result.getSilhouette());
		if(result.getSilhouette() <= 0.6) {
			
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
		
	}

	public void initialize(PhysicalTopology pt, VirtualNetworkEmbedding vne, VonControlPlane cp) {
		this.cp = cp;
		this.pt =pt;
		this.vne = vne;
	}
}
