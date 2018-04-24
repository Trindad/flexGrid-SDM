package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

/**
 * First-Core Priority
 * @author trindade
 *
 */
public class XTFFRCSA extends SCVCRCSA {

	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
//		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 5, 4, 3, 2 , 1 , 0));
		for(int c = 0; c < priorityCores.size() ; c++) {
			int i = priorityCores.get(c);
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
