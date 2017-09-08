package flexgridsim;

import java.util.ArrayList;

/**
 * This class is based on the WDMLink but it's adapted to RSA operations for
 * contiguous slots allocation.
 * 
 * @author pedrom
 */
public class FlexGridLink {

	private int id;
	private int src;
	private int dst;
	private double delay;
	private int slots;
	//private boolean[][] freeSlots;
	private  boolean[][] reservedSlots;
	private double weight;
	private int[] modulationLevel;
	private int distance;
	private int cores;
	private double[][] SNR;

	/**
	 * Creates a new Fiberlink object.
	 *
	 * @param id
	 *            unique identifier
	 * @param src
	 *            source node
	 * @param dst
	 *            destination node
	 * @param cores
	 *            number of fiber cores
	 * @param delay
	 *            propagation delay (miliseconds)
	 * @param slots
	 *            number of slots available
	 * @param weight
	 *            optional link weight
	 * @param distance
	 *            the distance
	 */
	public FlexGridLink(int id, int src, int dst, int cores, double delay, int slots, double weight, int distance) {
		if (id < 0 || src < 0 || dst < 0 || slots < 1) {
			throw (new IllegalArgumentException());
		} else {
			this.id = id;
			this.src = src;
			this.dst = dst;
			this.delay = delay;
			this.slots = slots;
			this.weight = weight;
			this.cores = cores;
			this.reservedSlots = new boolean[cores][slots];
			this.modulationLevel = new int[slots];
			this.SNR = new double[cores][slots];
			this.distance = distance;
			for (int i = 0; i < cores; i++) {
				this.modulationLevel[i] = 0;
				for (int j = 0; j < slots; j++) {
					this.reservedSlots[i][j] = false;
					//this.freeSlots[i][j] = true;
					this.SNR[i][j]=0;
				}
			}
		}
	}
	
	/**
	 * @param slotList 
	 */
	public void updateCrossTalk(ArrayList<Slot> slotList){
		for (Slot p : slotList) {
			this.SNR[p.x][p.y]=0;
		}
		/*
		 * TODO:implement crosstalk index
		 */
	}

	/**
	 * Gets the number of free slots in the link.
	 * @param modulation modulation level
	 *
	 * @return the free slots for this modulation
	 */
	public boolean[][] getSpectrum(int modulation) {
		boolean[][] freeSlots = new boolean[cores][slots];
		for (int i = 0; i < freeSlots.length; i++) {
			for (int j = 0; j < freeSlots[i].length; j++) {
				if (!reservedSlots[i][j] && SNR[i][j] < Modulations.getSNRThreshold(modulation)){
					freeSlots[i][j] = true;
				} else {
					freeSlots[i][j] = false;
				}
			}
		}
		return freeSlots;
	}

	/**
	 * Gets the number of free slots in the link.
	 * 
	 * @param core
	 *
	 * @return the free slots
	 */
	public boolean[] getSpectrumCore(int core) {
		return reservedSlots[core];
	}

	/**
	 * Retrieves the unique identifier for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's id attribute
	 */
	public int getID() {
		return this.id;
	}

	/**
	 * Retrieves the source node for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's src attribute
	 */
	public int getSource() {
		return this.src;
	}

	/**
	 * Retrieves the destination node for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's dst attribute
	 */
	public int getDestination() {
		return this.dst;
	}

	/**
	 * Retrieves the number of available slots for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's slots attribute
	 */
	public int getSlots() {
		return this.slots;
	}

	/**
	 * Retrieves the weight for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's weight attribute
	 */
	public double getWeight() {
		return this.weight;
	}

	/**
	 * Retrieves the propagation delay for a given FlexGridLink.
	 * 
	 * @return the value of the FlexGridLink's delay attribute
	 */
	public double getDelay() {
		return this.delay;
	}

	/**
	 * Says whether or not a determined set of contiguous slots are available.
	 * @param slotList list of slots
	 * @param modulation modulation
	 * 
	 * 
	 * @return true if the slots are available
	 */
	public Boolean areSlotsAvailable(ArrayList<Slot> slotList, int modulation) {
		for (int i = 0; i < slotList.size(); i++) {
			if (slotList.get(i).x < 0 ) 
				throw (new IllegalArgumentException());
			else if  (slotList.get(i).y >= slots) 
				throw (new IllegalArgumentException());
			else if   (slotList.get(i).x >= cores) 
				throw (new IllegalArgumentException());
			else if   (slotList.get(i).y < 0) 
				throw (new IllegalArgumentException());
				
			else {
				boolean[][] freeSlots = getSpectrum(modulation);
				for (Slot pixel : slotList) {
					if (!freeSlots[pixel.x][pixel.y]) {
						return false;
					} 
				}
			}
		}
		return true;
	}

	/**
	 * Gets the num free slots.
	 *
	 * @return the num free slots
	 */
	public int getNumFreeSlots() {
		int numFreeSlots = 0;
		for (int i = 0; i < cores; i++) {
			for (int j = 0; j < slots; j++) {
				if (reservedSlots[i][j]) {
					numFreeSlots++;
				}
			}
		}
		return numFreeSlots;
	}

