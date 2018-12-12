//==============================================================================
//	
//	Copyright (c) 2017-
//	Authors:
//	* Mario Uhrik <433501@mail.muni.cz> (Masaryk University)
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import explicit.ACTMCSimple;
import explicit.Distribution;
import explicit.GSMP;
import explicit.GSMPEvent;
import explicit.Model;
import explicit.Product;

/**
 * Simple read-only rewards data structure for an ACTMC based on {@code GSMPRewardsSimple}.
 * This class is only used during model checking.
 * <br>
 * ACTMCs have no distinct exponential events. Instead, they are all condensed into the CTMC transition matrix.
 * In addition, they have at most one active GSMP event in any given state.
 * <br>
 * This is a read-only class constructed on demand from GSMPRewardsSimple.
 * See class {@code ACTMCSimple} for deeper explanation.
 */
public class ACTMCRewardsSimple implements MCRewards
{
	
	/**
	 * Indices of states with rewards mapped onto them.
	 */
	private Map<Integer, Double> stateRewards;
	
	/**
	 * Indices of states with rewards mapped onto them.
	 * On the contrary to {@link ACTMCRewardsSimple#stateRewards}, this map already keeps merged
	 * CTMC transition rewards.
	 */
	private Map<Integer, Double> mergedStateRewards;
	
	/**
	 * The First map   contains Second Maps mapped onto source state indices.
	 * The Second maps contain non-exponential event rewards mapped onto destination state indices.
	 * <br>
	 * Note there is only up to one active event per state, so 2D mapping is enough.
	 */
	private Map<Integer,Map<Integer, Double>> eventTransitionRewards;
	/**
	 * The First map   contains Second Maps mapped onto source state indices.
	 * The Second maps contain exponential ctmc rewards mapped onto destination state indices.
	 */
	private Map<Integer,Map<Integer, Double>> ctmcTransitionRewards;

	/**
	 * Constructs ACTMC rewards from GSMP rewards.
	 * The GSMP rewards are assumed to belong to a GSMP that is confirmed to be an ACTMC.
	 * Otherwise, the behavior of this function is undefined.
	 * @param gsmpRew GSMP rewards of belonging to the {@code gsmpModel}
	 * @param gsmpModel GSMP confirmed to fit the definition of an ACTMC.
	 * @param actmc is an equivalent ACTMC constructed from {@code gsmpModel}
	 */
	public ACTMCRewardsSimple(GSMPRewardsSimple gsmpRew, GSMP gsmpModel, ACTMCSimple actmc) {
		this.stateRewards = new HashMap<Integer, Double>(gsmpRew.getStateRewards());
		this.mergedStateRewards = new HashMap<Integer, Double>(gsmpRew.getStateRewards());
		this.eventTransitionRewards = new HashMap<Integer, Map<Integer, Double>>();
		this.ctmcTransitionRewards = new HashMap<Integer, Map<Integer, Double>>();
		
		// get a list of exponential and non-exponential event names from the model
		List<GSMPEvent> allEvents = gsmpModel.getEventList();
		List<String> expEventNames = new ArrayList<String>();
		List<String> nonExpEventNames = new ArrayList<String>();
		for (GSMPEvent event : allEvents) {
			if (event.isExponential()) {
				expEventNames.add(event.getIdentifier());
			} else {
				nonExpEventNames.add(event.getIdentifier());
			}
		}
		
		// copy the non-exponential event rewards into eventTransitionRewards
		for (String nonExpEventName : nonExpEventNames) {	
			Map<Integer, Map<Integer, Double>> eventTransRew = gsmpRew.getTransitionRewardsOfEvent(nonExpEventName);
			if (eventTransRew == null) {
				continue;
			}
			for (Map.Entry<Integer, Map<Integer, Double>> entry : eventTransRew.entrySet()) {
				eventTransitionRewards.put(entry.getKey(), entry.getValue());
			}
		}
		
		// merge all exponential event rewards and weight them by the rates
		for (int s = 0; s < gsmpModel.getNumStates() ; ++s) {
			List<GSMPEvent> activeExpEvents = gsmpModel.getActiveEvents(s);
			activeExpEvents.removeIf(e -> !e.isExponential());
			
			Map<Integer, Double> sumsOfRates = new HashMap<Integer, Double>();
			Map<Integer, Double> ctmcRews = new HashMap<Integer, Double>();
			for (GSMPEvent actExpEvent : activeExpEvents) {
				for (Integer t : actExpEvent.getTransitions(s).getSupport()) {
					double rew = gsmpRew.getTransitionReward(actExpEvent.getIdentifier(), s, t);
					if (rew == 0.0) {
						continue;
					}
					double rate = actExpEvent.getFirstParameter();
					sumsOfRates.putIfAbsent(t, sumsOfRates.getOrDefault(t, 0.0) + rate);
					ctmcRews.putIfAbsent(t, ctmcRews.getOrDefault(t, 0.0) + (rew * rate)); // weight the reward by the rate
				}
			}
			for (Map.Entry<Integer, Double> entry : sumsOfRates.entrySet()) {
				int t = entry.getKey();
				double sumOfRates = entry.getValue();
				double ctmcRew = ctmcRews.get(t);
				
				//finally, normalize the result to obtain weighted average (weighted by rates)
				ctmcRew = ctmcRew / sumOfRates;
				// and add it
				ctmcTransitionRewards.putIfAbsent(s, new HashMap<Integer, Double>());
				ctmcTransitionRewards.get(s).put(t, ctmcRew);
			}
		}
		
		mergeCTMCTransitionRewards(actmc);
	}
	
