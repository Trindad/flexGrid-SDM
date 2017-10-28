package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.ConnectedComponent;
import flexgridsim.util.InscribedRectangle;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.Rectangle;
import flexgridsim.util.WeightedGraph;
import flexgridsim.BatchConnectionRequest;


public class EarliestDeadlineFirst extends RCSA {
	
	protected PhysicalTopology pt;
	protected VirtualTopology vt;
	protected ControlPlaneForRSA cp;
	protected WeightedGraph graph;
	
	private RSAProxy rsa;
	 
	 public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp,
				TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
			
		rsa = new RSAProxy(cp.getRsaAlgorithm());
		rsa.simulationInterface(xml, pt, vt, cp, traffic);
	}
	 
	/**
     * 
     * @param flow
     */
	public void deadlineArrival(BatchConnectionRequest batch) 
    {   
		ArrayList<Flow> postponedRequests = new ArrayList<Flow>();
		ArrayList<Flow> blockedRequests = new ArrayList<Flow>();
		Flow batchFlow;
		
		int []path = new int[0];
		
		while( batch.getNumberOfFlows() >= 1 ) 
		{  	 
		    batchFlow = batch.convertBatchToSingleFlow();
		    cp.newFlow(batchFlow);
		
			path = rsa.executeRCSA(batchFlow);//RSA using Image
		
			if (path.length == 0) 
		    {    
				if(batchFlow.getNumberOfFlowsGroomed() >= 2) 
				{
					cp.removeFlow(batchFlow.getID());
				}
				
				
//		    	Flow latestDeadline = batch.largestRate();//Inverse
//		    	Flow latestDeadline = batch.latestFlow();
		    	Flow latestDeadline = batch.smallestRate();
		    	
		        canBePostpone(batch, postponedRequests,blockedRequests, latestDeadline);		

		        batch.removeFlow(latestDeadline);
		    }
		    else
		    {
		    	if(batch.size() >= 2)
		    	{
		    		System.out.println("number of flows: "+batch.getNumberOfFlows());
		    	}
		    	for(Flow f: batch) 
		    	{
		    		System.out.println("established: "+f+" time: "+f.getTime() + " deadline: "+f.getDeadline());
		    	}
		    	
		    	batch.setEstablished(true);
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

	private void canBeBlock(BatchConnectionRequest batch, ArrayList<Flow> blockedRequests, Flow latestDeadline) 
	{
		int []path = new int[0];
		
		path = rsa.executeRCSA(latestDeadline);//RSA using Image
		
		if(path.length == 0)
		{
			System.out.println("blocked: "+latestDeadline+" time: "+latestDeadline.getTime() + " deadline: "+latestDeadline.getDeadline());
        	blockedRequests.add(latestDeadline);
		}	
		else 
		{
			System.out.println("established*: "+latestDeadline+" time: "+latestDeadline.getTime() + " deadline: "+latestDeadline.getDeadline());
		}
	}
	
	@SuppressWarnings("unused")
	private void postponeConditionLargestRate(BatchConnectionRequest batch, Flow request, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests) {
		
		if( (request.getTime()/request.getDeadline()) >= 0.6f && request.getRate() <= 1244)
		{
			System.out.println("postponed: "+request+" time: "+request.getTime() + " deadline: "+request.getDeadline());
    	
			postponedRequests.add(request);
			request.setPostponeRequest(true);
		}
		else
		{
			canBeBlock(batch, blockedRequests, request) ;
		}	
	}
	
	@SuppressWarnings("unused")
	private void postponeConditionSmallestRate(BatchConnectionRequest batch, Flow request, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests) {
		
		if(request.getRate() <= 622)//smallest bit-rate
		{
			System.out.println("postponed: "+request+" time: "+request.getTime() + " deadline: "+request.getDeadline());
        	
			postponedRequests.add(request);
			request.setPostponeRequest(true);
		}
		else
		{
			canBeBlock(batch, blockedRequests, request) ;
		}	
	}
	
	@SuppressWarnings("unused")
	private void justPostpone(Flow request, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests) {
		
		System.out.println("postponed: "+request+" time: "+request.getTime() + " deadline: "+request.getDeadline());
		postponedRequests.add(request);
    	request.setPostponeRequest(true);
	}
	
	/**
	 * 
	 * @param batch
	 * @param postponedRequests
	 * @param blockedRequests
	 * @param request
	 */
	private void canBePostpone(BatchConnectionRequest batch, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests, Flow request)
	{
    	
    	if( request.getDeadline() <= cp.getTime())
        {
    		canBeBlock(batch, blockedRequests, request) ;
        }
        else
        {
        	/**
        	 *  batch.largestRate() BBR: 18.502337% blocked = 4.87%
        	 *  batch.latestFlow BBR: 26.861143% blocked = 10.12%
        	 *  batch.smallestRate BBR: 31.757801% blocked = 13.570001%
        	 */
//        	justPostpone(request, postponedRequests, blockedRequests);
        	/**
        	 * batch.largestRate()BBR: 15.319607% blocked = 3.12%
        	 * batch.latestFlow BBR: 15.14443% blocked = 3.08%
        	 * batch.smallestRate BBR: 15.126721% blocked = 2.98%
        	 */
//        	postponeConditionSmallestRate(batch, request, postponedRequests, blockedRequests);
        	/**
        	 * batch.largestRate() BBR: 14.601167% blocked= 2.72% 
        	 * batch.latestFlow BBR: 17.953188% blocked = 4.5499997%
        	 * batch.smallestRate BBR: 17.738594% blocked = 4.44% 
        	 */
        	postponeConditionLargestRate(batch, request, postponedRequests, blockedRequests);
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
		
//		System.out.println(batch.getNumberOfFlows() + ", " + batch.getOldestDeadline().getTime() + ", " + batch.getEarliestDeadline().getTime());
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
	
	
	private class RSAProxy {
		private Object rsa;
		private String algorithm;
		
		public RSAProxy(String algorithm) {
			super();
			this.algorithm = algorithm;
			
			if (algorithm.equals("flexgridsim.rsa.MyImageRCSA")) {
	        	rsa = new MyImageRCSA();
	        } else if (algorithm.equals("flexgridsim.rsa.MyInscribedRectangleRCSA")) {
	        	rsa = new MyInscribedRectangleRCSA();
	        }
		}


		public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp,
				TrafficGenerator traffic) {
			
			if (algorithm.equals("flexgridsim.rsa.MyImageRCSA")) {
	        	((MyImageRCSA) rsa).simulationInterface(xml, pt, vt, cp, traffic);
	        } else if (algorithm.equals("flexgridsim.rsa.MyInscribedRectangleRCSA")) {
	        	((MyInscribedRectangleRCSA) rsa).simulationInterface(xml, pt, vt, cp, traffic);
	        }
		}
		
		public int[] executeRCSA(Flow flow) {
			if (algorithm.equals("flexgridsim.rsa.MyImageRCSA")) {
	        	return ((MyImageRCSA) rsa).executeRCSA(flow);
	        } else if (algorithm.equals("flexgridsim.rsa.MyInscribedRectangleRCSA")) {
	        	return ((MyInscribedRectangleRCSA) rsa).executeRCSA(flow);
	        }
			
			return null;
		}
	}
	
	@SuppressWarnings("unused")
	private class MyImageRCSA  extends ImageRCSA{
		
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
	
	
	@SuppressWarnings("unused")
	private class MyInscribedRectangleRCSA  extends InscribedRectangleRCSA {
		
		public  int[] executeRCSA(Flow flow) {
			
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
				
//				printSpectrum(spectrum);
				
				InscribedRectangle ir = new InscribedRectangle();
				ArrayList<Rectangle> rectangles = ir.calculateRectangles(spectrum.length, spectrum[0].length, spectrum);

				int[] links = new int[kPaths[k].length - 1];
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
				}

				if (fitConnection(rectangles, demandInSlots, links, flow, spectrum)){
					return links;
				}
			}
			cp.blockFlow(flow.getID());
			return new int[0];
		}
	}

}
