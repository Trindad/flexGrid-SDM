package flexgridsim;

/**
 * 
 * @author trindade
 *
 */
public class Cluster {
	
	//Centroid of x, y and z dimension
	private int modulationLevel = 1;
	private int rate = 0;//in Gb
	private double duration = 0;
	private int []cores;
	
	public Cluster(int modulationLevel, int rate, double duration) {
	
		this.modulationLevel = modulationLevel;
		this.rate = rate;
		this.duration = duration;
	}
	
	public int getModulationLevel() {
		return modulationLevel;
	}
	
	public void setModulationLevel(int m) {
		this.modulationLevel = m;
	}
	
	public int getRate() {
		return rate;
	}
	
	public void setRate(int r) {
		this.rate = r;
	}
	
	public double getDuration() {
		return duration;
	}
	
	public void setDuration(double d) {
		this.duration = d;
	}

	public int [] getCores() {
		return cores;
	}

	public void setCores(int [] cores) {
		this.cores = cores;
	}
}
