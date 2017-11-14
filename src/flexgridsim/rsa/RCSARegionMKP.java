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

public class RCSARegionMKP extends EarliestDeadlineFirst {
	
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
		
		private double slotCapacity;
		private int nSlots;
		private int nCores;
	
		MKPRCSA(WeightedGraph g, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp) {
			
			this.graph = g;
			this.nCores = pt.getCores();
			this.nSlots = pt.getNumSlots();
			this.slotCapacity = pt.getSlotCapacity();
			this.pt = pt;
			this.vt = vt;
			this.cp = cp;
			
		}
		/**
		 * Execute RCSA using Image
		 * @param flow
		 * @return
		 */
		public boolean run(BatchConnectionRequest batch) {
			
			int demandInSlots = nSlots;
	
			KShortestPaths kShortestPaths = new KShortestPaths();
			int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, batch.getSource(), batch.getDestination(), 5);
			boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
	
			int cont = batch.getNumberOfFlows();
			
			for (int k = 0; k < kPaths.length; k++) {
				
				if(cont == 0)
				{
					batch.setEstablished(true);
			    	removeFlowsOfBatch(batch);
			    	
					return true;
				}
	
				for (int i = 0; i < spectrum.length; i++) {
					for (int j = 0; j < spectrum[i].length; j++) {
						spectrum[i][j]=true;
					}
				}
				for (int i = 0; i < kPaths[k].length-1; i++) {
					imageAnd(pt.getLink(kPaths[k][i], kPaths[k][i+1]).getSpectrum(), spectrum, spectrum);
				}
				
				//printSpectrum(spectrum);
				ConnectedComponent cc = new ConnectedComponent();
				HashMap<Integer,ArrayList<Slot>> listOfRegions = cc.listOfRegions(spectrum);
				
				if (listOfRegions.isEmpty()){
					
					continue;
				}
				
				ArrayList<Integer> available = new ArrayList<Integer>();
				ArrayList<Integer> indexOfPaths = new ArrayList<Integer>();
				for(Integer key : listOfRegions.keySet())
				{
					if(listOfRegions.get(key).size() >= batch.minRate())
					{
						indexOfPaths.add(key);
						available.add(listOfRegions.get(key).size());
					}
					
				}
				
	//				System.out.println(listOfRegions.size());
				int[] links = new int[kPaths[k].length - 1];
				
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
				}
				
				OptimizedResourceAssignment mkp = new OptimizedResourceAssignment(available, batch, cp.isCostMKP(), pt.getSlotCapacity(), cp.getTime());
				ArrayList<ArrayList<Integer>> solution = mkp.getEachDemandPerPath();
				
				if(solution.size() >= 1)
				{
					ArrayList<Integer> established = new ArrayList<Integer>();
					System.out.println("solução "+solution);
					System.out.println(available);
					System.out.println(indexOfPaths);
					for(int i = 0; i < solution.size(); i++) {
						
						if(solution.get(i).size() >= 1) 
						{
	//							System.out.println(" solução: "+solution);
							
							Flow newFlow = convertBatchToSingleFlow(solution.get(i), batch);
							cp.newFlow(newFlow);
							int nSlots = (int) Math.ceil( newFlow.getRate() / this.slotCapacity);
							
							HashMap<Integer,ArrayList<Slot>> region = new HashMap<Integer,ArrayList<Slot>>();
							region.put(indexOfPaths.get(i), listOfRegions.get(indexOfPaths.get(i)));
	
							if( fitConnection(region, nSlots, links, newFlow) == true)
							{
								for(int u = 0; u < solution.get(i).size(); u++) {
									
									System.out.println("established*: " + solution.get(i).get(u) + " : " + batch.get(solution.get(i).get(u))+" time: "+batch.get(solution.get(i).get(u)).getTime() + " deadline: "+batch.get(solution.get(i).get(u)).getDeadline());
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
						indexOfPaths.clear();
					}
				}
				
			}
			
			return false;
		}
		
			private Flow convertBatchToSingleFlow(ArrayList<Integer> flows, BatchConnectionRequest batch) {
				
	//				System.out.println();
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
