package flexgridsim;

/**
 * 
 * @author trindade
 *
 */
public class Cluster {
	
	//Centroid of x, y and z dimension
	private int x = 1;
	private int y = 0;//in Gb
	private int z = 0;
	private int []cores;
	int nFeatures = 2;
	
	public Cluster(int x, int y) {
	
		this.x = x;
		this.y = y;
	}
	
	public Cluster(int x, int y, int z, int n) {
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.nFeatures = n;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	

	public int [] getCores() {
		return cores;
	}

	public void setCores(int [] cores) {
		this.cores = cores;
	}
	
	public int getNumberOfFeatures() {
		return nFeatures;
	}
}
