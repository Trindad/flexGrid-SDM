package flexgridsim.voncontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Database;
import flexgridsim.PhysicalTopology;

/**
 * 
 * @author trindade
 *
 */
public class Symptom {
	
	public enum SYMPTOM {
		COSTLY,
		NONBALANCED,
		PERFORMANCE
	}
	
	private double []bbr;//pair of adjacent nodes
	private int []availableSlots;//available slots between a pair of adjacent nodes
	private double []closenessCentrality;//pair of nodes
	private int []usedTransponders;//total number of transponders
	private int []availableTransponders;//node
	private double []computing;//node
	private double []xt;//crosstalk between a pair of adjacent nodes 
	private int []numberOfLightpaths; //number of lightpaths mapped
	private double []usedBandwidth;//percentage of bandwidth used in each nodes related to the transponders
	
	public SYMPTOM type;
	
	public ArrayList< ArrayList<Double> > dataset;
	
	public Symptom(PhysicalTopology pt) { 
		
		bbr = new double[pt.getNumLinks()];
		availableSlots = new int[pt.getNumLinks()];
		closenessCentrality = new double[pt.getNumLinks()];
		usedTransponders = new int[pt.getNumLinks()];
		availableTransponders = new int[pt.getNumLinks()]; 
		computing = new double[pt.getNumLinks()]; ;
		xt = new double[pt.getNumLinks()];
		numberOfLightpaths = new int[pt.getNumLinks()]; 		
	}

	public void setDataset(Database db) {
		ArrayList< ArrayList<Double> > matrix = new ArrayList<>();
		
		
		if(type == SYMPTOM.PERFORMANCE) 
		{
			availableSlots = db.slotsAvailablePerLink;
			bbr = db.bbrPerPair;
			xt = db.xtAdjacentNodes;	
			
			for (int i = 0; i < availableSlots.length; i++) {
				ArrayList<Double> row = new ArrayList<>();
				row.add(bbr[i]);
				row.add(xt[i]);
				row.add((double) availableSlots[i]);
				
				matrix.add(row);
			}
		}
		else if(type == SYMPTOM.NONBALANCED)
		{
			availableSlots = db.slotsAvailablePerLink;
			closenessCentrality = db.closenessCentrality;
			numberOfLightpaths = db.numberOfLightpaths;	
			
			for (int i = 0; i < availableSlots.length; i++) {
				ArrayList<Double> row = new ArrayList<>();
				row.add(bbr[i]);
				row.add(closenessCentrality[i]);
				row.add((double) numberOfLightpaths[i]);
				
				matrix.add(row);
			}
		}
		else if(type == SYMPTOM.COSTLY) 
		{
			usedTransponders = db.usedTransponders;
			computing = db.computing;
			usedBandwidth = db.usedBanwidth;
			
			for (int i = 0; i < usedTransponders.length; i++) {
				ArrayList<Double> row = new ArrayList<>();
				row.add(computing[i]);
				row.add((double) usedTransponders[i]);
				row.add((double) usedBandwidth[i]);
				
				matrix.add(row);
			}
		}
		else 
		{
			System.err.println("This problem doesn't exist...");
		}
		
		this.dataset = matrix;
	}
}
