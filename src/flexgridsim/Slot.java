package flexgridsim;

/**
 * @author pedrom
 *
 */
public class Slot {
	public int getC() {
		return c;
	}


	public void setC(int c) {
		this.c = c;
	}


	public int getS() {
		return s;
	}


	public void setS(int s) {
		this.s = s;
	}


	/**
	 * 
	 */
	public int c;
	/**
	 * 
	 */
	public int s;
	
	/**
	 * 
	 */
	/**
	 * @param x
	 * @param y
	 */
	public Slot(int x, int y) {
		super();
		this.c = x;
		this.s = y;
	}
	
	
	@Override
	public String toString(){
		return "("+c+","+s+")";
	}
	
}
