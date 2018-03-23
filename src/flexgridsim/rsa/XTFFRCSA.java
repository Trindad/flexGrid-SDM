package flexgridsim.rsa;

import java.util.ArrayList;


import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

/**
 * First-Core Priority
 * @author silvana
 *
 */
public class XTFFRCSA extends SCVCRCSA {

	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
	
		for(int i = (spectrum.length-1); i >= 0 ; i--) {
			
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int j = 0; j < spectrum[i].length; j++) {	
				
				if(spectrum[i][j] == true) 
				{
					temp.add( new Slot(i,j) );
				}
				else {
					
					temp.clear();
					if(Math.abs(spectrum[i].length-j) < demandInSlots) break;
				}
				
				if(temp.size() == demandInSlots) {
					
					if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {

						return temp;
					}
					
					break;
				}
			}
		}
	    
		return new ArrayList<Slot>();
	}
}
