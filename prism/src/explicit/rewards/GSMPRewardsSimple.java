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

import explicit.Model;
import explicit.Product;

/**
 * Simple explicit-state storage of rewards for an GSMP.
 * Like the related class GSMPSimple, this is not especially efficient, but mutable (in terms of size).
 */
public class GSMPRewardsSimple implements GSMPRewards
{
	// TODO MAJO - implement

	@Override
	public double getStateReward(int s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getTransitionReward(int s, int t, String eventName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public GSMPRewards liftFromModel(Product<? extends Model> product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasTransitionRewards() {
		// TODO Auto-generated method stub
		return false;
	}
}
