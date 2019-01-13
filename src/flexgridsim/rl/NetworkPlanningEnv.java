package flexgridsim.rl;

import java.util.List;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.mdp.singleagent.environment.extensions.EnvironmentObserver;
import burlap.mdp.singleagent.environment.extensions.EnvironmentServerInterface;

public class NetworkPlanningEnv implements Environment, EnvironmentServerInterface{

	private boolean terminated;
	private double reward;
	
	private List<EnvironmentObserver> observers;
	private State currentObservationState;
	

	
	public NetworkPlanningEnv() {
		
		terminated = false;
		resetEnvironment();
	}
	
	@Override
	public State currentObservation() {
		
		return currentObservationState;
	}

	@Override
	public EnvironmentOutcome executeAction(Action action) {
		
		
		return null;
	}

	@Override
	public boolean isInTerminalState() {
		
		return terminated;
	}

	@Override
	public double lastReward() {
		return reward;
	}

	@Override
	public void resetEnvironment() {
		
	}

	@Override
	public void addObservers(EnvironmentObserver... observers) {
		for(EnvironmentObserver o : observers) {
			this.observers().add(o);
		}
	}

	@Override
	public void clearAllObservers() {
		this.clearAllObservers();
	}

	@Override
	public List<EnvironmentObserver> observers() {
		
		return this.observers;
	}

	@Override
	public void removeObservers(EnvironmentObserver... observers) {
		for (EnvironmentObserver o : observers) {
			this.removeObservers(o);
		}
	}

	
}
