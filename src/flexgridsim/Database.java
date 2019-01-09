package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flexgridsim.voncontroller.DatabaseObserver;
import vne.VirtualNetworkEmbedding;

/**
 * Dataset 
 *  
 * @author trindade
 *
 */
public class Database {
	
	
	public int []usedTransponders;
	public int []availableTransponders;
	
	public double totalTransponders;//mean transponders used
	public int totalNumberOfTranspondersAvailable;
	public int totalComputeResource;
	
	public int []slotsAvailablePerLink;
	public Map<Long, Integer> slotsAvailable; 
	public Map<Long, Integer> slotsOccupied; 
	
	public double meanCrosstalk;
	public int []modulationFormats;
	public int []distances;
	
	public PhysicalTopology pt;
	public VirtualNetworkEmbedding vne;
	
	/**
	 * metrics
	 */
	int nVons;//number of active vons
	public int flowCount;//number of flows active
	public double acceptance;
	public double bbr;
	public double []bbrPerPair;
	public double []computing;
	public double cost;
	
	public double linkLoad;
	public double []closenessCentrality;
	public double []xtAdjacentNodes;
	public int[] numberOfLightpaths;
	public double[] usedBandwidth;
	
	private static Database instance;
	private static List<DatabaseObserver> listeners = new ArrayList<DatabaseObserver>();
	
	private Database() {
		
		slotsAvailable = new HashMap<Long, Integer>();
		slotsOccupied = new HashMap<Long, Integer>();
	}
	
	public static Database getInstance() {
		if(instance != null) return instance;
		
		Database db = new Database();
		instance = db;
		
		return instance;
	}
	
	public static void reset() {
		
		instance.totalTransponders = 0;

		instance.usedTransponders = null;
		instance.availableTransponders = null;
		instance.slotsAvailable.clear(); 
		instance.slotsOccupied.clear(); 
		instance.meanCrosstalk = -90;
		instance.distances = null;
		
		instance.slotsAvailablePerLink = null;
		instance.closenessCentrality = null;
		instance.xtAdjacentNodes = null;
		instance.numberOfLightpaths = null;
		instance.usedBandwidth = null;
		
		instance.pt = null;
		instance.vne = null;
		instance.nVons = 0;//number of active vons
		instance.flowCount = 0;
		instance.acceptance = 0;
		instance.bbr = 0;
		instance.cost = 0;
		instance.totalNumberOfTranspondersAvailable = 0;
		instance.computing = null;
		instance.bbrPerPair = null;
		instance.totalComputeResource = 0;
	}
	
	public static void setup(PhysicalTopology pt) {
		Database instance = Database.getInstance();
		
		instance.pt = pt;
		
		instance.closenessCentrality = new double[pt.getNumLinks()];
		instance.slotsAvailablePerLink = new int[pt.getNumLinks()];
		instance.closenessCentrality = new double[pt.getNumLinks()];
		instance.xtAdjacentNodes = new double[pt.getNumLinks()];
		instance.numberOfLightpaths = new int[pt.getNumLinks()];
		instance.usedBandwidth = new double[pt.getNumLinks()];
		instance.computing = new double[pt.getNumNodes()];
		
		instance.availableTransponders = new int[pt.getNumNodes()];
		instance.usedTransponders = new int[pt.getNumNodes()];
		instance.distances = new int[pt.getNumLinks()];
		instance.meanCrosstalk = -90;
		instance.bbrPerPair = new double[pt.getNumLinks()];
		
		for (int i = 0; i < pt.getNumLinks(); i++) {
			instance.slotsAvailable.put((long) i, pt.getNumSlots() * pt.getCores());
			instance.slotsOccupied.put((long) i, 0);
		}
		
		instance.linkLoad = 0;
	}

	public static void dataWasUpdated() {
		for (DatabaseObserver listener : listeners) {
			listener.dataUpdated();
		}
	}

	public static void attach(DatabaseObserver observer) {
		listeners.add(observer);
	}

	public void updateLinkLoad() {
		
		double a = 0;
		for(int i = 0; i < pt.getNumLinks(); i++) {
			
			a += ( (pt.getNumSlots() * pt.getCores()) - pt.getLink(i).getNumFreeSlots());
		}

		a = (double)a / (double)pt.getNumLinks();
		
		double b = 0;
		
		for(int i = 0; i < pt.getNumLinks(); i++) {
			
			double temp = (double)( (pt.getNumSlots() * pt.getCores()) - pt.getLink(i).getNumFreeSlots());
			b += Math.pow(temp - a, 2);
		}
		
		linkLoad = Math.sqrt( ( 1.0 / ( (double)pt.getNumLinks() - 1.0 ) ) * b);
	}
}