	/**
	 * Copy constructor
	 */
	public ACTMCRewardsSimple(ACTMCRewardsSimple rews) {
		this.stateRewards = new HashMap<Integer, Double>(rews.stateRewards);
		this.mergedStateRewards = new HashMap<Integer, Double>(rews.mergedStateRewards);
		this.eventTransitionRewards = new HashMap<Integer, Map<Integer, Double>>(rews.eventTransitionRewards);
		this.ctmcTransitionRewards = new HashMap<Integer, Map<Integer, Double>>(rews.ctmcTransitionRewards);
	}

	/**
	 * @param s state index
	 * @return state reward for state {@code s}.
	 * 		   See {@link ACTMCRewardsSimple#stateRewards}.
	 */
	public double getStateReward(int s) {
		Double reward = stateRewards.get(s);
		if (reward == null) {
			return 0.0;
		} else {
			return reward;
		}
	}
	
	/**
	 * @param s state index
	 * @return state reward for state {@code s}, incremented by the ctmc transition reward addition.
	 * 		   See {@link ACTMCRewardsSimple#mergedStateRewards}.
	 */
	public double getMergedStateReward(int s) {
		Double reward = mergedStateRewards.get(s);
		if (reward == null) {
			return 0.0;
		} else {
			return reward;
		}
	}

	/**
	 * Get the non-exponential event transition reward when going from state s to state t.
	 * <br>
	 * Returns 0 if not specified.
	 */
	public double getEventTransitionReward(int s, int t) {
		Map<Integer, Double> destToRewardMap = eventTransitionRewards.get(s);
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
	
	/**
	 * Get the reward map of non-exponential event transition rewards from state {@code s}.
	 * Returns null if not specified.
	 */
	public Map<Integer, Double> getEventTransitionRewards(int s) {
		return eventTransitionRewards.get(s);
	}
	
	/**
	 * Get the exponential transition reward when going from state s to state t.
	 * <br>
	 * Returns 0 if not specified.
	 */
	public double getTransitionReward(int s, int t) {
		Map<Integer, Double> destToRewardMap = ctmcTransitionRewards.get(s);
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
	
	/**
	 * Get the reward map of exponential transition rewards from state {@code s}.
	 * Returns null if not specified.
	 */
	public Map<Integer, Double> getTransitionRewards(int s) {
		return ctmcTransitionRewards.get(s);
	}

	public ACTMCRewardsSimple liftFromModel(Product<? extends Model> product) {
		throw new UnsupportedOperationException("Not implemented!");
	}
	
	/**
	 * Find a maximum state reward from within a set of states represented by {@code bs}.
	 * Returns 0 if none are found.
	 * @param bs set of states
	 */
	public double getMax(BitSet bs) {
		double max = Double.MIN_VALUE;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
			double rew = getStateReward(i);
			if (rew > max) {
				max = rew;
			}
		}
		
		if (max == Double.MIN_VALUE) {
			max = 0;
		}
		return max;
	}

	/**
	 * Returns true iff this model has exponential transition (CTMC) rewards.
	 * <br>
	 * To check whether the event has non-exponential transition (event) rewards,
	 * use {@code hasEventTransitionRewards()}.
	 */
	@Override
	public boolean hasTransitionRewards() {
		return (! ctmcTransitionRewards.isEmpty());
	}
	
	public boolean hasEventTransitionRewards() {
		return (! eventTransitionRewards.isEmpty());
	}
	
	/**
	 * Fills {@link ACTMCRewardsSimple#mergedStateRewards} with additions of the ctmc transition
	 * rewardsmaccording to {@code actmc}.
	 * @param actmc ACTMC model these rewards are created from.
	 *              This is needed because the rates are required as weights.
	 *              Assumes the {@code actmc} is uniformised!
	 */
	private void mergeCTMCTransitionRewards(ACTMCSimple actmc) {
		for (Map.Entry<Integer, Map<Integer,Double>> entry : ctmcTransitionRewards.entrySet()) {
			int s = entry.getKey();
			Map<Integer, Double> rews = entry.getValue();
			Set<Integer> rewSet = rews.keySet();
			
			// prepare transition rates from state s
			Distribution distr = actmc.getTransitions(s);
			
			double stateRewardAddition = 0;
			// multiply transitions rewards to state t by rates
			for ( int t : rewSet) {
				stateRewardAddition += rews.get(t) * distr.get(t);
			}
			
			// add the computed value to the existing state reward
			if (stateRewardAddition > 0) {
				mergedStateRewards.put(s, getStateReward(s) + stateRewardAddition);
			}
		}
	}
	
}
