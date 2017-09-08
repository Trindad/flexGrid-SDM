package flexgridsim.rsa;

import flexgridsim.Modulations;

/**
 * The Class EnergyUtilization.
 */
public class EnergyUtilization extends ModulationSpectrumGraphRSA {
	
	public double calculateCost(int src, int dst, int firstSlot, int demand, int modulationLevel) {
		return  (double)((demand-2)*Modulations.getPowerConsumption(modulationLevel)) + pt.getNodeDegree(dst)*85+200;
	}
}
