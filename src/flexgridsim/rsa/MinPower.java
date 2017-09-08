package flexgridsim.rsa;

import flexgridsim.Modulations;

/**
 * @author pedrom
 *
 */
public class MinPower extends ModulationSpectrumGraphRSA {
	
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
		 double cost =  (double)((demand-2)*Modulations.getPowerConsumption(modulationLevel)) + (pt.getNodeDegree(dst)*85+150);
//		double cost = Modulations.getPowerConsumption(modulationLevel);
		return cost;
//		return (double)(numberOfDataSlots*Modulations.getPowerConsumption(modulationLevel))+
//				((numberOfDataSlots+2)/(pt.getNumSlots()))*(PCoxc+200*(nodes.length-1));
	}
}
