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
	protected int numberOfAvailableSlots = 0;
	
	protected RSAProxy rsa;
	 
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
	
			
		while( batch.getNumberOfFlows() >= 1 ) 
		{  	 
			int n = 0;
			
			batchFlow = batch.convertBatchToSingleFlow();
		    cp.newFlow(batchFlow);
		    n = runRCSA(batchFlow,batch);
		   
		    if(batchFlow.getNumberOfFlowsGroomed() >= 2) 
			{
				cp.removeFlow(batchFlow.getID());
			}
			
			if (n == 0) 
		    { 
//		    	Flow latestDeadline = batch.largestRate();
			    Flow latestDeadline = batch.latestFlow();
//			    Flow latestDeadline = batch.smallestRate();
//				Flow latestDeadline = batch.largestDuration();
//				Flow latestDeadline = batch.smallestDuration();
		    	
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

	public int runRCSA(Flow flow, BatchConnectionRequest batch) {

		int []path = new int[0];
	    
		path = rsa.executeRCSA(flow);
		this.numberOfAvailableSlots = rsa.getNumberOfAvailableSlots();
		
		if(path.length >= 1)
		{
//		   if(batch.size() >= 2)
//		   {
//		    		System.out.println("number of flows: "+batch.getNumberOfFlows());
//			}
			for(Flow f: batch) 
			{
				System.out.println("established: "+f+" time: "+f.getTime() + " deadline: "+f.getDeadline());
			}
			
			batch.setEstablished(true);
			removeFlowsOfBatch(batch);
		}
		
		return path.length;//RSA using Image
	}
	
	private void canBeBlocked(BatchConnectionRequest batch, ArrayList<Flow> blockedRequests, Flow latestDeadline) 
	{
		System.out.println("blocked: "+latestDeadline+" time: "+latestDeadline.getTime() + " deadline: "+latestDeadline.getDeadline());
        blockedRequests.add(latestDeadline);
	}
	
	@SuppressWarnings("unused")
	private void postponeConditionLargestRate(BatchConnectionRequest batch, Flow request, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests) {

		//select the best option to postponed
		if(request.getRate() >= 622)
		{
			System.out.println("postponed: "+request+" time: "+request.getTime() + " deadline: "+request.getDeadline());
    	
			postponedRequests.add(request);
			request.setPostponeRequest(true);
			
			return;
		}	
		
		canBeBlocked(batch, blockedRequests, request);
	}
	
	@SuppressWarnings("unused")
	private void postponeConditionSmallestRate(BatchConnectionRequest batch, Flow request, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests) {
		
			if(request.getRate() < 622)//smallest bit-rate
			{
				System.out.println("postponed: "+request+" time: "+request.getTime() + " deadline: "+request.getDeadline());
	        	
				postponedRequests.add(request);
				request.setPostponeRequest(true);
				return;
			}
		
			canBeBlocked(batch, blockedRequests, request);
	}

	@SuppressWarnings("unused")
	private void justPostpone(BatchConnectionRequest batch, Flow request, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests) {
			
			System.out.println("postponed: "+request+" time: "+request.getTime() + " deadline: "+request.getDeadline());
			postponedRequests.add(request);
	    	request.setPostponeRequest(true);
	}
	
	
	public boolean differenceBeteweenDeadlineAndArrivalTime(double time, double deadline) {
		
		double diference = time/deadline;
				
		if(diference <= 0.7f)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param batch
	 * @param postponedRequests
	 * @param blockedRequests
	 * @param request
	 */
	protected void canBePostpone(BatchConnectionRequest batch, ArrayList<Flow> postponedRequests, ArrayList<Flow> blockedRequests, Flow request)
	{
		if( request.getDeadline() <= cp.getTime() || request.getDeadline() <= request.getTime() )
    	{
    		canBeBlocked(batch, blockedRequests, request) ;
        }
        else
        {
        	justPostpone(batch, request, postponedRequests, blockedRequests);
        	
//        	postponeConditionSmallestRate(batch, request, postponedRequests, blockedRequests);
        
//        	postponeConditionLargestRate(batch, request, postponedRequests, blockedRequests);
        }
	}

	protected void postponeFlows(ArrayList<Flow> postponedRequests, BatchConnectionRequest batch) 
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
	protected void removeFlowsOfBatch(BatchConnectionRequest batch) {
		
		try 
		{
			cp.removeDeadlineEvent(batch);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}
	
	protected class RSAProxy {
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


		public WeightedGraph getGraph() {
			
			return graph;
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
		
		public int getNumberOfAvailableSlots() {
			
			if (algorithm.equals("flexgridsim.rsa.MyImageRCSA")) {
	        	return ((MyImageRCSA) rsa).getNumberOfAvailableSlots();
	        } 
			
			return 0;
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
				
//				System.out.println(listOfRegions.size());
				int[] links = new int[kPaths[k].length - 1];
				
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
				}
				
				if (fitConnection(listOfRegions, demandInSlots, links, flow))
					return links;
				
			}
			
			return new int[0];
		}
		
		public int getNumberOfAvailableSlots() {
			
			return this.availableSlots;
		}
		
	}
	
	
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