/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

import java.util.Iterator;

/**
 * Simply runs the simulation, as long as there are events
 * scheduled to happen.
 * 
 * @author andred
 */
public class SimulationRunner {

    /**
     * Creates a new SimulationRunner object.
     * 
     * @param cp the the simulation's control plane
     * @param events the simulation's event scheduler
     */
	public SimulationRunner(ControlPlane cp, EventScheduler events) {
        Event event;
        Tracer tr = Tracer.getTracerObject();
        MyStatistics st = MyStatistics.getMyStatisticsObject();
        
        while ((event = events.popEvent()) != null) {
            tr.add(event);
            st.addEvent(event);
            cp.newEvent(event);
        }
    }
	
	public SimulationRunner(VonControlPlane cp, EventScheduler events, boolean dynamic) {
        Event event;
        Tracer tr = Tracer.getTracerObject();
        VonStatistics st = VonStatistics.getVonStatisticsObject();
        
        if(!dynamic)
        {
        	Iterator<Event> es = events.getEvents();
        	
        	while (es.hasNext()) {
        		Event e = es.next();
        		
        		if(e instanceof VonArrivalEvent) {
     	            tr.add(e);
        		}
        		
 	        }
        	
        	cp.newEvents(events);
        	st.addEvents(events);     	
        }
        
        while ((event = events.popEvent()) != null) {
        	
        	if(event instanceof VonArrivalEvent) {
        	
        		continue;
        	}
            tr.add(event);
            st.addEvent(event);
            cp.newEvent(event);
        }
    }
}