	/**
	 * By attributing false to a given slot inside the freSlots array, this
	 * function "reserves" a set of contiguous slots.
	 * @param slotList list of slots
	 * 
	 * 
	 * @return true if operation was successful, or false otherwise
	 */
	public boolean reserveSlots(ArrayList<Slot> slotList) {
		try {
			for (int i = 0; i < slotList.size(); i++) {
				if (slotList.get(i).x < 0 || slotList.get(i).y < 0 || slotList.get(i).x >= cores || slotList.get(i).y >= slots) {
					throw (new IllegalArgumentException());
				}
			}
			for (Slot pixel : slotList) {
				reservedSlots[pixel.x][pixel.y] = true;
			}
			return true;
		} catch (IllegalArgumentException e) {
			System.out.println("Illegal argument for reserveSlots");
			return false;
		}

	}

	/**
	 * By attributing true to a given set of slots inside the freeSlots array,
	 * this function "releases" a set of slots.
	 * @param slotList list of slots
	 * 
	 */
	public void releaseSlots(ArrayList<Slot> slotList) {
		for (int i = 0; i < slotList.size(); i++) {
			if (slotList.get(i).x < 0 || slotList.get(i).y < 0 || slotList.get(i).x >= cores || slotList.get(i).y >= slots) {
				throw (new IllegalArgumentException());
			}
		}
		try{
		for (Slot pixel : slotList) {
			reservedSlots[pixel.x][pixel.y] = false;
		}
		} catch (IllegalArgumentException e) {
			System.out.print("Slots para soltar:");
			for (Slot pixel : slotList) {
				System.out.print(" ("+pixel.x+","+pixel.y+")"+" ");
			}
			System.out.println();
			printSpectrum();
			System.out.println();
		}
	}

	/**
	 * Gets the distance.
	 *
	 * @return the distance
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * Gets the fragmentation ratio, a metric that states the potential of each
	 * free contiguous set of slots by telling the number of traffic calls it
	 * could fit in. then calculating the mean of that
	 *
	 * @param trafficCalls
	 *            the traffic calls
	 * @param slotCapacity
	 *            the slot capacity
	 * @return the fragmentation ratio
	 */
	public double getFragmentationRatio(TrafficInfo[] trafficCalls, double slotCapacity) {
		boolean[][] freeSlots = getSpectrum(0);
		ArrayList<Double> fragmentsPotential = new ArrayList<Double>();
		for (int i = 0; i < freeSlots.length - 1; i++) {
			if (freeSlots[0][i] == true) {
				i++;
				int fragmentSize = 1;
				while (freeSlots[0][i] == true && i < freeSlots.length - 2) {
					fragmentSize++;
					i++;
				}
				double counter = 0;
				for (TrafficInfo call : trafficCalls) {
					if (call.getRate() / slotCapacity >= fragmentSize) {
						counter++;
					}
				}
				fragmentsPotential.add(counter / trafficCalls.length);
			}
		}
		double sum = 0;
		for (Double potential : fragmentsPotential) {
			sum += potential.doubleValue();
		}
		return sum / fragmentsPotential.size();
	}
	
	/**
	 * @return the metric CpS
	 */
	public double getCrossTalkPerSlot(){
		if (cores==1 || getNumFreeSlots() == slots*cores){
			return -1;
		}
		int aoc=0;
		boolean[][] freeSlots = getSpectrum(0);
		for (int i = 0; i < freeSlots.length; i++) {
			for (int j = 0; j < freeSlots[i].length; j++) {
				if (!freeSlots[i][j]){
					if (i==0){
						if (!freeSlots[this.cores-1][j]){
							aoc++;
						}
						if (!freeSlots[1][j]){
							aoc++;
						}
					} else if (i==cores-1){
						if (!freeSlots[0][j] ){
							aoc++;
						}
						if (!freeSlots[cores-2][j]){
							aoc++;
						}
					} else {
						if (!freeSlots[i+1][j]){
							aoc++;
						}
						if (!freeSlots[i-1][j]){
							aoc++;
						}
					}
				}
			}
		}
		//printSpectrum();
		double usedSlots = (slots*cores-getNumFreeSlots());
		return aoc/usedSlots;
	}

	/**
	 * Prints all information related to the FlexGridLink object.
	 * 
	 * @return string containing all the values of the link's parameters.
	 */
	@Override
	public String toString() {
		String link = Long.toString(id) + ": " + Integer.toString(src) + "->" + Integer.toString(dst) + " delay: "
				+ Double.toString(delay) + " slots: " + Integer.toString(slots) + " weight:" + Double.toString(weight);
		return link;
	}

	/**
	 * Print spectrum.
	 */

	public void printSpectrum() {
		System.out.println("----------------------------------------------------");
		boolean[][] freeSlots = getSpectrum(0);
		for (int i = 0; i < freeSlots.length; i++) {
			
			for (int j = 0; j < freeSlots[i].length; j++) {
				
				if (freeSlots[i][j])
					System.out.print(1+"");
				else 
					System.out.print(0+"");
			}	
			System.out.println();
		}
	}

}
