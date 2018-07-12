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
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.BigDecimalUtils;
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
	
	/** Map where the keys are string identifiers of the GSMPEvents,
	 *  and the values are corresponding ACTMCPotatoData_poly structures.
	 *  This is useful for fast access and efficient reusage of the ACTMCPotatoData structures.*/
	protected Map<String, ACTMCPotato_poly> polyPDMap;
	/** True if we are minimizing the rewards. Otherwise, if maximizing, this variable is false. */
	protected boolean min;
	/** Verified list of event parameters to synthesize. */
	protected List<SynthParam> synthParams;
	/** Mapping of synthesis parameters onto states where they are active for convenience. */
	protected Map<Integer, SynthParam> paramMap = new HashMap<Integer, SynthParam>();
	/** Default ACTMC event map (eventMap from ACTMC) */
	protected Map<Integer, GSMPEvent> defaultEventMap;
	
	protected MathContext mc;
	
	
	/** {@link ACTMCReduction#ACTMCReduction(ACTMCSimple, ACTMCRewardsSimple, BitSet, boolean, PrismComponent)}
	 *  @param synthParams List of synthesis event parameters. Assumed to be verified and fully correct. */
	public ACTMCSymbolicParameterSynthesis(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, BitSet target,
			boolean computingSteadyState, PrismComponent parent, List<SynthParam> synthParams, boolean min) throws PrismException {
		super(actmc, actmcRew, target, computingSteadyState, parent);
		this.defaultEventMap = new HashMap<Integer, GSMPEvent>(actmc.getEventMap());
		this.synthParams = synthParams;
		this.min = min;
		
		// Construct paramMap
		int numStates = actmc.getNumStates();
		for (int s = 0 ; s < numStates ; ++s) {
			GSMPEvent event = actmc.getActiveEvent(s);
			if (event == null) {
				continue;
			}
			for (SynthParam param : synthParams) {
				if (event.getOriginalIdentifier().equals(param.getEventName())) {
					paramMap.put(s, param);
					break;
				}
			}
		}
		
		// Set kappa precision
		setKappa(deduceKappa());
	}
	
	/**
	 * Works out the exact value of kappa precision to use for the ACTMC analysis
	 * @return BigDecimal kappa
	 */
	protected BigDecimal deduceKappa() throws PrismException {
		BigDecimal kappa;
		if (computeKappa && !pdMap.isEmpty()) {
			kappa = BigDecimalUtils.min(computeKappa(), constantKappa);
		} else {
			kappa = constantKappa;
		}
		mc = new MathContext(BigDecimalUtils.decimalDigits(kappa) + 3, RoundingMode.HALF_UP);
		return kappa.divide(new BigDecimal("3", mc), mc);
	}
	
	/**
	 * Performs parameter synthesis for the given member variables.
	 * @return actmc events where the queried event parameters are kappa-optimal, mapped onto states of the actmc.
	 */
	public Map<Integer, GSMPEvent> reachabilityRewardParameterSynthesis() throws PrismException {
		// TODO MAJO - implement
		Map<Integer, GSMPEvent> arbitraryParams = chooseArbitraryParams();
		return null;
	}
	
	/**
	 * Returns a mapping of events onto states, where the events have some arbitrary parameters, 
	 * within boundaries specified by {@code synthParams}.
	 * @throws PrismException 
	 */
	private Map<Integer, GSMPEvent> chooseArbitraryParams() throws PrismException {
		Map<Integer, GSMPEvent> arbitraryParamMap = new HashMap<Integer, GSMPEvent>(defaultEventMap);
		List<GSMPEvent> events = new ArrayList<GSMPEvent>(arbitraryParamMap.values());
		for (SynthParam synthParam : synthParams) {
			
			GSMPEvent event = lookUpEvent(synthParam, events);
			if (event == null) {
				throw new PrismException("ACTMC Parameter synthesis error: failed to find matching event " + synthParam.getEventName());
			}

			if (synthParam.getParamIndex() == 1) {
				event.setFirstParameter(synthParam.getUpperBound() - synthParam.getLowerBound());
			}
			if (synthParam.getParamIndex() == 2) {
				event.setSecondParameter(synthParam.getUpperBound() - synthParam.getLowerBound());
			}
		}
		return arbitraryParamMap;
	}

	/**
	 * Creates a map where the keys are string identifiers of the GSMPEvents,
	 * and the values are corresponding ACTMCPotato_poly structures.
	 * The ACTMCPotato_poly structures will then be used for parameter synthesis of individual events.
	 * @param actmc ACTMC model for which to create the ACTMCPotato_poly structures
	 * @param rew Optional rewards associated with {@code actmc}. May be null, but calls
	 *            to {@code ACTMCPotato.getMeanReward()} will throw an exception!
	 */
	protected Map<String, ACTMCPotato_poly> createPolyPotatoDataMap(ACTMCSimple actmc,
			ACTMCRewardsSimple rew, BitSet target) throws PrismException {
		Map<String, ACTMCPotato_poly> pdMap = new HashMap<String, ACTMCPotato_poly>();
		List<GSMPEvent> events = actmc.getEventList();
		
		for (GSMPEvent event: events) {
			ACTMCPotato_poly potatoData;
			
			switch (event.getDistributionType().getEnum()) { //Symbolic parameter synthesis requires the "poly" implementations!
			case DIRAC:
				potatoData = new ACTMCPotatoDirac_polyTaylor(actmc, event, rew, target);
				break;
			case ERLANG:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis does not yet support the Erlang distribution!");
				// TODO MAJO - implement weibull distributed event support
				//break;
			case EXPONENTIAL:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis not yet support the Exponential distribution!");
				// TODO MAJO - implement weibull distributed event support
				//break;
			case UNIFORM:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis does not yet support the Weibull distribution!");
				// TODO MAJO - implement weibull distributed event support
				//break;
			case WEIBULL:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis does not yet support the Weibull distribution!");
				// TODO MAJO - implement weibull distributed event support
				//break;
			default:
				throw new PrismException("ACTMCSymbolicParameterSynthesis received an event with unrecognized distribution!");
			}
			
			pdMap.put(event.getIdentifier(), potatoData);
		}
		return pdMap;
	}
	
	/**
	 * Attempts to find the fitting GSMPEvent out of {@code events} for the given {@code SynthParam}.
	 * @param synthParam synthesis parameter
	 * @param events List of candidate events to search from
	 * @return GSMPEvent with original name equal to the synthParam event name. Null if not found.
	 */
	public GSMPEvent lookUpEvent(SynthParam synthParam, List<GSMPEvent> events) {
		for (GSMPEvent event : events) {
			if (synthParam.getEventName().equals(event.getOriginalIdentifier())) {
				return event;
			}
		}
		return null;
	}
	
}