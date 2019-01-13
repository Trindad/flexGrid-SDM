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
import burlap.statehashing.simple.SimpleHashableStateFactory;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld;

public class ReinforcementLearning {
	
	
	private SADomain domain;
	private HashableStateFactory hashingFactory;
	private State initialState;
	
	public ReinforcementLearning() {
		hashingFactory = new SimpleHashableStateFactory();
	}

	public void QLearningExecute(){
		
		ReinforcementLearningWorld gen = new ReinforcementLearningWorld();
		gen.setGoalLocation();
		
		SADomain domain = gen.generateDomain();
		State initialState = new GridState(1, 1);
		SimulatedEnvironment env = new SimulatedEnvironment(domain, initialState);
		
	
		LearningAgent agent = new QLearning(domain, 0.99, hashingFactory, 0., 1.);

//		if(env == null) System.out.println("ENV NULL");
		//run learning for 5 episodes
		for(int i = 0; i < 5; i++){
			System.out.println("HERE");
			Episode e = agent.runLearningEpisode(env);
			System.out.println("HERE");
			System.out.println(i + ": " + e.maxTimeStep());

			//reset environment for next learning episode
			env.resetEnvironment();
		}
		
	}	
	
	public void valueIteration(String outputPath){
		
		Planner planner = new ValueIteration(domain, 0.99, hashingFactory, 0.001, 100);
		Policy p = planner.planFromState(initialState);

		PolicyUtils.rollout(p, initialState, domain.getModel()).write(outputPath + "vi");
		
	}
		

}
