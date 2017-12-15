package flexgridsim;

/**
 * @author pedrom, trindade
 *
 */
public class ModulationsMuticore {
		/**
		 * The Class Modulations.
		 */
		public static final int numberOfModulations = 6;
		
		/**
		 * 
		 */
		public static final int numberOfSimbols[] = {1,2,4,6};
		
		/**
		 * 
		 */
		public static final double subcarriersCapacity[] = {12.5,25,37.5,50,62.5,75};
		
		/** The Constant distance. */
		public static final int maxDistance[] = {4000,2000,1000,500,250,125};
		/**
		 * SNR threshold for the correct 
		 */
		public static final double SNR_THRESHOLD[] = {4.2,7.2,13.9,19.8};
		
		
		/**
		 * 
		 */
		public static final double inBandXT[] = {-14,-18.5,-21,-25,-27,-34};
		/**
		 * Number of modulations.
		 *
		 * @return the int
		 */
		public static int numberOfModulations(){
			return numberOfModulations;
		}
		/**
		 * Gets the bandwidth.
		 *
		 * @param modulationLevel the modulation level
		 * @param slotCapacity 
		 * @return the bandwidth which modulation level can transmit
		 */
		public static double getBandwidth(int modulationLevel, int slotCapacity){
			return numberOfSimbols[modulationLevel]*slotCapacity;
		}
		
		
		/**
		 * Gets the max distance.
		 *
		 * @param modulationLevel the modulation level
		 * @return the max distance
		 */
		public static int getMaxDistance(int modulationLevel){
			
			if (modulationLevel >= 0 && modulationLevel <= 5){
				return maxDistance[modulationLevel];
			} else {
				return maxDistance[0];
			}
		}
		
		/**
		 * Gets the max distance.
		 *
		 * @param modulationLevel the modulation level
		 * @return the max distance
		 */
		public static double getSNRThreshold(int modulationLevel){
			if (modulationLevel >= 0 && modulationLevel <= 3){
				return SNR_THRESHOLD[modulationLevel];
			} else {
				return SNR_THRESHOLD[0];
			}
		}
		
		/**
		 * Gets the modulation by distance.
		 *
		 * @param givendistance the distance
		 * @return the modulation by distance
		 */
		public static int getModulationByDistance(int givendistance){
			
			int i = numberOfModulations-1;
			
			while (givendistance <= maxDistance[i] && i >= 0) {
				
				i--;
			}
			System.out.println(" "+givendistance+" "+maxDistance[i]+" "+i);
			return i;
		}
		
		/**
		 * @param cores
		 * @return worst aggregate inter core crosstalk crosstalk 
		 */
		public static double interCoreXT(int cores) {
			if (cores <= 7) {
				return -84;
			} else if (cores > 7 && cores <=12) {
				return -61.9;
			} else if (cores >12) {
				return -54.8;
			} else {
				return 1000;
			}
		}
		
}
