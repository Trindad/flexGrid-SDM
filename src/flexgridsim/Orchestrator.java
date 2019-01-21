package flexgridsim;

import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;
import flexgridsim.von.VirtualLink;
import flexgridsim.von.VirtualNode;
import flexgridsim.von.VirtualTopology;
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
		Hooks.init();
	}
	
	public static Orchestrator getInstance() {
		if(instance != null) return instance;
		
		Orchestrator orchestrator = new Orchestrator();
		instance = orchestrator;
		
		return instance;
	}

	public static void reset() {		
		instance.configure = new MAPEConfiguring();
		instance.optimize = new MAPEOptimizing();
		
		Hooks.reset();
		ShapedPlanRF.reset();
		VirtualTopology.resetID();
		VirtualNode.resetID();
		VirtualLink.resetID();
	}
	
	public void run() {
		configure.run();
	}
}
