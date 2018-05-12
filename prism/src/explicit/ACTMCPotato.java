//==============================================================================
//	
//	Copyright (c) 2018-
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

package explicit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * Abstract superclass for storage and computation of single potato-related data for ACTMCs.
 * <br>
 * Potato is a subset of states of an ACTMC in which a given event is active.
 * <br><br>
 * This data is fundamental for ACTMC model checking methods based on reduction
 * of ACTMC to DTMC. The reduction works by pre-computing the expected behavior
 * (rewards, spent time, resulting distribution...) occurring between
 * entering and leaving a potato. Then, these expected values are used in
 * regular CTMC/DTMC model checking methods.
 */
public abstract class ACTMCPotato
{
	/** ACTMC model this data is associated with */
	protected ACTMCSimple actmc;
	/** specific event of {@code actmc} this data is associated with */
	protected GSMPEvent event;
	/** Reward structure of the {@code actmc}. May be null.
	 *  The CTMC transition rewards are expected to have already been converted to state rewards only. */
	protected ACTMCRewardsSimple rewards = null;
	/** Bitset of target states for reachability analysis. May be null. */
	protected BitSet target = null;
	
	/**
	 * Set of states that belong to the potato, but are not reachability targets.
	 * <br>
	 * I.e. such states of {@code actmc} that are not in {@code target} and 
	 * where {@code event} is active.
	 */
	protected Set<Integer> potato = new HashSet<Integer>();
	/** 
	 * Subset of {@code potato} states that are acting as entrances into the potato.
	 * <br>
	 * I.e. such states of {@code actmc} where {@code event} is active,
	 * they are not reachability targets, and at the same time they are:
	 * <br>
	 * 1) reachable in a single exponential transition
	 * from a state where {@code event} is not active, or
	 * <br>
	 * 2) reachable as a self-loop of {@code event},
	 * <br>
	 * 3) part of the initial distribution, i.e. it may be the initial state.
	 */
	protected Set<Integer> entrances = new HashSet<Integer>();
	/**
	 * Set of states that are successors of the potato states, or reachability targets.
	 * <br>
	 * I.e. the states of the {@code actmc} that are:
	 * <br>
	 * 1) outside the potato and reachable from within
	 * the potato in a single transition, or
	 * <br>
	 * 2) inside the potato and reachable from within
	 * the potato as a self-loop of {@code event}, or
	 * <br>
	 * 3) target states that would otherwise be within the potato.
	 */
	protected Set<Integer> successors = new HashSet<Integer>();
	protected boolean statesComputed = false;
	
	/**
	 * DTMC making up the part of {@code actmc} such that it only
	 * contains states that are the union of {@code potato} and {@code successors}.
	 */
	protected DTMCSimple potatoDTMC = null;
	protected double uniformizationRate;
	/** Mapping from the state indices of {@code actmc} (K) to {@code potatoDTMC} (V)*/
	protected Map<Integer, Integer> ACTMCtoDTMC = new HashMap<Integer, Integer>();
	/** Mapping from the state indices of {@code potatoDTMC} (index) to {@code actmc} (value) */
	protected Vector<Integer> DTMCtoACTMC = new Vector<Integer>();
	protected boolean potatoDTMCComputed = false;
	
	/** Allowed error (kappa) for computation of FoxGlynn */
	protected BigDecimal kappa;
	/** Poisson distribution values computed and stored by class FoxGlynn. */
	protected FoxGlynn_BD foxGlynn;
	protected boolean foxGlynnComputed = false;
	
	/** Mapping of expected accumulated rewards until leaving the potato onto states used to enter the potato */
	protected Map<Integer, Double> meanRewards = new HashMap<Integer, Double>();
	protected boolean meanRewardsComputed = false;
	
	/** Mapping of expected times spent in individual states of the potato before leaving the potato
	 * onto individual states used to enter the potato.
	 * Sum of this distribution yields the total expected time spent within the potato. */
	protected Map<Integer, Distribution> meanTimes = new HashMap<Integer, Distribution>();
	protected boolean meanTimesComputed = false;
	
