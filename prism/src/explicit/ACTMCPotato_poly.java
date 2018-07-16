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
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import common.polynomials.Polynomial;
import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * Abstract superclass of ACTMCPotato classes that are implemented using general
 * polynomials P(t), where P(t) is the potato event firing time.
 * <br>
 * This superclass expects the implementations of it's inheritors to fill up these
 * polynomial member variables accordingly with calls to {@code computeMeanTimes()},
 * {@code computeMeanDistributions()}, and {@code computeMeanRewards()} in original indexing. 
 * <br>
 * WARNING: Be mindful that the computed polynomials are good only up to certain precision,
 * determined by the given event parameters at the time of creation of the polynomials.
 * <br>
 * See {@link ACTMCPotato} for more basic info.
 */
public abstract class ACTMCPotato_poly extends ACTMCPotato
{
	
	/**
	 * Map that holds polynomials which when evaluated at some firing time t yield
	 * values of {@link ACTMCPotato#meanTimes}.
	 * <br>
	 * First key is the potato entrance. The second key is the queried state within the potato.
	 */
	protected Map<Integer, Map<Integer, Polynomial>> meanTimesPolynomials;
	
	/**
	 * Map that holds polynomials which when evaluated at some firing time t yield
	 * values of {@link ACTMCPotato#meanDistributions}. 
	 * <br>
	 * First key is the potato entrance. The second key is the queried successor/potato state.
	 */
	protected Map<Integer, Map<Integer, Polynomial>> meanDistributionsPolynomials;
	/**
	 * Map that holds polynomials which when evaluated at some firing time t yield
	 * values of {@link ACTMCPotato#meanDistributionsBeforeEvent}. 
	 * <br>
	 * First key is the potato entrance. The second key is the queried successor/potato state.
	 */
	protected Map<Integer, Map<Integer, Polynomial>> meanDistributionsBeforeEventPolynomials;
	
	/**
	 * Map that holds polynomials which when evaluated at some firing time t yield
	 * values of {@link ACTMCPotato#meanRewards}. 
	 * <br>
	 * Key is the potato entrance.
	 */
	protected Map<Integer, Polynomial> meanRewardsPolynomials;
	/**
	 * Map that holds polynomials which when evaluated at some firing time t yield
	 * values of {@link ACTMCPotato#meanRewardsBeforeEvent}. 
	 * <br>
	 * Key is the potato entrance.
	 */
	protected Map<Integer, Polynomial> meanRewardsBeforeEventPolynomials;
	
	/**
	 * {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)}
	 */
	public ACTMCPotato_poly(ACTMCSimple actmc, GSMPEvent event, 
			ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
		initializeMaps();
	}

	public ACTMCPotato_poly(ACTMCPotato_poly other) {
		super(other);
		//Some computed values may no longer be valid. Recompute the vulnerable stuff.
		initializeMaps();
	}
	
	private void initializeMaps() {
		computeStates();
		
		meanTimesPolynomials = new HashMap<Integer, Map<Integer, Polynomial>>(entrances.size());
		for (int entrance : entrances) {
			meanTimesPolynomials.put(entrance, new HashMap<Integer, Polynomial>(potato.size()));
		}
		
		meanDistributionsBeforeEventPolynomials = new HashMap<Integer, Map<Integer, Polynomial>>(entrances.size());
		for (int entrance : entrances) {
			meanDistributionsBeforeEventPolynomials.put(entrance, new HashMap<Integer, Polynomial>(potato.size() + successors.size()));
		}
		meanDistributionsPolynomials = new HashMap<Integer, Map<Integer, Polynomial>>(entrances.size());
		for (int entrance : entrances) {
			meanDistributionsPolynomials.put(entrance, new HashMap<Integer, Polynomial>(successors.size()));
		}
		
		meanRewardsBeforeEventPolynomials = new HashMap<Integer, Polynomial>(potato.size());
		meanRewardsPolynomials = new HashMap<Integer, Polynomial>(potato.size());
	}
	
