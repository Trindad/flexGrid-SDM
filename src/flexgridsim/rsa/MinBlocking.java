package flexgridsim.rsa;

import flexgridsim.Modulations;

/**
 * @author pedrom
 *
 */
public class MinBlocking extends ModulationSpectrumGraphRSA {
	/**
	 * implement a function that.
	 *
	 * @param src the source node
	 * @param dst the destination node
	 * @param firstSlot the first slot
	 * @param demand the demand
	 * @param modulationLevel the modulation level
	 * @return the double
	 */
	public double calculateCost(int src, int dst, int firstSlot, int demand, int modulationLevel) {
		return (double)(demand-2*Modulations.getPowerConsumption(modulationLevel) 
				+ (pt.getNodeDegree(dst)*85+150));
	}
}
