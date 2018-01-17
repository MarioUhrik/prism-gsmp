//==============================================================================
//	
//	Copyright (c) 2013-
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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class for storage and computation of potato-related data for ACTMCs.
 * <br>
 * Potato is a subset of states of an ACTMC in which a given event is active.
 * <br><br>
 * This data is fundamental for ACTMC model checking methods based on reduction
 * of ACTMC to CTMC. The reduction works by pre-computing the expected behavior
 * (rewards, spent time, resulting distribution...) occurring between
 * entering and leaving a potato. Then, these expected values are used in
 * regular CTMC/DTMC model checking methods.
 */
public class ACTMCPotatoData
{
	/** ACTMC model this data is associated with */
	private ACTMCSimple actmc;
	/** specific event of {@code actmc} this data is associated with */
	private GSMPEvent event;
	
	/** 
	 * List of entrances into the potato.
	 * <br>
	 * I.e. such states of {@code actmc} where {@code event} is active,
	 * and at the same time they are reachable in a single transition
	 * from a state where {@code event} is not active.
	 */
	private List<Integer> entrances = new ArrayList<Integer>();
	private boolean entrancesComputed = false;
	
	/** Mapping of expected accumulated rewards until leaving the potato onto states used to enter the potato */
	private Map<Integer, Double> meanRewards = new HashMap<Integer, Double>();
	private boolean meanRewardsComputed = false;
	
	/** Mapping of expected times spent in the potato onto states used to enter the potato */
	private Map<Integer, Double> meanTimes = new HashMap<Integer, Double>();
	private boolean meanTimesComputed = false;
	
	/** Mapping of expected outcome state probability distributions onto states used to enter the potato */
	private Map<Integer, Distribution> meanDistributions = new HashMap<Integer, Distribution>();
	private boolean meanDistributionsComputed = false;
	

	/**
	 * Constructor
	 * @param actmc Associated ACTMC model. Must not be null!
	 * @param event Event belonging to the ACTMC. Must not be null!
	 * @throws Exception if the arguments break the above rules
	 */
	public ACTMCPotatoData(ACTMCSimple actmc, GSMPEvent event) throws Exception {
		if (actmc == null || event == null) {
			throw new NullPointerException("ACTMCPotatoData constructor has received a null object!");
		}
		if (!actmc.getEventList().contains(event)) {
			throw new IllegalArgumentException("ACTMCPotatoData received arguments (actmc,event) where event does not belong to actmc!");
		}
		
		this.actmc = actmc;
		this.event = event;
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
	 * Gets a list of states that are entrances into this potato.
	 * I.e. such states of {@code actmc} where {@code event} is active,
	 * and at the same time they are reachable in a single transition
	 * from a state where {@code event} is not active.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public List<Integer> getEntrances() {
		if (!entrancesComputed) {
			computeEntrances();
		}
		return entrances;
	}
	
	/**
	 * Gets a map where the keys are entrances into the potato, and
	 * the values are mean accumulated rewards until leaving the potato
	 * if entered from state {@code key}.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Map<Integer, Double> getMeanRewards() {
		if (!meanRewardsComputed) {
			computeMeanRewards();
		}
		return meanRewards;
	}
	
	/**
	 * Gets a map where the keys are entrances into the potato, and
	 * the values are mean times until leaving the potato
	 * if entered from state {@code key}.
	 * <br>
	 * If this is the first call, this method computes it before returning it.
	 */
	public Map<Integer, Double> getMeanTimes() {
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
	public Map<Integer, Distribution> getMeanDistributions() {
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		return meanDistributions;
	}
	
	private void computeEntrances() {
		BitSet potatoBs = event.getActive();
		List<Integer> candidateEntrances = new ArrayList<Integer>();
		for (int ps = potatoBs.nextSetBit(0); ps >= 0; ps = potatoBs.nextSetBit(ps+1)) {
			candidateEntrances.add(ps);
		}
		
		// For each state of the ACTMC...
		for (int s = 0 ; s < actmc.getNumStates() ; ++s) {
			// ...if it does not belong to the potato...
			if (actmc.getActiveEvent(s) != event) {
				// ...check whether it has an exponential transition into the potato.
				for (Iterator<Integer> iter = candidateEntrances.iterator() ; iter.hasNext();) {
					int ps = iter.next();
					if (actmc.getTransitions(s).get(ps) > 0.0) {
						entrances.add(ps);
						iter.remove();
					}
				}
				if (candidateEntrances.isEmpty()) {
					break;
				}
			}
		}
		entrancesComputed = true;
	}
	
	private void computeMeanRewards() {
		if (!entrancesComputed) {
			computeEntrances();
		}
		for (int entrance : entrances) {
			// TODO MAJO - implement
			// I am not sure how to compute this though.
			throw new UnsupportedOperationException();
		}
		meanRewardsComputed = true;
	}
	
	private void computeMeanTimes() {
		if (!entrancesComputed) {
			computeEntrances();
		}
		for (int entrance : entrances) {
			// TODO MAJO - implement
			// I am not sure how to compute this though.
			throw new UnsupportedOperationException();
		}
		meanTimesComputed = true;
	}
	
	private void computeMeanDistributions() {
		if (!entrancesComputed) {
			computeEntrances();
		}
		for (int entrance : entrances) {
			// TODO MAJO - implement
			// I am not sure how to compute this though.
			throw new UnsupportedOperationException();
		}
		meanDistributionsComputed = true;
	}


}