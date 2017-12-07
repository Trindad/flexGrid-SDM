package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.util.KShortestPaths;

/**
 * 
 * @author trindade
 *
 */
public class DefragmentationRCSA extends SCVCRCSA{

	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 3);
		
		if(kPaths.length >= 1)
		{
			boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
			
			int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());
			
			for (int k = 0; k < kPaths.length; k++) {
				
				spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
				
				int[] links = new int[kPaths[k].length - 1];
				
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
					bitMap(pt.getLink(kPaths[k][j], kPaths[k][j+1]).getSpectrum(), spectrum, spectrum);
				}
				
				ArrayList<Slot> slotList = fitConnection(spectrum, links, demandInSlots, 0);
				
		    	if(establishConnection(links, slotList, 0, flow)) {
					return;
				}
			}
		}
		
		cp.blockFlow(flow.getID());
	}
}
