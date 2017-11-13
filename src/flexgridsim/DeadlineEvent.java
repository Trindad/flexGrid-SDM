package flexgridsim;
import flexgridsim.BatchConnectionRequest;

/**
 * 
 * @author trindade
 *
 */
public class DeadlineEvent  extends Event {

	protected BatchConnectionRequest batch;
	private long ID;
	private long indexOfEvent;
	private static long numberOfEvents = 0;
	
	public DeadlineEvent(double time, BatchConnectionRequest batch) {
		super(time);
		
		numberOfEvents++;
		this.ID = numberOfEvents;
		this.batch = batch;
	}
	
	public DeadlineEvent(double time) {
		super(time);
	}

	/**
	 * Gets the batch.
	 *
	 * @return the batch
	 */
	public BatchConnectionRequest getBatch() {
		return batch;
	}


	/**
	 * Set of batch
	 * @param batch
	 */
	public void setBatch(BatchConnectionRequest batch) {
		this.batch = batch;
	}

	public long getIndexOfEvent() {
		return indexOfEvent;
	}

	/**
	 * set of number of events
	 */
	public void setIndexOfEvent() {
		this.indexOfEvent ++;
	}
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public long getID(){
		return this.ID;
	}
}
