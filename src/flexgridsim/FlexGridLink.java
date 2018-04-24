package flexgridsim;

import java.util.ArrayList;
import java.util.LinkedList;

import flexgridsim.util.Decibel;

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
	private  boolean[][] reservedSlots;
	private double weight;
	private int[] modulationLevel;
	private int distance;
	private int cores;
	private double[][] noise;
	private double BFR; //Bandwidth Fragmentation Ratio
	private XTAwareResourceAllocation xt; //considering inter-core cross-talk 
	private int reserved;
	private boolean isBlocked = false;
	private int slotsAvailable = 0;
	private double XT = 0;
//	private double currentXT = 0;

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
			this.slotsAvailable = (this.slots * this.cores);
			this.reservedSlots = new boolean[cores][slots];
			this.modulationLevel = new int[slots];
			this.noise = new double[cores][slots];
			this.distance = distance;
			for (int i = 0; i < cores; i++) {
				this.modulationLevel[i] = 0;
				for (int j = 0; j < slots; j++) {
					this.reservedSlots[i][j] = false;
					this.noise[i][j]=-100;
				}
			}
			
			xt = new XTAwareResourceAllocation(this.cores, this.cores, this.slots);
		}
	}
	
	public int getCores() {
		return cores;
	}

	/**
	 * @param slotList 
	 * @param modulation 
	 */
	public void updateNoise(ArrayList<Slot> slotList, int modulation){
		for (Slot s : slotList) {
			this.noise[s.c][s.s] = Decibel.add( ModulationsMuticore.interCoreXT(modulation), ModulationsMuticore.inBandXT[modulation]);
		}
	}
	
	public void resetNoises() {
		for (int i = 0; i < cores; i++) {
			this.modulationLevel[i] = 0;
			for (int j = 0; j < slots; j++) {
				this.reservedSlots[i][j] = false;
				this.noise[i][j]=-100;
			}
		}
	}
	
	/**
	 * @param slot
	 * @return crosstalk in the slot
	 */
	public double getNoise(Slot slot) {
		return noise[slot.c][slot.s];
	}
	
	/**
	 * 
	 * @return crosstalk
	 */
	public double getXT(int core, int slot, double db) {

		return this.xt.getXT(core, slot);
	}
	
	public void resetCrosstalk() {
		for(int k = 0; k < this.cores; k++) {
			for(int i = 0; i < this.slots; i++) {
				this.xt.resetCrosstalk(k, i);
			}
		}
		
		XT = 0;
	}
	
	public void resetCrosstalk(ArrayList<Slot> slotList) {
		
		for(Slot s: slotList) {
			this.xt.resetCrosstalk(s.c, s.s);
		}
	}
	
	/**
	 * Update the XT 
	 */
	public double updateCrosstalk(ArrayList<Slot> slotList, double dbLimited) {
	
		double sumXT = 0;
		for(Slot s: slotList) {
				
			double n = 0;
			for(Integer c: this.xt.getAdjacentsCores(s.c) ) {
					
				if(reservedSlots[c][s.s]) {
					n++;
				}
			}
			
			sumXT += this.xt.interCoreCrosstalk(s.c, s.s, n, this.distance);
			this.xt.setLimitDB(s.c, s.s, dbLimited);
		}
		
//		System.out.println("penalty: "+(-10.0f * Math.log(1- Math.sqrt(sumXT))) +" :: "+(-10.0f * Math.log(1- sumXT))+ " xt: "+(10.0f * Math.log10(sumXT)/Math.log10(10)) );
		
		return sumXT;
	}
	
	private double convertToDB(double p) {
		return 10.0f * Math.log10(p)/Math.log10(10);
//		return ( 10.0f * Math.log10(p/10.0) );
	}
	
	public void updateCrosstalk() {
		
		double xt = 0, xti = 0;
		int nXT = 0;
		for(int i = 0; i < this.cores; i++) {
			for(int j = 0; j < this.slots; j++) {
				double n = 0;
				if(reservedSlots[i][j]) {
					for(Integer c: this.xt.getAdjacentsCores(i) ) {
						if(reservedSlots[c][j]) {
							n++;
						}
					}
				}
				
				xti = this.xt.interCoreCrosstalk(i, j, n, this.distance);
				
				if(xti > 0) {
//					System.out.println(xti + " "+n+ " c: "+i);
					nXT++;
					xt = xt + xti;
				}
			}
		}
		
		if( nXT >= 1) {
			xt = convertToDB(xt);//db
			if(XT > xt) 
			{
				XT = xt;
			}
		}
	}
	
	public double getSumOfInterCoreCrosstalk() {
		return XT;
	}

	
	public void updateCrosstalk(ArrayList<Slot> slotList) {
		
		for(Slot s: slotList) {
				
			double n = 0;
			for(Integer c: this.xt.getAdjacentsCores(s.c) ) {
					
				if(reservedSlots[c][s.s]) {
					n++;
					
				}
			}
			
			this.xt.interCoreCrosstalk(s.c, s.s,  n, this.distance);
			this.xt.setLimitDB(s.c, s.s, 0);
		}
	}

	/**
	 * Gets the number of free slots in the link.
	 * @param modulation modulation level
	 *
	 * @return the free slots for this modulation
	 */
	public boolean[][] getSpectrum() {
		boolean[][] freeSlots = new boolean[cores][slots];
		for (int i = 0; i < cores; i++) {
			for (int j = 0; j < slots; j++) {
				if (!reservedSlots[i][j]){
					freeSlots[i][j] = true;
				} else {
					freeSlots[i][j] = false;
				}
			}
		}
		
		return freeSlots;
	}
	
	/**
	 * reset the slots 
	 * @return
	 */
	public void resetSpectrum() {
		
		for (int i = 0; i < cores; i++) {
			this.modulationLevel[i] = 0;
			for (int j = 0; j < slots; j++) {
				if(this.reservedSlots[i][j]) {
					this.slotsAvailable++;
				}
				this.reservedSlots[i][j] = false;
				
				this.noise[i][j]=-100;
			}
		}
		
		XT = 0;
	}
	
	/**
	 * Gets the number of free slots in the link.
	 * @param modulation modulation level
	 * @param power power of transmission
	 *
	 * @return the free slots for this modulation
	 */
	public boolean[][] getAllocableSpectrum(int modulation, double power) {
		boolean[][] freeSlots = new boolean[cores][slots];
		for (int i = 0; i < freeSlots.length; i++) {
			for (int j = 0; j < freeSlots[i].length; j++) {
				double SNR = Decibel.subtract(power, noise[i][j]);
				if (!reservedSlots[i][j] 
					&& SNR >= ModulationsMuticore.getSNRThreshold(modulation)//check if allocation is possible
					&& !allocationAffectsNeighbors(i, j, modulation, power)) { //check if allocation will disrupt other connections
					
					freeSlots[i][j] = true;
				} else {
					freeSlots[i][j] = false;
				}
			}
		}
		
		return freeSlots;
	}

	/**
	 * @param i index of spectrum (core)
	 * @param j index of spectrum (slot)
	 * @param modulation
	 * @param power
	 * @return true if the allocation affects neighbors; false otherwise
	 */
	public boolean allocationAffectsNeighbors(int i, int j, int modulation, double power) {
		for (Slot s : getNeighborSlotsInUse(i, j)) {
			double totalNoise = Decibel.add(noise[s.c][s.s], ModulationsMuticore.interCoreXT(modulation)); 
			if (Decibel.subtract(power,totalNoise) < ModulationsMuticore.getSNRThreshold(modulation)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param i index of spectrum (core)
	 * @param j index of spectrum (slot)
	 * @return a list of neighbor slots
	 */
	public ArrayList<Slot> getNeighborSlotsInUse(int i, int j){
		ArrayList<Slot> neighbors = new ArrayList<Slot>();
		if (i==0){
			if (reservedSlots[this.cores-1][j]){
				neighbors.add(new Slot(this.cores-1, j));
			}
			if (reservedSlots[1][j]){
				neighbors.add(new Slot(1, j));
			}
		} else if (i==cores-1){
			if (reservedSlots[0][j] ){
				neighbors.add(new Slot(0, j));
			}
			if (reservedSlots[cores-2][j]){
				neighbors.add(new Slot(this.cores-2, j));
			}
		} else {
			if (reservedSlots[i+1][j]){
				neighbors.add(new Slot(i+1, j));
			}
			if (reservedSlots[i-1][j]){
				neighbors.add(new Slot(i-1, j));
			}
		}
		return neighbors;
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
			if (slotList.get(i).c < 0 ) 
				throw (new IllegalArgumentException());
			else if  (slotList.get(i).s >= slots) 
				throw (new IllegalArgumentException());
			else if   (slotList.get(i).c >= cores) 
				throw (new IllegalArgumentException());
			else if   (slotList.get(i).s < 0) 
				throw (new IllegalArgumentException());
		}
		
		boolean[][] freeSlots = getSpectrum();
		
		for (Slot slot : slotList) {
			if (!freeSlots[slot.c][slot.s]) {
				return false;
			} 
		}
		
		return true;
	}

	/**
	 * Gets the number of free slots.
	 *
	 * @return the num free slots
	 */
	public int getNumFreeSlots() {
		int numFreeSlots = 0;
		for (int i = 0; i < cores; i++) {
			for (int j = 0; j < slots; j++) {
				if (!reservedSlots[i][j]) {
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
				if (slotList.get(i).c < 0 || slotList.get(i).s < 0 || slotList.get(i).c >= cores || slotList.get(i).s >= slots) {
					throw (new IllegalArgumentException());
				}
			}
			for (Slot slot: slotList) {
				if (!reservedSlots[slot.c][slot.s]) {
					reservedSlots[slot.c][slot.s] = true;
					this.slotsAvailable--;
				}
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
			if (slotList.get(i).c < 0 || slotList.get(i).s < 0 || slotList.get(i).c >= cores || slotList.get(i).s >= slots) {
				throw (new IllegalArgumentException());
			}
		}
		for (Slot pixel : slotList) {
			if (reservedSlots[pixel.c][pixel.s]) {
				reservedSlots[pixel.c][pixel.s] = false;
				this.slotsAvailable++;
			}
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
		boolean[][] freeSlots = getSpectrum();
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
		boolean[][] freeSlots = getSpectrum();
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
		boolean[][] freeSlots = getSpectrum();
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
	
	/**
	 * 
	 * @return
	 */
	public boolean bandwidthDefragmentationIsNecessary() {
		
		BFR = 1.0f - (maxBlock() / ( (this.cores * this.slots) - this.reserved)) ;
		
		return false;
	}

	private int maxBlock() {
		
		int max = 0, last = 0;
		last = max;
		
		for(int i = 0; i < this.cores; i++) {
			for(int j = 0; j < this.slots; j++) {
				if(!reservedSlots[i][j]) {
					max++;
				}
				else {
					max = 0;
				}
			}
			
			if(max > last) {
				last = max;
			}
			
			max = 0;
		}
		
		return last;
	}

	public double getBFR() {
		return BFR;
	}

	public void setBFR(double n) {
		BFR = n;
	}

	public boolean isBlocked() {
		return isBlocked;
	}

	public void setBlocked(boolean isBlocked) {
		this.isBlocked = isBlocked;
	}

	public int getSlotsAvailable() {
		
		return this.slotsAvailable;
	}

	public LinkedList<Integer>getXTAdjacents(int c, int s) {
		
		return xt.getAdjacentsCores(c);
	}

	public double getNewXT(Slot s, int count) {
		
		int n = 0;
		
		for(Integer c: this.xt.getAdjacentsCores(s.c) ) {
				
			if(reservedSlots[c][s.s]) {
				n++;
			}
		}
		
		return xt.interCoreCrosstalk(s.c, s.s, (n + count), distance);
	}

	public int getInterCoreCrosstalkInAdjacent(Slot s) {
		
		int n = 0;
		for(Integer c: this.xt.getAdjacentsCores(s.c) ) {
			
			if(reservedSlots[c][s.s]) {
				n++;	
			}
		}
		
		return n;
	}

	public void printXTMatrix() {
		xt.printXTMatrix();
	}

	public LinkedList<Integer> getAdjacentCores(int c) {

		return xt.getAdjacentsCores(c);
	}

}