	/** Mapping of expected outcome state probability distributions onto states used to enter the potato.
	 *  I.e. if we enter the potato using state {@code key}, then {@code value} is the distribution
	 *  saying which states we are in after leaving the potato on average. */
	protected Map<Integer, Distribution> meanDistributions = new HashMap<Integer, Distribution>();
	/** Mapping of just-before-event state probability distributions onto states used to enter the potato.
	 *  I.e. if we enter the potato using state {@code key}, then {@code value} is the distribution
	 *  saying which states we are in just before the event occurs on average. */
	protected Map<Integer, Distribution> meanDistributionsBeforeEvent = new HashMap<Integer, Distribution>();
	protected boolean meanDistributionsComputed = false;
	

	/**
	 * From the scratch constructor
	 * @param actmc Associated ACTMC model. Must not be null!
	 * @param event Event belonging to the ACTMC. Must not be null!
	 * @param rewards Optional ACTMC Reward structure. May be null, but calls to
	 *        {@code getMeanReward()} with null reward structure throws an exception!
	 * @param target Bitset of target states (if doing reachability). May be null.
	 * @throws Exception if the arguments break the above rules
	 */
	public ACTMCPotato(ACTMCSimple actmc, GSMPEvent event, 
			ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		if (actmc == null || event == null) {
			throw new NullPointerException("ACTMCPotatoData constructor has received a null object!");
		}
		if (!actmc.getEventList().contains(event)) {
			throw new IllegalArgumentException("ACTMCPotatoData received arguments (actmc,event) where event does not belong to actmc!");
		}
		
		this.actmc = actmc;
		this.event = event;
		this.rewards = rewards;
		this.target = target;
	}
	
	/**
	 * Copy constructor
	 */
	public ACTMCPotato(ACTMCPotato other) {
		this.actmc = other.actmc;
		this.event = other.event;
		this.rewards = other.rewards;
		this.target = other.target;
		
		this.potato = other.potato;
		this.entrances = other.entrances;
		this.successors = other.successors;
		this.statesComputed = other.statesComputed;
		
		this.potatoDTMC = other.potatoDTMC;
		this.uniformizationRate = other.uniformizationRate;
		this.ACTMCtoDTMC = other.ACTMCtoDTMC;
		this.DTMCtoACTMC = other.DTMCtoACTMC;
		this.potatoDTMCComputed = other.potatoDTMCComputed;
		
		this.kappa = other.kappa;
		//If some different subclass is called, some computed values may no longer be valid. So recomputation is required.
		this.foxGlynn = null;
		this.foxGlynnComputed = false;
		
		this.meanRewards = new HashMap<Integer, Double>();
		this.meanRewardsComputed = false;
		
		this.meanTimes = new HashMap<Integer, Distribution>();
		this.meanTimesComputed = false;
		
		this.meanDistributions = new HashMap<Integer, Distribution>();
		this.meanDistributionsBeforeEvent = new HashMap<Integer, Distribution>();
		this.meanDistributionsComputed = false;
	}
	
	/**
	 * This method allows external insertion of custom kappa allowed error bounds.
	 * Kappa is the required accuracy for computation of FoxGlynn.
	 * @param kappa kappa allowed error bound
	 */
	public void setKappa(BigDecimal kappa) {
		this.kappa = kappa;
		// force this class to recompute all data with the new kappa
		foxGlynnComputed = false;
		meanTimesComputed = false;
		meanDistributionsComputed = false;
		meanRewardsComputed = false;
	}
	
	/** Gets the actmc model associated with this object */
	public ACTMCSimple getACTMC() {
		return actmc;
	}
	
	/** Gets the event within the model associated with this object */
	public GSMPEvent getEvent() {
		return event;
	}
	
