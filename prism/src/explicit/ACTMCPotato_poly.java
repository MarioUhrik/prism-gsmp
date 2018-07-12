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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

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
	

}