	/**
	 * Returns {@link ACTMCPotato_poly#meanTimesPolynomials}.
	 */
	public Map<Integer, Map<Integer, Polynomial>> getMeanTimesPolynomials() throws PrismException {
		if (!meanTimesComputed) {
			computeMeanTimes();
		}
		return meanTimesPolynomials;
	}
	
	/**
	 * Returns {@link ACTMCPotato_poly#meanDistributionsBeforeEventPolynomials}.
	 */
	public Map<Integer, Map<Integer, Polynomial>> getMeanDistributionsBeforeEventPolynomials() throws PrismException {
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		return meanDistributionsBeforeEventPolynomials;
	}
	
	/**
	 * Returns {@link ACTMCPotato_poly#meanDistributionsPolynomials}.
	 */
	public Map<Integer, Map<Integer, Polynomial>> getMeanDistributionsPolynomials() throws PrismException {
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		return meanDistributionsPolynomials;
	}
	
	/**
	 * Returns {@link ACTMCPotato_poly#meanRewardsBeforeEventPolynomials}.
	 */
	public Map<Integer, Polynomial> getMeanRewardsBeforeEventPolynomials() throws PrismException {
		if (!meanRewardsComputed) {
			computeMeanRewards();
		}
		return meanRewardsBeforeEventPolynomials;
	}
	
	/**
	 * Returns {@link ACTMCPotato_poly#meanRewardsPolynomials}.
	 */
	public Map<Integer, Polynomial> getMeanRewardsPolynomials() throws PrismException {
		if (!meanRewardsComputed) {
			computeMeanRewards();
		}
		return meanRewardsPolynomials;
	}
	
	/**
	 * Returns an array of polynomials that represents the event transition rewards.
	 * I.e. if n is an entrance state index and t is a time parameter, resPoly[n](t)
	 * returns the mean event transition reward for n and t. 
	 * <br>
	 * This is done by weighting the event transition reward by the probability of the model
	 * being in the correct state at the time of event occurrence, and by the probability
	 * that event transition then actually occurs.
	 * NOTE: No adjustment for the mean time it takes the event to occur is done!!!
	 * @param originalIndexing true if the array is indexed the same way as the original ACTMC.
	 *                         This should generally be true when this method is called from the outside.
	 *                         However, when called from within, the array may be indexed differently.
	 * @return New array of polynomials with potato event transition reward application.
	 * @throws PrismException 
	 */
	public Polynomial[] getEventRewardsPoly(boolean originalIndexing) throws PrismException {
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		
		//Create a hard copy of rewPolyBeforeEvent array
		Polynomial[] resPoly = new Polynomial[actmc.getNumStates()];
		for (int n = 0; n < resPoly.length ; ++n) {
			resPoly[n] = new Polynomial();
		}
		
		for (int entrance : entrances) {
			for (int ps : potato) {
				Map<Integer, Double> rews = rewards.getEventTransitionRewards(ps);
				if (rews == null) {
					continue;
				}
				
				Distribution eventTransitions = event.getTransitions(ps);
				Polynomial additionPoly = new Polynomial(meanDistributionsBeforeEventPolynomials.get(entrance).get(ps).coeffs);
				Set<Integer> rewSet = rews.keySet();
				for (int succ : rewSet) {
					double prob = eventTransitions.get(succ);
					double eventRew = rews.get(succ);
					BigDecimal factor = new BigDecimal(String.valueOf(prob), mc).multiply(new BigDecimal(String.valueOf(eventRew), mc), mc);
					additionPoly.multiplyWithScalar(factor, mc);
					if (originalIndexing) {
						resPoly[entrance].add(additionPoly, mc);
					} else {
						resPoly[ACTMCtoDTMC.get(entrance)].add(additionPoly, mc);
					}
				}
			}
		}
		return resPoly;
	}
	

}