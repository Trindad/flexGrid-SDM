package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.util.ConnectedComponent;
import flexgridsim.util.KShortestPaths;
import flexgridsim.BatchConnectionRequest;


public class EarliestDeadlineFirst extends ImageRCSA {
	/**
     * 
     * @param flow
     */
	public void deadlineArrival(BatchConnectionRequest batch) 
    {
		ArrayList<Flow> postponedRequests = new ArrayList<Flow>();
		ArrayList<Flow> blockedRequests = new ArrayList<Flow>();
		
        int []path = new int[0];
        
        for(Flow f: batch) {
        	System.out.println(f + ", postponed? " + f.isPostponeRequest());
        }
        
        while( batch.getNumberOfFlows() >= 1 )
        {   
        	Flow batchFlow;
        	
        	batchFlow = batch.convertBatchToSingleFlow();
        	cp.newFlow(batchFlow);
        	
        	path = executeRCSA(batchFlow);//RSA using Image
        	
        	if (path.length == 0) 
            {    
        		if(batchFlow.getNumberOfFlowsGroomed() >= 2) 
        		{
        			cp.removeFlow(batchFlow.getID());
        		}
        		
            	Flow latestDeadline = batch.latestFlow();

            	double minDeadline = batch.getEarliestDeadline().getTime();
            	
//            	if ( ( latestDeadline.getDeadline() > minDeadline) && ( latestDeadline.isPostponeRequest() == true )  ) {
//            		System.out.println("esse caso aqui");
//            	}
            	
//            	System.out.println(" "+latestDeadline.getDeadline()+" "+ minDeadline+" "+cp.getEarliestDeadline());
                if ( ( latestDeadline.getDeadline() > minDeadline) || latestDeadline.isPostponeRequest() == false )
                {
                	System.out.println("postponed: "+latestDeadline);
                	
                	postponedRequests.add(latestDeadline);
                }
                else
                {
                	System.out.println("blocked: "+latestDeadline);
                	blockedRequests.add(latestDeadline);
                }
                
                batch.removeFlow(latestDeadline);
            }
            else
            {
            	for(Flow f: batch) 
            	{
            		System.out.println("established: "+f);
            	}
            	
            	removeFlowsOfBatch(batch);
            }
        	
        }
        
        postponeFlows(postponedRequests, batch);
        	
        for(Flow f: blockedRequests) 
        {
			if( cp.blockFlow(f.getID()) == false) 
			{
				System.out.println("error while blocking");
			}
        }
        
    }
	
	private void postponeFlows(ArrayList<Flow> postponedRequests, BatchConnectionRequest batch) 
	{
		if(postponedRequests.isEmpty())
		{
			
			if (batch.isEmpty()) 
			{
				cp.removeDeadlineEvent(batch);
			}
			
			return;
		}
		 
		for(Flow f: postponedRequests)
		{
			f.setPostponeRequest(true);
			batch.add(f);	
			
		}
		System.out.println(batch.getNumberOfFlows() + ", " + batch.getOldestDeadline().getTime() + ", " + batch.getEarliestDeadline().getTime());
        cp.updateDeadlineEvent(batch);
        
	}
	
	/**
	 * 
	 * @param batch of requests
	 */
	private void removeFlowsOfBatch(BatchConnectionRequest batch) {
		
		try 
		{
			cp.removeDeadlineEvent(batch);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Execute RCSA using Image
	 * @param flow
	 * @return
	 */
	public int[] executeRCSA(Flow flow) {
		
		int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 5);
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		
		for (int k = 0; k < kPaths.length; k++) {
			
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
			
			int[] links = new int[kPaths[k].length - 1];
			
			for (int j = 0; j < kPaths[k].length - 1; j++) {
				links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
			}
			if (fitConnection(listOfRegions, demandInSlots, links, flow))
				return links;
			
		}
		
		return new int[0];
	}
}