	/**
	 * Gets a list of states that belong to this potato.
	 * I.e. such states of {@code actmc} where {@code event} is active.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Set<Integer> getPotato() {
		if (!statesComputed) {
			computeStates();
		}
		return potato;
	}
	
	/**
	 * Gets a list of states that are entrances into this potato.
	 * I.e. such states of {@code actmc} where {@code event} is active,
	 * and at the same time they are reachable in a single transition
	 * from a state where {@code event} is not active.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Set<Integer> getEntrances() {
		if (!statesComputed) {
			computeStates();
		}
		return entrances;
	}
	
	/**
	 * Gets a list of states outside the potato that are reachable
	 * from within the potato in a single transition.
	 * <br>
	 * I.e. the states of the {@code actmc} that are successors of the potato states.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Set<Integer> getSuccessors() {
		if (!statesComputed) {
			computeStates();
		}
		return successors;
	}
	
	/**
	 * Gets a uniformized DTMC making up the part of the {@code actmc} such that it
	 * only contains states that are the union of {@code potato} and {@code successors}.
	 * It is a sub-model mimicking the potato behavior of the underlying DTMC.
	 * <br>
	 * WARNING: this DTMC uses a different state indexing to that of the {@code actmc}.
	 * Use maps from {@code getMapDTMCtoACTMC()} and {@code getMapACTMCtoDTMC()}.
	 * <br>
	 * If this is the first call, this method computes them before returning it.
	 */
	public DTMCSimple getPotatoDTMC() {
		if (!potatoDTMCComputed) {
			computePotatoDTMC();
		}
		return potatoDTMC;
	}
	
	/**
	 * Gets the current kappa allowed error bound. May be null.
	 */
	public BigDecimal getKappa() {
		return kappa;
	}
	
	/**
	 * Gets a mapping from the state indices of {@code actmc} to {@code potatoDTMC}.
	 * I.e. {@code actmc} indices are keys, and {@code potatoDTMC} are values.
	 * <br>
	 * This is a reverse mapping of {@code getMapDTMCtoACTMC()}.
	 * <br>
	 * If this is the first call, this method computes them before returning it.
	 */
	public Map<Integer, Integer> getMapACTMCtoDTMC() {
		if (!potatoDTMCComputed) {
			computePotatoDTMC();
		}
		return ACTMCtoDTMC;
	}
	
	/**
	 * Gets a mapping from the state indices of {@code potatoDTMC} to {@code actmc}.
	 * I.e. {@code potatoDTMC} indices are indices, and {@code actmc} are values.
	 * <br>
	 * This is a reverse mapping of {@code getMapACTMCtoDTMC()}.
	 * <br>
	 * If this is the first call, this method computes them before returning it.
	 */
	public Vector<Integer> getMapDTMCtoACTMC() {
		if (!potatoDTMCComputed) {
			computePotatoDTMC();
		}
		return DTMCtoACTMC;
	}
	
