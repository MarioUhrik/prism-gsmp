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

import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * See parent class documentation for more basic info. {@link ACTMCPotato}
 * <br>
 * This extension implements high-precision precomputation
 * of exponentially-distributed potatoes using class BigDecimal.
 * <br>
 * HOW IT'S DONE:
 * This class is implemented as Erlang distribution with k=1.
 * See {@link ACTMCPotatoErlang} for details.
 */
public class ACTMCPotatoExponential extends ACTMCPotatoErlang
{
	
	/** {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)} */
	public ACTMCPotatoExponential(ACTMCSimple actmc, GSMPEvent event, ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
		this.event.setSecondParameter(1);
	}
	
	public ACTMCPotatoExponential(ACTMCPotato other) {
		super(other);
		this.event.setSecondParameter(1);
	}
	
}