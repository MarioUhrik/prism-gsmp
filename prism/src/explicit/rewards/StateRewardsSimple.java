//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//  * Mario Uhrik <433501@mail.muni.cz> (Masaryk University)
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

import explicit.Model;
import explicit.Product;

/**
 * Explicit-state storage of just state rewards (mutable).
 */
public class StateRewardsSimple extends StateRewards
{
	/** Arraylist of state rewards **/
	protected ArrayList<Double> stateRewards;

	/**
	 * Constructor: all zero rewards.
	 */
	public StateRewardsSimple()
	{
		stateRewards = new ArrayList<Double>();
	}

	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public StateRewardsSimple(StateRewardsSimple rews)
	{
		if (rews.stateRewards == null) {
			stateRewards = null;
		} else {
			int n = rews.stateRewards.size();
			stateRewards = new ArrayList<Double>(n);
			for (int i = 0; i < n; i++) {
				stateRewards.add(rews.stateRewards.get(i));
			}
		}
	}

	// Mutators

	/**
	 * Set the reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, double r)
	{
		if (r == 0.0 && s >= stateRewards.size())
			return;
		// If list not big enough, extend
		int n = s - stateRewards.size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				stateRewards.add(0.0);
			}
		}
		// Set reward
		stateRewards.set(s, r);
	}

	// Accessors

	@Override
	public double getStateReward(int s)
	{
		// TODO MAJO - Note: I reimplemented this
		if (stateRewards.size() > s) {
			return stateRewards.get(s);
		} else {
			return 0.0;
		}
		
		/*
		try {
			return stateRewards.get(s);
		} catch (ArrayIndexOutOfBoundsException e) {
			return 0.0;
		}
		*/
	}
	
	/**
	 * Find the maximum state reward.
	 * Returns 0 if none are found.
	 */
	public double getMax() {
		BitSet bs = new BitSet(stateRewards.size());
		bs.flip(0, bs.size());
		return getMax(bs);
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

	// Converters
	
	@Override
	public StateRewards liftFromModel(Product<? extends Model> product)
	{
		Model modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();
		StateRewardsSimple rewardsProd = new StateRewardsSimple();
		for (int s = 0; s < numStatesProd; s++) {
			rewardsProd.setStateReward(s, getStateReward(product.getModelState(s)));
		}
		return rewardsProd;
	}
	
	// Other

	@Override
	public StateRewardsSimple deepCopy()
	{
		return new StateRewardsSimple(this);
	}
}
