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

import explicit.Model;
import explicit.Product;

/**
 * Classes that provide (read) access to explicit-state rewards for an GSMP.
 */
public interface GSMPRewards extends Rewards
{
	/**
	 * Get the state reward for state {@code s}.
	 */
	public abstract double getStateReward(int s);

	/**
	 * Get the transition rewards for going from state {@code s} to state {@code t} via event of name {@code eventName}
	 */
	public abstract double getTransitionReward(String eventName, int s, int t);

	@Override
	public GSMPRewards liftFromModel(Product<? extends Model> product);

	/** Returns true if this reward structure has transition rewards */
	public boolean hasTransitionRewards();
	
	/**
	 * Returns a human-readable string of state rewards suited for printing
	 */
	public String toStringStateRewards();
	
	/**
	 * Returns a human-readable string of transition rewards suited for printing
	 */
	public String toStringTransitionRewards();
}
