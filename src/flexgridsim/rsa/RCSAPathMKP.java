package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import flexgridsim.BatchConnectionRequest;
import flexgridsim.Flow;
import flexgridsim.OptimizedResourceAssignment;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VirtualTopology;
import flexgridsim.util.ConnectedComponent;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.WeightedGraph;

public class RCSAPathMKP  extends EarliestDeadlineFirst{

	public void deadlineArrival(BatchConnectionRequest batch) 
    {   
		ArrayList<Flow> postponedRequests = new ArrayList<Flow>();
		ArrayList<Flow> blockedRequests = new ArrayList<Flow>();
		Flow batchFlow;
	
			
		while( batch.getNumberOfFlows() >= 1 ) 
		{  	 
			int n = 0;
			
			if(batch.size() >= 2)
			{	 
				if(rsa.getGraph() != null)
				{
					MKPRCSA optimization = new MKPRCSA(rsa.getGraph(), pt, vt, cp);
					
					if( optimization.run(batch) == true)
					{
						break;
					}
				}
				
			}
			else
			{
				batchFlow = batch.convertBatchToSingleFlow();
			    cp.newFlow(batchFlow);
			    n = runRCSA(batchFlow,batch);
			   
			    if(batchFlow.getNumberOfFlowsGroomed() >= 2) 
				{
					cp.removeFlow(batchFlow.getID());
				}
			}
		    
		    
			if (n == 0) 
		    { 
			    Flow latestDeadline = batch.latestFlow();
			    	
			    canBePostpone(batch, postponedRequests,blockedRequests, latestDeadline);
			    batch.removeFlow(latestDeadline);	 
		    }	
		}
	
		postponeFlows(postponedRequests, batch);
		
		for(Flow f: blockedRequests) 
		{
			if( cp.blockFlow(f.getID()) == false) 
			{
				System.out.println("error while blocking: "+ f);
			}
		}
    }

	
	@SuppressWarnings("unused")
	private class MKPRCSA extends ImageRCSA {

		private KShortestPaths kShortestPaths;
		private int[][] kPaths;
		
		private boolean[][] spectrum;
		private int []available;
		private int[][] listOfLinks;
		private WeightedGraph graph;
		private int nCores;
		private int nSlots;
		private double slotCapacity;
		private PhysicalTopology pt;
		private ArrayList<Integer> indexOfPaths;
		
		MKPRCSA(WeightedGraph g, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp) {
			
			this.graph = g;
			this.nCores = pt.getCores();
			this.nSlots = pt.getNumSlots();
			this.slotCapacity = pt.getSlotCapacity();
			this.pt = pt;
			this.vt = vt;
			this.cp = cp;
			
		}
		
		ArrayList<HashMap<Integer, ArrayList<Slot>>> listOfRegionsFromEachPath;
		
		public void initializeVariables(BatchConnectionRequest batch) {

			kShortestPaths = new KShortestPaths();
			int src =  batch.getSource(), dst =  batch.getDestination();
			this.kPaths = kShortestPaths.dijkstraKShortestPaths(graph, src, dst, 5);
			indexOfPaths = new ArrayList<Integer>();
		
			this.spectrum = new boolean[this.nCores][this.nSlots];
			this.available = new int[this.kPaths.length];
			this.listOfLinks = new int[this.available.length][];
			
			this.listOfRegionsFromEachPath = new ArrayList<HashMap<Integer, ArrayList<Slot>>> ();
			
			for (int k = 0; k < this.kPaths.length; k++) {

				for (int i = 0; i < spectrum.length; i++) {
					for (int j = 0; j < spectrum[i].length; j++) {
						spectrum[i][j]=true;
					}
				}
				for (int i = 0; i < kPaths[k].length-1; i++) {
					imageAnd(pt.getLink(kPaths[k][i], kPaths[k][i+1]).getSpectrum(), spectrum, spectrum);
				}
				
				
				
				ConnectedComponent cc = new ConnectedComponent();
				this.listOfRegionsFromEachPath.add(cc.listOfRegions(spectrum));
				
				if (this.listOfRegionsFromEachPath.get(k).isEmpty()) {
					continue;
				}

				for(Integer key : listOfRegionsFromEachPath.get(k).keySet())
				{
					this.available[k] += listOfRegionsFromEachPath.get(k).get(key).size();
				}
				
				int[] links = new int[this.kPaths[k].length - 1];
				
				for (int j = 0; j < this.kPaths[k].length - 1; j++) {
					links[j] = pt.getLink(this.kPaths[k][j], this.kPaths[k][j + 1]).getID();
				}
				
				this.listOfLinks[k] = links;
			}
		}
		
