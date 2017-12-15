package flexgridsim.rsa;

import java.util.ArrayList;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.WeightedGraph;

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
