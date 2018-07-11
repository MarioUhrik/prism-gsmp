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
import java.util.List;
import java.util.Map;

import explicit.rewards.ACTMCRewardsSimple;
import parser.ast.SynthParam;
import prism.PrismComponent;
import prism.PrismException;

/**
 * Specialized class for carrying out symbolic parameter synthesis of ACTMCs.
 * Based on {@link ACTMCReduction}.
 */
public class ACTMCSymbolicParameterSynthesis extends ACTMCReduction
{
	
	/** List of event parameters to synthesize. */
	List<SynthParam> synthParams;
	
	/** {@link ACTMCReduction#ACTMCReduction(ACTMCSimple, ACTMCRewardsSimple, BitSet, boolean, PrismComponent)} */
	public ACTMCSymbolicParameterSynthesis(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, BitSet target, boolean computingSteadyState, PrismComponent parent) throws PrismException {
		super(actmc, actmcRew, target, computingSteadyState, parent);
	}
	

	/**
	 * Creates a map where the keys are string identifiers of the GSMPEvents,
	 * and the values are corresponding ACTMCPotato structures.
	 * The ACTMCPotato structures will then be used for parameter synthesis of individual events.
	 * @param actmc ACTMC model for which to create the ACTMCPotato structures
	 * @param rew Optional rewards associated with {@code actmc}. May be null, but calls
	 *            to {@code ACTMCPotato.getMeanReward()} will throw an exception!
	 */
	@Override
	protected Map<String, ACTMCPotato> createPotatoDataMap(ACTMCSimple actmc,
			ACTMCRewardsSimple rew, BitSet target) throws PrismException {
		Map<String, ACTMCPotato> pdMap = new HashMap<String, ACTMCPotato>();
		List<GSMPEvent> events = actmc.getEventList();
		
		for (GSMPEvent event: events) {
			ACTMCPotato potatoData;
			
			switch (event.getDistributionType().getEnum()) { //Parameter synthesis requires the "poly" implementations!
			case DIRAC:
				potatoData = new ACTMCPotatoDirac_poly(actmc, event, rew, target);
			case ERLANG:
				potatoData = new ACTMCPotatoErlang_poly(actmc, event, rew, target);
				break;
			case EXPONENTIAL:
				potatoData = new ACTMCPotatoExponential_poly(actmc, event, rew, target);
				break;
			case UNIFORM:
				potatoData = new ACTMCPotatoUniform_poly(actmc, event, rew, target);
				break;
			case WEIBULL:
				throw new UnsupportedOperationException("ACTMCReduction does not yet support the Weibull distribution!");
				// TODO MAJO - implement weibull distributed event support
				//break;
			default:
				throw new PrismException("ACTMCReduction received an event with unrecognized distribution!");
			}
			
			pdMap.put(event.getIdentifier(), potatoData);
		}
		return pdMap;
	}
	
}