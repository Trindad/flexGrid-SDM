package flexgridsim.rsa;

import org.w3c.dom.Element;

import flexgridsim.PhysicalTopology;
import flexgridsim.TrafficGenerator;
import flexgridsim.TrafficInfo;
import flexgridsim.VirtualTopology;


/**
 * The Class DefragmentRSA.
 */
public class LocalDefrag extends SpectrumGraphRSA implements RSA {
	private double leftWeight;
	private double rightWeight;
	/**
	 * Instantiates a new defragment rsa and set the parameters alpha and beta as 1.
	 */
	public LocalDefrag() {
		super();
	}
	
	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt,
			ControlPlaneForRSA cp, TrafficGenerator traffic) {
		super.simulationInterface(xml, pt, vt, cp, traffic);
		leftWeight = Double.parseDouble(rsaXml.getAttribute("leftWeight"));
		rightWeight = Double.parseDouble(rsaXml.getAttribute("rightWeight"));
	}
	/* (non-Javadoc)
	 * @see flexgridsim.rsa.SpectrumGraphRSA#calculateCost(flexgridsim.util.MultiGraph, int, int)
	 */
	public double calculateCost(int src, int dst, int firstSlot, int demand) {
		int i = firstSlot - 1;
		int freeSlotsCountLeft = 0;
		if (i >= 0){
			while (this.spectrumGraph.hasEdge(src, dst, i) && i >= 1){
				i--;
				freeSlotsCountLeft++;
			}
		}
		int j = firstSlot + demand;
		int freeSlotsCountRight = 0;
		if (j < this.spectrumGraph.size()){
			while (this.spectrumGraph.hasEdge(src, dst, j) && j < this.spectrumGraph.size()-1){
				j++;
				freeSlotsCountRight++;
			}
		}
		
		
		int max = 0;
		for (TrafficInfo t : this.spectrumGraph.getTrafficInfo()) {
			if (t.getRate() > max){
				max = t.getRate();
			}
		}
		double result = 1;
		if (freeSlotsCountLeft < Math.ceil((double)max/this.pt.getSlotCapacity()) && freeSlotsCountLeft > 0){
			result += leftWeight;
		}
		if (freeSlotsCountRight < Math.ceil((double)max/this.pt.getSlotCapacity()) && freeSlotsCountRight > 0){
			result += rightWeight;
		}
		return result;
	}
}

