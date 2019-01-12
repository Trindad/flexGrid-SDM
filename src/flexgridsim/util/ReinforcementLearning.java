package flexgridsim.util;


import burlap.behavior.policy.Policy;
import burlap.behavior.policy.PolicyUtils;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.Planner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.SimulatedEnvironment;
import burlap.statehashing.HashableStateFactory;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld;

public class ReinforcementLearning {
	
	
	private SADomain domain;
	private HashableStateFactory hashingFactory;
	private State initialState;
	private Environment env;
	
	public ReinforcementLearning() {
		
	}

	public void QLearningExecute(String outputPath){
		
		ReinforcementLearningWorld gen = new ReinforcementLearningWorld();
		gen.setGoalLocation(10, 10);
		SADomain domain = gen.generateDomain();
		State initialState = new GridState(0, 0);
		SimulatedEnvironment env = new SimulatedEnvironment(domain, initialState);
		
	}	
	
	public void valueIteration(String outputPath){
		
		Planner planner = new ValueIteration(domain, 0.99, hashingFactory, 0.001, 100);
		Policy p = planner.planFromState(initialState);

		PolicyUtils.rollout(p, initialState, domain.getModel()).write(outputPath + "vi");
		
	}

}