		public boolean run(BatchConnectionRequest batch) {

			initializeVariables(batch);
		
//			for(int i = 0; i < this.available.length; i++) System.out.println("* "+this.available[i]);
			int cont = batch.getNumberOfFlows();
			
			if(batch.getNumberOfFlows() >= 2)
			{	
				//MKP
				if(this.kPaths.length >= 2)
				{
					ArrayList<Integer> temp = new ArrayList<Integer>();
					
					double minRate = (double)batch.smallestRate().getRate();
					int minSlots = (int)Math.ceil(minRate/this.slotCapacity);
					
					for(int v = 0; v < available.length; v++) {
						
						if(available[v] >= minSlots)
						{
							indexOfPaths.add(v);
							temp.add(available[v]);
						}
					}
					
					if(temp.isEmpty() || indexOfPaths.isEmpty()) return false;
					
					OptimizedResourceAssignment mkp = new OptimizedResourceAssignment(temp, batch, cp.isCostMKP(), this.slotCapacity, cp.getTime());
					ArrayList<ArrayList<Integer>> solution = mkp.getEachDemandPerPath();
					
					if(solution.size() >= 1)
					{
						ArrayList<Integer> established = new ArrayList<Integer>();
//						System.out.println("solução "+solution);
//						System.out.println(temp);
//						System.out.println(indexOfPaths);
						for(int i = 0; i < solution.size(); i++) {
							
							if(solution.get(i).size() >= 1) 
							{
								System.out.println(solution.get(i));
								
								Flow newFlow = convertBatchToSingleFlow(solution.get(i), batch);
								 cp.newFlow(newFlow);
								int nSlots = (int) Math.ceil( newFlow.getRate() / this.slotCapacity);
								
								HashMap<Integer, ArrayList<Slot>> regions = this.listOfRegionsFromEachPath.get(indexOfPaths.get(i));
								int[] links = this.listOfLinks[indexOfPaths.get(i)];

								if( fitConnection(regions, nSlots, links, newFlow) == true)
								{
//									System.out.println("Accepted more than one request");	
									
									for(int u = 0; u < solution.get(i).size(); u++) {
										
//										Flow f = batch.get(solution.get(i).get(i));
//										batch.remove(f);
//										System.out.println(" "+);
										System.out.println("established: " + solution.get(i).get(u) + " : " + batch.get(solution.get(i).get(u))+" time: "+batch.get(solution.get(i).get(u)).getTime() + " deadline: "+batch.get(solution.get(i).get(u)).getDeadline());
										established.add(solution.get(i).get(u));
										cont--;
									}

									
								}
								
								if(newFlow.getNumberOfFlowsGroomed() >= 2) 
								{
									cp.removeFlow(newFlow.getID());
								}
								
							}
						}
						
						if( !established.isEmpty() )
						{
							//remove established connections from batch
							established.sort(Comparator.comparing(Integer::intValue));
							for(int i = established.size()-1; i >= 0; i--) {
								int u = established.get(i);
								batch.remove( batch.get(u) );
							}
							
							established.clear();
						}
						
					}
					
					//accepted all requests
					if(cont == 0)
					{
						batch.setEstablished(true);
				    	removeFlowsOfBatch(batch);
				    	
				    	return true;
					}
				}
			}
			
			return false;
		}

		private Flow convertBatchToSingleFlow(ArrayList<Integer> flows, BatchConnectionRequest batch) {
			
//			System.out.println();
			if(flows.size() == 1 || batch.size() == 1)
			{
				return batch.get(flows.get(0));
			}
			
			int rateSum = 0;
			int maxCos = 0;
			double maxDuration = 0;
			int i =0;
			//Get the maximum cost and the maximum time duration of requests in the batch
			for (i = 0; i < flows.size(); i++)  
			{
				int u = flows.get(i);
				rateSum += batch.get(u).getRate();
				
				if (batch.get(u).getCOS() > maxCos) 
				{
					maxCos = batch.get(u).getCOS();
				}
				if (batch.get(u).getDuration() > maxDuration) 
				{
					maxDuration = batch.get(u).getDuration();
				}
			}
			
			//long id, int src, int dst, double time, int bw, double duration, int cos, double deadline
			Flow newFlow = new Flow(Long.MAX_VALUE - 1, batch.getSource(), batch.getDestination(), 
					batch.getEarliestDeadline().getTime(), rateSum, maxDuration, maxCos, batch.getEarliestDeadline().getTime());
			
			newFlow.setBatchRequest(true);
			newFlow.setNumberOfFlowsGroomed(i);
			
			return newFlow;//return a new flow composed by a set of requests

		}
	}

}
