package flexgridsim;

import java.util.ArrayList;
import java.util.Comparator;

import flexgridsim.Flow;

/**
 * 
 * @author trindade
 *
 */
public class BatchConnectionRequest extends ArrayList<Flow> {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int source;
    private int destination;
    private int []links;//path
    private int demandInSlots;
    private boolean established;
    
    private DeadlineEvent earliestDeadline;
    private DeadlineEvent oldestDeadline;
    
    static int  indexFlow = 0;
   
    
    public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public int getDestination() {
		return destination;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}

	public BatchConnectionRequest(int source, int destination) 
    {
    	established = false;
    	this.source = source;
    	this.destination = destination;
    	links = new int[0]; 
    	
    	earliestDeadline = new DeadlineEvent(Double.MAX_VALUE, this);
    	oldestDeadline = new DeadlineEvent(Double.MAX_VALUE, this);
    }
    
	public boolean isEstablished() {
		return established;
	}

	public void setEstablished(boolean established) {
		this.established = established;
	}

	public Flow getFlow(int index) {
		return this.get(index);
	}

	public int[] getLinks() {
		return links;
	}

	public void setLinks(int[] links) {
		this.links = links;
	}

	public int getDemandInSlots() {
		return demandInSlots;
	}

	public void setDemandInSlots(int demandInSlots) {
		this.demandInSlots = demandInSlots;
	}

    public boolean isEmptyPath() {

        return links.length == 0;
    }
    
	/**
	 * Result of the earliest deadline of the batch
	 * @param key
	 * @return
	 */
	public DeadlineEvent getEarliestDeadline() {

		return earliestDeadline;
	}
	
	/**
	 * Convert batch request to single flow
	 * @param flow
	 * @param key
	 * @return
	 */
	public Flow convertBatchToSingleFlow(){
		
		if(this.size() == 1)
		{
			return this.get(0);
		}
		
		int rateSum = 0;
		int maxCos = 0;
		double maxDuration = 0;
		
		//Get the maximum cost and the maximum time duration of requests in the batch
		for (Flow f : this)  
		{
			rateSum += f.getRate();
			
			if (f.getCOS() > maxCos) 
			{
				maxCos = f.getCOS();
			}
			if (f.getDuration() > maxDuration) 
			{
				maxDuration = f.getDuration();
			}
		}
		
		indexFlow++;
		//long id, int src, int dst, double time, int bw, double duration, int cos, double deadline
		Flow newFlow = new Flow(Long.MAX_VALUE - indexFlow, getSource(), getDestination(), 
				earliestDeadline.getTime(), rateSum, maxDuration, maxCos, 0);
		
		newFlow.setBatchRequest(true);
		newFlow.setNumberOfFlowsGroomed(this.size());
		
		return newFlow;//return a new flow composed by a set of requests
	}
	
	
	@Override
	public boolean add(Flow flow) {
		
		super.add(flow);
		
		
		if ( flow.getDeadline() < this.earliestDeadline.getTime() )
		{
			this.oldestDeadline = this.earliestDeadline;
			this.earliestDeadline = new DeadlineEvent(flow.getDeadline(), this);
		}
		
		flow.setBatchRequest(false);
		
		return true;
	}

	public void removeFlow(Flow flow) {
		
		this.remove(flow);

		//update deadline
		this.sort(Comparator.comparing(Flow::getDeadline));
		
		if(this.size() >= 2)
		{
			earliestDeadline = new DeadlineEvent(this.get(0).getDeadline(), this);
	    	oldestDeadline = new DeadlineEvent(this.get(this.size() - 1).getDeadline(), this);
		}
		else
		{
			earliestDeadline = new DeadlineEvent(Double.MAX_VALUE, this);
	    	oldestDeadline = new DeadlineEvent(Double.MIN_VALUE, this);
		}
		
	}

	public DeadlineEvent getOldestDeadline() {
		return oldestDeadline;
	}

	public void setOldestDeadline(DeadlineEvent oldestDeadline) {
		this.oldestDeadline = oldestDeadline;
	}

	public void setEarliestDeadline(DeadlineEvent earliestDeadline) {
		this.earliestDeadline = earliestDeadline;
	}

	public Flow latestFlow() {
		
		this.sort(Comparator.comparing(Flow::getDeadline));
		return ( this.get(this.size() - 1) );
	}

	public int getNumberOfFlows() {
		
		return this.size();
	}
}