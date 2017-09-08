package flexgridsim.rsa;

import flexgridsim.Modulations;

/**
 * @author pedrom
 *
 */
public class MinPowerUtilization extends ModulationSpectrumGraphRSA {
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
		this.modulationSpectrumGraph[modulationLevel].markEdgesRemoved(src, dst, firstSlot, firstSlot + demand);
		double cost = (double)(demand*Modulations.getPowerConsumption(modulationLevel) 
				+  (pt.getNodeDegree(dst)*85+150));
		this.modulationSpectrumGraph[modulationLevel].restoreRemovedEdges(src, dst, firstSlot, firstSlot + demand);
		return cost;
	}
}
