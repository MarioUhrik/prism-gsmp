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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class ACTMCRewardsSimple implements Rewards
{
	
	/**
	 * Indices of states with rewards mapped onto them.
	 */
	private Map<Integer, Double> stateRewards;
	
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
	 *                  Required to distinguish exponential events.
	 */
	public ACTMCRewardsSimple(GSMPRewardsSimple gsmpRew, GSMP gsmpModel) {
		// TODO MAJO - confirm this is without bugs!
		this.stateRewards = gsmpRew.getStateRewards();
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
			for (int t = 0; t < gsmpModel.getNumStates() ; ++t) {
				double sumOfRates = 0.0;
				double ctmcRew = 0.0;
				for (GSMPEvent actExpEvent : activeExpEvents) {
					double rew = gsmpRew.getTransitionReward(actExpEvent.getIdentifier(), s, t);
					if (rew == 0.0) {
						continue;
					}
					double rate = actExpEvent.getFirstParameter();
					sumOfRates += rate;
					ctmcRew += rew * rate; // weight the reward by the rate
				}
				if (ctmcRew > 0.0) {
					//finally, normalize the result to obtain weighted average (weighted by rates)
					ctmcRew = ctmcRew / sumOfRates;
					// and add it
					ctmcTransitionRewards.putIfAbsent(s, new HashMap<Integer, Double>());
					ctmcTransitionRewards.get(s).put(t, ctmcRew);
				}
			}
		}
		
	}
	
	public double getStateReward(int s) {
		Double reward = stateRewards.get(s);
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
	public double getCTMCTransitionReward(int s, int t) {
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
	 * This class is read only. Do not call this method.
	 */
	@Deprecated
	@Override
	public ACTMCRewardsSimple liftFromModel(Product<? extends Model> product) {
		return null;
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
	
}
