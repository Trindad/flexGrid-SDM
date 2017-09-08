package flexgridsim.rsa;

import org.w3c.dom.Element;

import flexgridsim.PhysicalTopology;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;


/**
 * The Class DefragmentRSA.
 */
public class DefragmentRSA extends SpectrumGraphRSA implements RSA {
	
	/** hops weigh in the cost calculation. */
	private double alpha;
	
	/** . */
	private double beta;
	
	/**
	 * Gets the alpha.
	 *
	 * @return the alpha
	 */
	public double getAlpha() {
		return alpha;
	}

	/**
	 * Sets the alpha.
	 *
	 * @param alpha the new alpha
	 */
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	/**
	 * Gets the beta.
	 *
	 * @return the beta
	 */
	public double getBeta() {
		return beta;
	}

	/**
	 * Sets the beta.
	 *
	 * @param beta the new beta
	 */
	public void setBeta(double beta) {
		this.beta = beta;
	}

	/**
	 * Instantiates a new defragment rsa and set the parameters alpha and beta as 1.
	 */
	public DefragmentRSA() {
		super();
		
	}
	
	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt,
			ControlPlaneForRSA cp, TrafficGenerator traffic) {
		super.simulationInterface(xml, pt, vt, cp, traffic);
		alpha = Double.parseDouble(rsaXml.getAttribute("alpha"));
		beta = Double.parseDouble(rsaXml.getAttribute("beta"));
	}
	
	/* (non-Javadoc)
	 * @see flexgridsim.rsa.SpectrumGraphRSA#calculateCost(flexgridsim.util.MultiGraph, int, int)
	 */
	public double calculateCost(int src, int dst, int firstSlot, int demand) {
		this.spectrumGraph.markEdgesRemoved(src, dst, firstSlot, firstSlot +demand );
		final double freeSlots = this.spectrumGraph.getNumberOfFreeSlots(src, dst);
		final double maxContiguousFreeSlots = this.spectrumGraph.maxNumberOfContiguousEdges(src, dst);
		double frangmentationIndex = (freeSlots-maxContiguousFreeSlots)/freeSlots;
		this.spectrumGraph.restoreRemovedEdges(src, dst, firstSlot, firstSlot + demand-1);
		return ((double) this.alpha + this.beta*(frangmentationIndex));
	}
}

