package flexgridsim;

import java.util.ArrayList;
import java.util.Optional;

public class SetOfBatches extends ArrayList<BatchConnectionRequest>{

	private static final long serialVersionUID = 6955514003876428494L;
	private BatchConnectionRequest earliestDeadline;

	public long getNumberOfBatches() {
		return this.size();
	}
	
	/**
	 * New flow 
	 * @param flow
	 */
	public BatchConnectionRequest addFlow(Flow flow) {
		
		for(BatchConnectionRequest batch: this) 
		{
			if(batch.getSource() == flow.getSource() && batch.getDestination() == flow.getDestination()) 
			{
				batch.add(flow);
				setEarliestDeadline(flow);
				return batch;
			}
		}
		
		// create a new batch
		BatchConnectionRequest newBatch = new BatchConnectionRequest(flow.getSource(), flow.getDestination());
		newBatch.add(flow);		
		this.add(newBatch);
		setEarliestDeadline(flow);
		
		return newBatch;
	}
	
	public void setEarliestDeadline(Flow flow) {
		
		if(earliestDeadline == null)
		{
			earliestDeadline = this.getBatch(flow.getSource(), flow.getDestination());
		}
		else
		{
			if(flow.getDeadline() < earliestDeadline.getEarliestDeadline().getTime())
			{
				earliestDeadline = this.getBatch(flow.getSource(), flow.getDestination());
			}
		}
	}
	
	public void updateEarliestDeadlineFirst() {
		
		BatchConnectionRequest newEarliest = null;
		double temp = Double.MAX_VALUE;
		double max = Double.MAX_VALUE;
		
		for(BatchConnectionRequest b: this) {
			
			if(b.getNumberOfFlows() > 0 && b.getEarliestDeadline().getTime() < max) 
			{
				max = temp;
				temp = b.getEarliestDeadline().getTime();
				newEarliest = b;
			}
		}
		
		if(newEarliest != null) earliestDeadline = newEarliest; 
	}
	
	/**
	 * Remove batch
	 * @param batch
	 * @return
	 */
	public boolean removeBatch(BatchConnectionRequest batch) {
		
		try {
			this.remove(batch);
			updateEarliestDeadlineFirst();
			
			return true;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return false;
	}
	
	
	public BatchConnectionRequest getBatch(int source, int destination) {
		
		try 
		{
			Optional<BatchConnectionRequest> optional = this.stream().filter(x -> x.getSource() 
															== source && x.getDestination() == destination).findFirst();
			if (optional.isPresent()) {
				return optional.get();
			}
			
			return null;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return null;
	}

	public void setEarliestDeadline(BatchConnectionRequest earliestDeadline2) {
		earliestDeadline = earliestDeadline2;
	}
	
	public BatchConnectionRequest getEarliestDeadline() {
			
		return earliestDeadline;
	}

	public void resetEarliestDeadline() {
		
		earliestDeadline.clear();
		earliestDeadline = null;
	}
	
}
