package flexgridsim;

public class VonStatistics {

	private static VonStatistics singletonObject;
	private int vonAcceptedRate = 0;
	private int vonBlockedRate = 0;
	
	public static synchronized VonStatistics getVonStatisticsObject() {
        if (singletonObject == null) {
            singletonObject = new VonStatistics();
        }
        return singletonObject;
    }
	
	 public void addEvent(Event event) {
		 
	 }
	 
	 public void blocked() {
		 
	 }
	 
	 public void accepted(VirtualTopology von) {
		 
	 }

	public void addEvents(EventScheduler events) {
		// TODO Auto-generated method stub
		
	}
}
