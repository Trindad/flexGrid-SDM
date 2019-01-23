package flexgridsim.util;

import java.util.ArrayList;

import burlap.behavior.policy.EpsilonGreedy;
import burlap.behavior.policy.Policy;
import burlap.behavior.policy.PolicyUtils;
import burlap.behavior.singleagent.Episode;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.Planner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.environment.SimulatedEnvironment;
import burlap.statehashing.HashableStateFactory;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld;

public class ReinforcementLearning {
	
	
	private SADomain domain;
	private HashableStateFactory hashingFactory;
	private ArrayList<State> initialStates;
	private QLearning agent;
	
	private int n = 5;
	
	public ReinforcementLearning() {
		hashingFactory = new SimpleHashableStateFactory();
	}
	
	public void relearn() {
		agent.resetSolver();
		
		for(State initialState : initialStates) {
			SimulatedEnvironment env = new SimulatedEnvironment(domain, initialState);

			//run learning for 5 episodes
			for(int i = 0; i < 100; i++) {
				agent.runLearningEpisode(env);
				
//				System.out.println(i + ": " + e.maxTimeStep());

				//reset environment for next learning episode
				env.resetEnvironment();
			}
			
		}
	}

	public void QLearningExecute() {
		
		ReinforcementLearningWorld gen = new ReinforcementLearningWorld();
		gen.setGoalLocation();
		gen.setParent(this);
		
		domain = gen.generateDomain();
		initialStates = new ArrayList<State>(){{
				add(new GridState(1, 1));
				add(new GridState(4, 1));
				add(new GridState(8, 1));
				add(new GridState(10, 1));
				add(new GridState(14, 1));
				add(new GridState(17, 1));
				add(new GridState(19, 1));
				add(new GridState(22, 1));
				add(new GridState(26, 1));
				add(new GridState(28, 1));
				add(new GridState(30, 1));
				add(new GridState(33, 1));
		}};
		
		agent = new QLearning(domain, 0.99, hashingFactory, 0., 1.);
		
		for(State initialState : initialStates) {
			SimulatedEnvironment env = new SimulatedEnvironment(domain, initialState);

			//run learning for 5 episodes
			for(int i = 0; i < 100; i++) {
				agent.runLearningEpisode(env);
				
//				System.out.println(i + ": " + e.maxTimeStep());

				//reset environment for next learning episode
				env.resetEnvironment();
			}
			
		}
	
	}	
	
	public ArrayList<String> valueIteration(String problem){
		ArrayList<String> actions = new ArrayList<String>();
		
		int x = 1, y = 0;
		
		if (problem.equals("performance_high")) {
			y = 10;
		} else if (problem.equals("performance_medium")) {
			y = 14;
		} else if (problem.equals("performance_low")) {
			y = 17;
		} else if (problem.equals("costly_high")) {
			y = 19;
		} else if (problem.equals("costly_medium")) {
			y = 22;
		} else if (problem.equals("costly_low")) {
			y = 26;
		} else if (problem.equals("overloaded_high")) {
			y = 28;
		} else if (problem.equals("overloaded_medium")) {
			y = 30;
		} else if (problem.equals("overloaded_low")) {
			y = 33;
		} else if (problem.equals("nonbalanced_high")) {
			y = 1;
		} else if (problem.equals("nonbalanced_medium")) {
			y = 4;
		} else if (problem.equals("nonbalanced_low")) {
			y = 8;
		}
		
		State s = new GridState(y,x);
		
//		Planner planner = new ValueIteration(domain, 0.99, hashingFactory, 0.001, 100);
//		Policy p = planner.planFromState(s);
		Policy p = agent.planFromState(s);
		
		Episode ea  = PolicyUtils.rollout(p, s, domain.getModel(), 150);
		
		for (State a : ea.stateSequence) {
			GridState gs = (GridState) a;
			
//			System.out.println("State " + gs.x + ", " + gs.y);
			
			String str = ReinforcementLearningWorld.getRelevantActionFromState(gs.x, gs.y);
			
			if (!str.equals("nothing")) {
				actions.add(str);
			}
		}
		
		return actions;
	}
		

}
