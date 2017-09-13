package flexgridsim;

import java.util.ArrayList;
import java.util.Optional;

public class SetOfBatches extends ArrayList<BatchConnectionRequest>{

	private static final long serialVersionUID = 6955514003876428494L;

	public long getNumberOfBatches() {
		return this.size();
	}

	public SetOfBatches() {

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
				return batch;
			}
		}
		
		// create a new batch
		BatchConnectionRequest newBatch = new BatchConnectionRequest(flow.getSource(), flow.getDestination());
		
		newBatch.add(flow);
		
		this.add(newBatch);
		
		return newBatch;
	}
	
	/**
	 * Remove batch
	 * @param batch
	 * @return
	 */
	public boolean removeBatch(BatchConnectionRequest batch) {
		
		boolean deleted = false;
		
		for(BatchConnectionRequest b: this) 
		{
			if(b.getSource() == batch.getSource() && batch.getDestination() == batch.getDestination()) 
			{
				this.remove(batch);
				deleted = true;
				break;
			}
		}
		
		return deleted;
	}
	
	
	public BatchConnectionRequest getBatch(int source, int destination) {
		
		try 
		{
			Optional<BatchConnectionRequest> optional = this.stream()
															.filter(x -> x.getSource() == source && x.getDestination() == destination)
															.findFirst();
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
	
}
