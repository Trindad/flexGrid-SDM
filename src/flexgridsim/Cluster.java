package flexgridsim;

/**
 * 
 * @author trindade
 *
 */
public class Cluster {
	
	//Centroid of x, y and z dimension
	private double x = 0;
	private double y = 0;//in Gb
	private double z = 0;
	private int []cores;
	int nFeatures = 2;
	
	public Cluster(int x, int y) {
	
		this.x = x;
		this.y = y;
	}
	
	public Cluster(double x, double y, double z, int n) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.nFeatures = n;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}
	

	public int []getCores() {
		return cores;
	}

	public void setCores(int []cores) {
//		for(int i = 0; i < cores.length; i++) System.out.print(" "+cores[i]);
//		System.out.println("");
		this.cores = cores;
	}
	
	public int getNumberOfFeatures() {
		return nFeatures;
	}
}
