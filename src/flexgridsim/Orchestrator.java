package flexgridsim;

import flexgridsim.voncontroller.MAPEConfiguring;
import flexgridsim.voncontroller.MAPEOptimizing;

/**
 * 
 * @author trindade
 *
 */
public class Orchestrator {

	private static Orchestrator instance;
	
	public MAPEConfiguring configure;
	public MAPEOptimizing optimize;
	
	private Orchestrator() {
		configure = new MAPEConfiguring();
		optimize = new MAPEOptimizing();
	}
	
	public static Orchestrator getInstance() {
		if(instance != null) return instance;
		
		Orchestrator orchestrator = new Orchestrator();
		instance = orchestrator;
		
		return instance;
	}

	public static void reset() {
		
		instance.configure = null;
		instance.optimize = null;
	}
	
	public void run() {
		configure.run();
	}
}