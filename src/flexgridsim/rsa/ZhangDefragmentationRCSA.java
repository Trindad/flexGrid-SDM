package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;

/**
 * 
 * @author trindade
 *
 */

public class ZhangDefragmentationRCSA extends DefragmentationRCSA{

	@Override
	public void runDefragmentantion() {
		
		ArrayList<Flow> flows = selectConnections();
		
		if(flows.size() >= 1) {
			
		}
	}
	
	/**
	 * MFUSF --> Most Frequency Used Slot First 
	 */
	protected void MFUSFStrategy() {
		
	}
	
	protected ArrayList<Flow> selectConnections() {
		ArrayList<Flow> flows = new ArrayList<Flow>();
		
		
		
		return flows;
	}

}
