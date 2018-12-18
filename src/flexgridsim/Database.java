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
	
	public int totalTransponders;

	public int []usedTranspoders;
	public int []availableTransponders;
	public Map<Long, Integer> slotsAvailable; 
	public Map<Long, Integer> slotsOccupied; 
	public double []meanCrosstalk;
	public int []distances;
	
	public PhysicalTopology pt;
	public VirtualNetworkEmbedding vne;
	int nVons;//number of active vons
	int flowCount;
	
	public int []modulationFormats;
	public double linkLoad;
	
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

		instance.usedTranspoders = null;
		instance.availableTransponders = null;
		instance.slotsAvailable.clear(); 
		instance.slotsOccupied.clear(); 
		instance.meanCrosstalk = null;
		instance.distances = null;
		
		instance.pt = null;
		instance.vne = null;
		instance.nVons = 0;//number of active vons
		instance.flowCount = 0;
	}
	
	public static void setup(PhysicalTopology pt) {
		Database instance = Database.getInstance();
		
		instance.pt = pt;
		
		instance.availableTransponders = new int[pt.getNumNodes()];
		instance.usedTranspoders = new int[pt.getNumNodes()];
		instance.distances = new int[pt.getNumLinks()];
		instance.meanCrosstalk = new double[pt.getNumLinks()];
		
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
