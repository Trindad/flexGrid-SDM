package flexgridsim;
import flexgridsim.von.VirtualTopology;

public class VonDepartureEvent extends Event {

	private long ID;
	private VirtualTopology von;
	
	
	 public VonDepartureEvent(double time, long id, VirtualTopology von) {
	    	super(time);
	        this.setID(id);
	        this.setVon(von);
	    }


	public long getID() {
		return ID;
	}


	public void setID(long iD) {
		ID = iD;
	}


	public VirtualTopology getVon() {
		return von;
	}


	public void setVon(VirtualTopology von) {
		this.von = von;
	}
	
    public String toString() {
        return "Departure: "+Long.toString(ID);
	}
}
