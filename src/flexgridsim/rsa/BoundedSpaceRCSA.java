package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class BoundedSpaceRCSA extends SCVCRCSA{

	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation)  {
		
		ArrayList<Integer> priorityCores = new ArrayList<Integer>();
		if(flow.getRate() == 50) {
			
			priorityCores.add(3);
			priorityCores.add(6);
		}
		else if(demandInSlots == 400) {
			priorityCores.add(2);
			priorityCores.add(5);
			
		}
		else {
			priorityCores.add(1);
			priorityCores.add(4);
			
		}
		priorityCores.add(0);//the lowest priority 
		
//		boolean [][]spectrum = bitMapAll(links);
		
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