	/**
	 * Gets a map where the keys are entrances into the potato, and
	 * the values are mean accumulated rewards until leaving the potato
	 * if entered from state {@code key}.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Map<Integer, Double> getMeanRewards() throws PrismException {
		if (!meanRewardsComputed) {
			computeMeanRewards();
		}
		return meanRewards;
	}
	
	/**
	 * Gets a map where the keys are entrances into the potato, and
	 * the value is a distribution of time spent within the states of the potato
	 * until first leaving the potato, having entered from state {@code key}.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Map<Integer, Distribution> getMeanTimes() throws PrismException {
		if (!meanTimesComputed) {
			computeMeanTimes();
		}
		return meanTimes;
	}
	
	/**
	 * Gets a map where the keys are entrances into the potato, and
	 * the values are mean outcome probability distributions after leaving the potato
	 * if entered from state {@code key}.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Map<Integer, Distribution> getMeanDistributions() throws PrismException {
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		return meanDistributions;
	}
	
	protected void computeStates() {
		computePotato();
		computeEntrances();
		computeSuccessors();
		processTargets();
		statesComputed = true;
	}
	
	protected void computePotato() {
		BitSet potatoBs = event.getActive();
		for (int ps = potatoBs.nextSetBit(0); ps >= 0; ps = potatoBs.nextSetBit(ps+1)) {
			potato.add(ps);
		}
	}
	
	/** Assumes that {@code computePotato()} has been called already */
	protected void computeEntrances() {
		List<Integer> candidateEntrances = new ArrayList<Integer>(potato);
		
		// For each state of the ACTMC...
		for (int s = 0 ; s < actmc.getNumStates() ; ++s) {
			// ...if it does not belong to the potato...
			if (actmc.getActiveEvent(s) != event) {
				// ...check whether it has an exponential transition into the potato.
				Distribution distr = actmc.getTransitions(s);
				for (Iterator<Integer> iter = candidateEntrances.iterator() ; iter.hasNext();) {
					int ps = iter.next();
					if (distr.get(ps) > 0.0) {
						entrances.add(ps);
						iter.remove();
					}
				}
				if (candidateEntrances.isEmpty()) {
					break;
				}
			}
		}
		
		// Check for non-exponential transitions into the potato (from other potatoes)
		List<GSMPEvent> events = actmc.getEventList();
		for (GSMPEvent e : events) {
			if (e == this.event) {
				continue; // consider only other events for now
			}
			BitSet eActStates = e.getActive();
			for (int s = eActStates.nextSetBit(0); s >= 0; s = eActStates.nextSetBit(s+1)) {
			     Distribution eDistr = e.getTransitions(s);
			     Set<Integer> eDistrSupport = eDistr.getSupport();
			     for (int t : eDistrSupport) {
			    	 if (potato.contains(t)) {
			    		 entrances.add(t);
			    	 }
			     }
			}
		}
		
		// Also, add all initial states within the potato.
		for (int is : actmc.getInitialStates()) {
			if (potato.contains(is)) {
				entrances.add(is);
				// TODO MAJO - what if we read the initial distribution from a file?
			}
		}
		
		// Lastly, check whether the event has a self-loop.
		for (int ps : potato) {
			Distribution distr = event.getTransitions(ps);
			for (int ps2 : potato) {
				if (distr.getSupport().contains(ps2)) {
					entrances.add(ps2);
				}
			}
			// Also, since I am iterating over the event distributions,
			// I might as well find the event distribution successors.
			successors.addAll(distr.getSupport());
		}
	}
	
	/** Assumes that {@code computePotato()} and {@code computeEntrances()}
	 *  have been called already */
	protected void computeSuccessors() {
		for (int ps : potato) {
			Set<Integer> support = new HashSet<Integer>(actmc.getTransitions(ps).getSupport());
			support.removeIf( s -> potato.contains(s));
			successors.addAll(support);
		}
	}
	
	/** Assumes that {@code computeSuccessors()} has been called already. */
	protected void processTargets() {
		if (target == null) {
			return;
		}
		// Move all target states outside the potato, but consider them successors
		for (int s = target.nextSetBit(0); s >= 0; s = target.nextSetBit(s+1)) {
		     if (potato.remove(s)) {
		    	 entrances.remove(s);
		    	 successors.add(s);
		     }
		}
	}
	
	protected void computePotatoDTMC() {
		if (!statesComputed) {
			computeStates();
		}
		
		// Identify the set of relevant states and declare the new CTMC
		Set<Integer> potatoACTMCStates = new HashSet<Integer>(potato);
		potatoACTMCStates.addAll(successors);
		CTMCSimple potatoCTMC = new CTMCSimple(potatoACTMCStates.size());
		
		// Since the states of the new CTMC are indexed from 0,
		// we need a mapping from the original ACTMC to the new DTMC,
		// and vice-versa.
		{
			int index = 0;
			for (int s : potatoACTMCStates) {
				ACTMCtoDTMC.put(s, index);
				DTMCtoACTMC.add(index, s);
				++index;
			}
		}
		
		uniformizationRate = actmc.getMaxExitRate(); // TODO MAJO - maxExitRate of the potatoCTMC is enough!!! // TODO MAJO - actually, I dont think its enough
		// Construct the transition matrix of the new CTMC
		for (int s : potatoACTMCStates) {
			if (potato.contains(s)) {
				// If the state is a part of the potato, retain the distribution as is
				Distribution distr = actmc.getTransitions(s);
				Set<Integer> support = new HashSet<Integer>(distr.getSupport());
				support.removeIf( state -> !potatoACTMCStates.contains(state) );
				for ( int state : support) {
					potatoCTMC.addToProbability(ACTMCtoDTMC.get(s), ACTMCtoDTMC.get(state), distr.get(state));
				}
			} else {
				// Else the state is a potato successor, so make it absorbing.
				potatoCTMC.addToProbability(ACTMCtoDTMC.get(s), ACTMCtoDTMC.get(s), uniformizationRate);
			}
		}
		
		// convert the CTMC to a DTMC and store the DTMC
		//potatoCTMC.uniformise(uniformizationRate); // TODO MAJO - make 100% sure this can be deleted
		potatoDTMC = potatoCTMC.buildUniformisedDTMC(uniformizationRate);
		
		potatoDTMCComputed = true;
	}
	
