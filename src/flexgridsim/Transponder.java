package flexgridsim;

/**
 * 
 * @author trindade
 *
 */

public class Transponder {
	
	int idLink;
	int idFlow;
	int id;
	double capacity;
	boolean status = false;
	
	public int getIdLink() {
		return idLink;
	}
	public void setIdLink(int idLink) {
		this.idLink = idLink;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public double getCapacity() {
		return capacity;
	}
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	public boolean isStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}

}
