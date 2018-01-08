package flexgridsim.rsa;

import java.util.ArrayList;
import flexgridsim.Flow;
import flexgridsim.Slot;

/**
 * Paper: Crosstalk-aware cross-core virtual concatenation in spatial division multiplexing elastic optical networks
 * Authors: Zhao and Zhang
 * Published: September 2016
 * 
 * @author trindade
 *
 */
public class CCVCRCSA extends SCVCRCSA {

	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {
		
		if(runRCSA(flow)) return;
		
		constructSuperChannel();
		
		ArrayList<Slot> slotList = new ArrayList<Slot> ();
		int []links = null;
		
		if( establishConnection(links, slotList,0, flow) ) return;
		
		cp.blockFlow(flow.getID());
	}

	/**
	 * Construct a super-channel crossing different cores
	 */
	private void constructSuperChannel() {
		// TODO Auto-generated method stub	
	}
}