	/** Uses class FoxGlynn to pre-compute the Poisson distribution. 
	 *  <br>
	 *  After calling this, {@code foxGlynnComputed} is set to true,
	 *  and the result is saved within {@code foxGlynn} */
	protected abstract void computeFoxGlynn() throws PrismException;

	/**
	 * For all potato entrances, computes the expected time spent within the potato
	 * before leaving the potato, having entered from a particular entrance.
	 * This is computed using the expected cumulative reward with reward 1
	 * for the potato entrances, and with a time bound given by the potato event.
	 * <br>
	 * After calling this, {@code meanTimesComputed} is set to true,
	 * and the result is saved within {@code meanTimes}.
	 */
	protected abstract void computeMeanTimes() throws PrismException;
	
	/**
	 * For all potato entrances, computes the expected distributions
	 * on states after leaving the potato, having entered from a particular entrance.
	 * I.e., on average, where does the ACTMC end up when it happens to enter a potato.
	 * <br>
	 * After calling this, {@code meanDistributionsComputed} is set to true,
	 * and the result is saved within {@code meanDistributions} and {@code meanDistributionsBeforeEvent}.
	 */
	protected abstract void computeMeanDistributions() throws PrismException;
	
	/**
	 * For all potato entrances, computes the expected reward earned within the potato
	 * before leaving the potato, having entered from a particular entrance.
	 * This is computed using the expected cumulative reward using the ACTMC reward
	 * structure for states within the potato, and with a time bound
	 * given by the potato event. Since this would only be the underlying CTMC behavior,
	 * the potato event behavior is then applied as well.
	 * <br>
	 * After calling this, {@code meanRewardsComputed} is set to true,
	 * and the result is saved within {@code meanRewards}.
	 */
	protected abstract  void computeMeanRewards() throws PrismException;
	
	/**
	 * Applies the potato event transition rewards to a given reward vector.
	 * This is done by weighting the event transition reward by the probability of the model
	 * being in the correct state at the time of event occurrence, and by the probability
	 * that event transition then actually occurs. This value is then added to the reward vector.
	 * NOTE: No adjustment for the mean time it takes the event to occur is done!!!
	 * @param rewardsArray rewards array of rewards where each index is a state of the model
	 * @param originalIndexing true if the array is indexed the same way as the original ACTMC.
	 *                         This should generally be true when this method is called from the outside.
	 *                         However, when called from within, the array may be indexed differently.
	 * @return {@code rewardsArray}, but with rewards increased by the potato event transition reward application.
	 * @throws PrismException 
	 */
	public double[] applyEventRewards(double[] rewardsArray, boolean originalIndexing) throws PrismException {
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		
		for (int entrance : entrances) {
			for (int ps : potato) {
				Map<Integer, Double> rews = rewards.getEventTransitionRewards(ps);
				if (rews == null) {
					continue;
				}
				
				Distribution eventTransitions = event.getTransitions(ps);
				double weight = meanDistributionsBeforeEvent.get(entrance).get(ps);
				Set<Integer> rewSet = rews.keySet();
				for (int succ : rewSet) {
					double prob = eventTransitions.get(succ);
					double eventRew = rews.get(succ);
					if (originalIndexing) {
						rewardsArray[entrance] += prob * weight * eventRew;
					} else {
						rewardsArray[ACTMCtoDTMC.get(entrance)] += prob * weight * eventRew;
					}
				}
			}
		}
		return rewardsArray;
	}

}