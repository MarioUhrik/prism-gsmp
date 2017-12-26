//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit.rewards;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import explicit.GSMP;
import explicit.Model;
import explicit.Product;

/**
 * Sparse-state storage of rewards for an GSMP.
 * 
 * For a GSMP, transition rewards have an additional dimension - event they belong to.
 * Hence, to identify a GSMP transition, we need the source state, the destination state, and the event.
 */
public class GSMPRewardsSimple implements GSMPRewards
{
	/**
	 * Indices of states with rewards mapped onto them.
	 */
	private Map<Integer, Double> stateRewards;
	/**
	 * The First map   contains Second Maps mapped onto eventNames.
	 * The Second maps contain  Third maps  mapped onto source state indices.
	 * The Third maps  contain  rewards     mapped onto destination state indices.
	 * <br>
	 * One can imagine this as a subset of a 3D array of size |Events|x|States|x|States|
	 * containing only the non-zero transition rewards.
	 */
	private Map<String,Map<Integer,Map<Integer, Double>>> transitionRewards; 
	// TODO MAJO - is this the best possible data structure?
		//Why maps - fast access, which is what we want for model checking
			//HashMap - O(1) read/insertion, but the hash tables have recursive(3x) memory redundancy
			//TreeMap - O(log n) read/insertion, but no memory redundancy at all
		//Why not just ArrayList - awkward indexing for events, overwhelming memory redundancy
	    	//(always O(|E|*|S|*|S|) space complexity regardless of rewards)

	public GSMPRewardsSimple() {
		stateRewards = new HashMap<Integer, Double>();
		transitionRewards = new HashMap<String,Map<Integer,Map<Integer, Double>>>();
	}
	
	@Override
	public double getStateReward(int s) {
		Double reward = stateRewards.get(s);
		if (reward == null) {
			return 0.0;
		} else {
			return reward;
		}
	}
	
	public Map<Integer, Double> getStateRewards() {
		return stateRewards;
	}
	
	/**
	 * Assign reward {@code r} to state {@code s}.
	 */
	public void setStateReward(int s, double r) {
		stateRewards.put(s, r);
	}
	
	public void addToStateReward(int s, double r) {
		double newReward = r + getStateReward(s); // new reward = previous + addition
		setStateReward(s, newReward); // set new reward
	}

	@Override
	public double getTransitionReward(String eventName, int s, int t) {
		Map<Integer,Map<Integer, Double>> sourceToDestToRewardMap = transitionRewards.get(eventName);
		if (sourceToDestToRewardMap == null) {
			return 0.0;
		}
		Map<Integer, Double> destToRewardMap = sourceToDestToRewardMap.get(s);
		if (destToRewardMap == null) {
			return 0.0;
		}
		Double reward = destToRewardMap.get(t);
		if (reward == null) {
			return 0.0;
		} else {
			return reward;
		}
	}
	
	public Map<Integer,Map<Integer, Double>> getTransitionRewardsOfEvent(String eventName) {
		return transitionRewards.get(eventName);
	}
	
	/**
	 * Assign reward {@code r} for transitions from state {@code s} to state {@code t} via event of name {@code eventName}.
	 */
	public void setTransitionReward(String eventName, int s, int t, double r) {
		//if no records of such event exists so far, create it
		if (!transitionRewards.containsKey(eventName)) {
			transitionRewards.put(eventName, new HashMap<Integer,Map<Integer, Double>>());
		}
		Map<Integer,Map<Integer, Double>> sourceToDestToRewardMap = transitionRewards.get(eventName);
		//if no records of such source state exists so far, create it
		if (!sourceToDestToRewardMap.containsKey(s)) {
			sourceToDestToRewardMap.put(s, new HashMap<Integer, Double>());
		}
		Map<Integer, Double> destToRewardMap = sourceToDestToRewardMap.get(s);
		//now that everything exists, just assign the reward
		destToRewardMap.put(t, r);
	}
	
	/**
	 * Add {@code r} to rewards for transitions from state {@code s} to state {@code t} via event of name {@code eventName}.
	 */
	public void addToTransitionReward(String eventName, int s, int t, double r) {
		double newReward = r + getTransitionReward(eventName, s, t); // additional reward + existing reward
		setTransitionReward(eventName, s, t, newReward); // assign their sum
	}

	// assumes the product GSMP has the same events of the same name
	@Override
	public GSMPRewards liftFromModel(Product<? extends Model> product) {
		GSMPRewardsSimple rewardsProd = new GSMPRewardsSimple();
		GSMP modelProd = (GSMP)product.getProductModel();
		for (int s = 0; s < modelProd.getNumStates(); ++s) {
			rewardsProd.setStateReward(s, stateRewards.get(product.getModelState(s)));
		}
		
		Set<Map.Entry<String,Map<Integer,Map<Integer, Double>>>> eventToSourceToDestToRewardEntrySet =
				new HashSet<>(transitionRewards.entrySet());
		for (Map.Entry<String,Map<Integer,Map<Integer, Double>>> eventEntry : eventToSourceToDestToRewardEntrySet) {
			Set<Map.Entry<Integer,Map<Integer, Double>>> sourceToDestToRewardEntrySet =
					new HashSet<>(eventEntry.getValue().entrySet());
			for (Map.Entry<Integer,Map<Integer, Double>> sourceEntry : sourceToDestToRewardEntrySet) {
				Set<Map.Entry<Integer, Double>> destToRewardEntrySet = 
						new HashSet<>(sourceEntry.getValue().entrySet());
				for (Map.Entry<Integer, Double> destEntry : destToRewardEntrySet) {
					double reward = getTransitionReward(
							eventEntry.getKey(),
							sourceEntry.getKey(),
							destEntry.getKey());
					rewardsProd.setTransitionReward(
							eventEntry.getKey(),
							product.getModelState(sourceEntry.getKey()),
							product.getModelState(destEntry.getKey()),
							reward);
					// TODO MAJO - not sure if this is correct. But this method is probably irrelevant anyway.
				}
			}
		}
		return rewardsProd;
	}

	@Override
	public boolean hasTransitionRewards() {
		return (!transitionRewards.isEmpty());
	}
	
	public String toStringStateRewards() {
		return "GSMP state rewards:\n" + stateRewards;
	}
	
	public String toStringTransitionRewards() {
		String str = "GSMP transition rewards:\n";
		for ( Map.Entry<String, Map<Integer,Map<Integer, Double>>> firstEntry : transitionRewards.entrySet()) {
			str += "Event \"" + firstEntry.getKey() + "\n";
			boolean first = true;
			for (Map.Entry<Integer, Map<Integer, Double>> secondEntry : firstEntry.getValue().entrySet()) {
				if (first) {
					first = false;
				} else {
					str += ", ";
				}
				str += secondEntry.getKey() + ": " + secondEntry.getValue();
			}
			str += "\n";
		}
		return str;
	}
	
	@Override
	public String toString()
	{
		return toStringStateRewards() + "\n" + toStringTransitionRewards();
	}
}
