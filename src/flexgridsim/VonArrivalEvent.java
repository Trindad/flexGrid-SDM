package flexgridsim;
import flexgridsim.von.VirtualTopology;

public class VonArrivalEvent extends Event {
	
	private VirtualTopology von;
	
	 public VonArrivalEvent(double time, VirtualTopology von) {
	    	super(time);
	        this.von = von;
	}
	 
	 public String toString() {
	        return "Arrival: "+von.toString();
	 }

	public VirtualTopology getVon() {
		return von;
	}

	public void setVon(VirtualTopology von) {
		this.von = von;
	}
	    
}
