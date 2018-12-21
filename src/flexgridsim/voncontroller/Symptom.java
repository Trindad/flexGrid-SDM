package flexgridsim.voncontroller;

/**
 * 
 * @author trindade
 *
 */
public class Symptom {
	
	public enum SYMPTOM {
		LOAD_BALANCE,
		RESOURCE_OPTIMIZATION
	}
	
	private double linkLoad;
	private double bbr;
	private double acceptance;
	private int transponders;//total number of transponders
	private int availableTransponders;
	private double cost;
	
	public SYMPTOM type;
	
	public Symptom( double linkLoad, double bbr, double acceptance, 
			int transponders, int availableTransponders, double cost) {
		
		this.linkLoad = linkLoad;
		this.bbr = bbr;
		this.acceptance = acceptance;
		this.transponders = transponders;
		this.cost = cost;
		this.availableTransponders = availableTransponders;
	}


	public double getBandwidthBlockingRatio() {
		return bbr;
	}


	public double getLinkLoad() {
		return linkLoad;
	}


	public double getCost() {
		return cost;
	}


	public double getAcceptance() {
		return acceptance;
	}


	public void setAcceptance(double acceptance) {
		this.acceptance = acceptance;
	}


	public int getnTranspondersActived() {
		return transponders;
	}


	public int getAvailableTransponders() {
		return availableTransponders;
	}
}
