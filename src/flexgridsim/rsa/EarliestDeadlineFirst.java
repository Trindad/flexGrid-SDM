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
	@SuppressWarnings("unlikely-arg-type")
	public void deadlineArrival(BatchConnectionRequest batch) 
    {
		
		System.out.println("**********************************************************");
		ArrayList<Flow> postponedRequests = new ArrayList<Flow>();
		ArrayList<Flow> blockedRequests = new ArrayList<Flow>();
		
        int []path = new int[0];
        
        for(Flow f: batch) {
        	System.out.println(f);
        }
        
        while( batch.getNumberOfFlows() >= 1 )
        {   
        	Flow batchFlow;
        	
        	batchFlow = batch.convertBatchToSingleFlow();
        	cp.newFlow(batchFlow);
        	
        	path = executeRCSA(batchFlow);//RSA using Image
        	
        	if (path.length == 0) 
            {    
        		cp.removeFlow(batchFlow.getID());
        		batch.remove(batchFlow.getID());
        		
            	Flow latestDeadline = batch.latestFlow();

            	double minDeadline = batch.getEarliestDeadline().getTime();
            	
//            	System.out.println("currentTime:  "+ minDeadline+ " latestDeadline: "+latestDeadline.getDeadline()+" "+batch.size());

                if ( ( latestDeadline.getDeadline() > minDeadline  || batch.getNumberOfJointFlows() == 0) && (latestDeadline.isBatchRequest() == false) )
                {
                	System.out.println("postponed: "+" s: "+latestDeadline.getSource()+" d: "+latestDeadline.getDestination()+" deadline:"+ latestDeadline.getDeadline()+ " t: "+latestDeadline.getTime()+" bw:"+latestDeadline.getRate()+" tam:"+ batch.size());
                	
                	latestDeadline.setBatchRequest(true);
                	postponedRequests.add(latestDeadline);
                }
                else
                {
                	System.out.println("block: " +" s: "+latestDeadline.getSource()+" d: "+latestDeadline.getDestination()+" deadline:"+ latestDeadline.getDeadline()+ " t: "+latestDeadline.getTime()+" bw:"+latestDeadline.getRate()+" tam:"+ batch.size());
                	blockedRequests.add(latestDeadline);
                }
                
                batch.removeFlow(latestDeadline);
            }
            else
            {
            	System.out.println("established: " +" s: "+batch.getSource()+" d: "+batch.getDestination()+" deadline:"+ batchFlow.getDeadline()+ " t: "+batchFlow.getTime()+" bw:"+batchFlow.getRate()+" tam:"+ batch.size());
            	 
            	cp.removeDeadlineEvent(batch);
            	removeFlowsOfBatch(batch);
            }
        	
        }
        
        postponeFlows(postponedRequests, batch);
        	
        for(Flow f: blockedRequests) 
        {
			cp.blockFlow(f.getID());
        }
        
    }
	
	private void postponeFlows(ArrayList<Flow> postponedRequests, BatchConnectionRequest batch) 
	{
		if(postponedRequests.isEmpty())
		{
			if (batch.isEmpty()) {
				cp.removeDeadlineEvent(batch);
			}
			
			return;
		}
		 
		for(Flow f: postponedRequests)
		{
			batch.add(f);
			f.setBatchRequest(true);
		}
		
        cp.updateDeadlineEvent(batch);
        
	}
	
	private void removeFlowsOfBatch(BatchConnectionRequest batch) {
		
		for(Flow flow : batch) 
		{
			flow.setBatchRequest(true);
			cp.removeFlow(flow.getID());
		}
		
		try 
		{
			cp.removeDeadlineEvent(batch);
			batch.clear();
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
