package flexgridsim.voncontroller;

import java.util.ArrayList;

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
		OVERLOADED,
		PERFORMANCE,
		PERFECT
	}
	
	private double []bbr;//pair of adjacent nodes
	private int []availableSlots;//available slots between a pair of adjacent nodes
	private double []closenessCentrality;//pair of nodes
	private int []usedTransponders;//total number of transponders
	private double []computing;//node
	private double []xt;//crosstalk between a pair of adjacent nodes 
	private int []numberOfLightpaths; //number of lightpaths mapped
	private double []usedBandwidth;//percentage of bandwidth used in each nodes related to the transponders
	private PhysicalTopology pt;
	public SYMPTOM type;
	
	public ArrayList< ArrayList<Double> > dataset;
	
	public Symptom(PhysicalTopology pt) { 
		
		bbr = new double[pt.getNumLinks()];
		availableSlots = new int[pt.getNumLinks()];
		closenessCentrality = new double[pt.getNumLinks()];
		usedTransponders = new int[pt.getNumLinks()];
		computing = new double[pt.getNumLinks()]; ;
		xt = new double[pt.getNumLinks()];
		numberOfLightpaths = new int[pt.getNumLinks()]; 	
		
		this.pt = pt;
	}

	public void setDataset(Database db) {
		ArrayList< ArrayList<Double> > matrix = new ArrayList<>();
		
		
		if(type == SYMPTOM.PERFORMANCE) 
		{
			availableSlots = db.slotsAvailablePerLink;
			bbr = db.bbrPerPair;
			xt = db.xtLinks;	
			
			for (int i = 0; i < pt.getNumLinks(); i++) {
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
			
			for (int i = 0; i < pt.getNumLinks(); i++) {
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
			usedBandwidth = db.usedBandwidth;
			
			for (int i = 0; i < pt.getNumNodes(); i++) {
				ArrayList<Double> row = new ArrayList<>();
				row.add(computing[i]);
				row.add((double) usedTransponders[i]);
				row.add((double) usedBandwidth[i]);
				
				matrix.add(row);
			}
			
		}
		else if(type == SYMPTOM.OVERLOADED) {
			
		}
		else 
		{
			System.err.println("This problem doesn't exist..."+type.toString().length());
		}
		
		this.dataset = matrix;
	}
}
