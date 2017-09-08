package flexgridsim;

/**
 * @author pedrom
 *
 */
public class Slot {
	/**
	 * 
	 */
	public int x;
	/**
	 * 
	 */
	public int y;
	
	/**
	 * 
	 */
	/**
	 * @param x
	 * @param y
	 */
	public Slot(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	@Override
	public String toString(){
		return "("+x+","+y+")";
	}
}
