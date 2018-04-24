package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class SlotsPartitionRCSA extends XTFFRCSA{

	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6, 3, 1, 5, 2, 4 , 0));
		int start = 0;
		int end = 0;
		
		if(flow.getRate() <= 50) {
			start = 0;
			end = 50;
		}
		else if(demandInSlots < 400 && flow.getRate() > 50) {
			start = 50;
			end = 150;
		}
		else {
			start = 151;
			end = 220;
		}
		
		
		for(int c = 0; c < priorityCores.size(); c++) {
			
			int i = priorityCores.get(c);
			ArrayList<Slot> temp = new ArrayList<Slot>();
			
			for(int j = start; j < end; j++) {
				
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
		
		end = 220;
		//common area
		for(int c = 0; c < priorityCores.size(); c++) {
			int i = priorityCores.get(c);
			ArrayList<Slot> temp = new ArrayList<Slot>();
			
			if(demandInSlots % 2 == 0) {
				for(int j = end; j < spectrum[i].length; j++) {
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
			else {
				for(int j = spectrum[i].length-1; j >= end; j--) {
					if(spectrum[i][j] == true) 
					{
						temp.add( new Slot(i,j) );
					}
					
					if(temp.size() == demandInSlots) {
						
						if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {

							return temp;
						}
						
						break;
					}
				}	
			}
			
		}
		
//		System.out.println(flow);
		return new ArrayList<Slot>();
	}
}
