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
import common.polynomials.Polynomial;
import common.polynomials.PolynomialRootFinding;
import explicit.rewards.ACTMCRewardsSimple;
import parser.ast.SynthParam;
import prism.PrismComponent;
import prism.PrismDevNullLog;
import prism.PrismException;

/**
 * Specialized class for carrying out symbolic parameter synthesis of ACTMCs.
 * Based on {@link ACTMCReduction}.
 */
public class ACTMCSymbolicParameterSynthesis extends ACTMCReduction
{
	
	/** Map where the keys are string identifiers of the GSMPEvents,
	 *  and the values are corresponding ACTMCPotato_poly structures.
	 *  This is useful for fast access and efficient reusage of the ACTMCPotato_poly structures.*/
	protected Map<String, ACTMCPotato_poly> polyPDMap;
	/** True if we are minimizing the rewards. Otherwise, if maximizing, this variable is false. */
	protected boolean min;
	/** Verified list of event parameters to synthesize. */
	protected List<SynthParam> synthParams;
	/** Default ACTMC event map (eventMap from ACTMC) */
	protected Map<Integer, GSMPEvent> defaultEventMap;
	/** Last computed Soln solution vector from e.g. computeReachRewards(). May be null. */
	protected double[] lastComputedSoln;
	
	protected MathContext mc;
	
	
	/** {@link ACTMCReduction#ACTMCReduction(ACTMCSimple, ACTMCRewardsSimple, BitSet, boolean, PrismComponent)}
	 *  @param synthParams List of synthesis event parameters. Assumed to be verified and fully correct. */
	public ACTMCSymbolicParameterSynthesis(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, BitSet target,
			boolean computingSteadyState, PrismComponent parent, List<SynthParam> synthParams, boolean min) throws PrismException {
		super(actmc, actmcRew, target, computingSteadyState, parent);
		this.defaultEventMap = copyEventMap(actmc.getEventMap());
		this.synthParams = synthParams;
		this.min = min;
		this.polyPDMap = createPolyPotatoDataMap(actmc, actmcRew, target);
		
		// Set kappa precision
		setKappa(deduceKappa());
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
	
	/**
	 * Attempts to find all fitting SynthParams out of {@code synthParams} for the given {@code event}.
	 * @param events event
	 * @param synthParam List of synthesis parameters to search from
	 * @return List of SynthParams with event names equal to the event original name. May be empty.
	 */
	public List<SynthParam> lookUpSynthParams(GSMPEvent event, List<SynthParam> synthParams) {
		List<SynthParam> result = new ArrayList<SynthParam>();
		for (SynthParam synthParam : synthParams) {
			if (synthParam.getEventName().equals(event.getOriginalIdentifier())) {
				result.add(synthParam);
			}
		}
		return result;
	}
	
	/**
	 * Performs parameter synthesis for the given member variables.
	 * <BR>
	 * NOTE: The ACTMC is expected to be strongly connected, with only localized alarms, and without unreachable states.
	 * @return actmc events where the queried event parameters are kappa-optimal, mapped onto states of the actmc.
	 */
	public Map<Integer, GSMPEvent> reachabilityRewardParameterSynthesis() throws PrismException {
		// set ACTMC event parameters to the their upper synthesis bounds to ensure enough precision
		Map<Integer, GSMPEvent> params = chooseUpperBoundParams();
		Map<Integer, GSMPEvent> newParams = chooseUpperBoundParams();
		Map<Integer, GSMPEvent> tmp;
		actmc.setEventParameters(newParams);
		
		// adjust local MathContext
		setKappa(deduceKappa());
		getMinimumKappaAndSetMC();
		
		do {
			actmc.setEventParameters(newParams);
			tmp = params;
			params = newParams;
			newParams = tmp;
			// POLICY EVALUATION
			Map<Integer, Double> reachRewards = computeReachRewards();
			if (reachRewards.containsValue(new Double(Double.POSITIVE_INFINITY))) {
				return actmc.getEventMap();
			}
			
			// POLICY IMPROVEMENT
			for (ACTMCPotato_poly actmcPotatoData : polyPDMap.values()) {
				// create symbolic polynomial
				int entrance = (int)actmcPotatoData.getEntrances().toArray()[0]; // event entrance state
				Polynomial symbolicPolynomial = new Polynomial();
				symbolicPolynomial.add(actmcPotatoData.getMeanRewardsBeforeEventPolynomials().get(entrance), mc);
				for (Map.Entry<Integer, Double> entry : reachRewards.entrySet()) {
					int state = entry.getKey();
					double reachRew = entry.getValue();
					Polynomial poly = actmcPotatoData.meanDistributionsPolynomials.get(entrance).get(state);
					if (poly == null) {
						continue;
					}
					poly = new Polynomial(actmcPotatoData.getMeanDistributionsPolynomials().get(entrance).get(state).coeffs);
					
					poly.multiplyWithScalar(new BigDecimal(reachRew, mc), mc);
					symbolicPolynomial.add(poly, mc);
				}
				
				//find relevant SynthParams
				List<SynthParam> eventSPs = lookUpSynthParams(actmcPotatoData.getEvent(), synthParams);
				if (eventSPs.isEmpty()) {
					continue; // TODO MAJO - is this OK or not ?
					//throw new PrismException("ACTMCSymbolicParameterSynthesis:findDerivPolyRoots could not find requested synthParams!");
				}
				// TODO MAJO - which parameter index to care about? Taking the first one, for now.
				// TODO MAJO - duplicates within SynthParams will be harmful here! Taking the first one, for now.
				eventSPs.removeIf(sp-> sp.getParamIndex() != 1);
				
				//find roots of the derivation of the symbolic polynomial
				List<BigDecimal> roots = findDerivPolyRoots(symbolicPolynomial, eventSPs);
				
				//Make a list of candidate optimal parameters
				List<BigDecimal> candidates = new ArrayList<BigDecimal>(roots);
				candidates.add(new BigDecimal(String.valueOf(eventSPs.get(0).getLowerBound()), mc));
				candidates.add(new BigDecimal(String.valueOf(eventSPs.get(0).getUpperBound()), mc));
				
				//evaluate the polynomial for the candidates and find min/max
				//first, try the current one
				BigDecimal bestParam = new BigDecimal(params.get(entrance).getFirstParameter(), mc);
				BigDecimal bestEvaluatedCandidate = symbolicPolynomial.value(bestParam, mc);
				// TODO MAJO - this only works for the first parameter for now!
				for (BigDecimal candidate : candidates) {
					BigDecimal evaluatedCandidate = symbolicPolynomial.value(candidate, mc);
					if (this.min) {
						if (evaluatedCandidate.compareTo(bestEvaluatedCandidate) < 0) {
							bestEvaluatedCandidate = evaluatedCandidate;
							bestParam = candidate;
						}
					} else {
						if (evaluatedCandidate.compareTo(bestEvaluatedCandidate) > 0) {
							bestEvaluatedCandidate = evaluatedCandidate;
							bestParam = candidate;
						}
					}
				}
				
				//Lastly, just set the newly found best parameter
				//TODO MAJO - this only works for the first parameter for now!
				newParams.get(entrance).setFirstParameter(bestParam.doubleValue());
			}
			
		} while(!params.equals(newParams));
		
		//Put the actmc events back in order
		actmc.setEventParameters(defaultEventMap);
		
		return newParams;
	}
	
	/**
	 * Return the contents of {@link ACTMCSymbolicParameterSynthesis#lastComputedSoln},
	 * i.e. an array holding the results of the related computation.
	 */
	public double[] getLastComputedSoln() {
		return lastComputedSoln;
	}

	
	/**
	 * Extended version of {@link ACTMCPotato#setKappa(BigDecimal)}
	 * that also does the same to the polyPDMap member variable.
	 */
	@Override
	protected void setKappa(BigDecimal kappa) {
		super.setKappa(kappa);
		for (Map.Entry<String, ACTMCPotato_poly> pdEntry : polyPDMap.entrySet()) {
			pdEntry.getValue().setKappa(kappa);
		}
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
	 * Returns a hard-copy mapping of events onto states where they are active,
	 * where the events parameters are equal to their upper synthesis bounds specified by {@code synthParams}.
	 */
	private Map<Integer, GSMPEvent> chooseUpperBoundParams() throws PrismException {
		Map<Integer, GSMPEvent> upperBoundEventMap = copyEventMap(defaultEventMap);		
		List<GSMPEvent> events = new ArrayList<GSMPEvent>(upperBoundEventMap.values());
		for (SynthParam synthParam : synthParams) {
			
			GSMPEvent event = lookUpEvent(synthParam, events);
			if (event == null) {
				throw new PrismException("ACTMC Parameter synthesis error: failed to find matching event " + synthParam.getEventName());
			}

			if (synthParam.getParamIndex() == 1) {
				event.setFirstParameter(synthParam.getUpperBound());
			}
			if (synthParam.getParamIndex() == 2) {
				event.setSecondParameter(synthParam.getUpperBound());
			}
		}
		return upperBoundEventMap;
	}
	
	/**
	 * Makes a hard copy of the {@code eventMap} (events mapped onto states).
	 */
	private Map<Integer, GSMPEvent> copyEventMap(Map<Integer, GSMPEvent> eventMap) {
		Map<Integer, GSMPEvent> newEventMap = new HashMap<Integer, GSMPEvent>(eventMap.size());
		for (Map.Entry<Integer, GSMPEvent> entry : eventMap.entrySet()) {
			int state = entry.getKey();
			GSMPEvent event = new GSMPEvent(entry.getValue());
			newEventMap.put(state, event);
		}
		return newEventMap;
	}
	
	/**
	 * Computes and returns reachability rewards for the current actmc.
	 * The reachability results are organized into a map where the
	 * keys are states and the values are reachability rewards.
	 * Last computed rewards are also stored as a member variable!
	 */
	private Map<Integer, Double> computeReachRewards() throws PrismException {
		GSMPModelChecker modelChecker = new GSMPModelChecker(this);
		modelChecker.setLog(new PrismDevNullLog());
		ModelCheckerResult res = modelChecker.computeReachRewardsACTMC(actmc, actmcRew, target);
		lastComputedSoln = res.soln;
		
		Map<Integer, Double> resMap = new HashMap<Integer, Double>();
		for (int i = 0; i < res.soln.length ; ++i) {
			resMap.put(i, res.soln[i]);
		}
		return resMap;
	}
	
	/**
	 * Explores polyPDMap to find the lowest kappa (highest precision).
	 * Then, MathContext this.mc is adjusted to it and the lowest kappa is returned.
	 * <br>
	 * NOTE: This method is important to set the MathContext properly!
	 * @throws PrismException 
	 */
	private BigDecimal getMinimumKappaAndSetMC() throws PrismException {
		BigDecimal lowestKappa = deduceKappa();
		for (ACTMCPotato_poly actmcPotatoData : polyPDMap.values()) {
			BigDecimal kappa = actmcPotatoData.getKappa();
			if (kappa == null) {
				throw new PrismException("ACTMCSymbolicParameterSynthesis.getMinimumKappa: kappa not yet set for the potato!");
			}
			if (kappa.compareTo(lowestKappa) < 0) {
				lowestKappa = kappa;
			}
		}
		mc = new MathContext(BigDecimalUtils.decimalDigits(lowestKappa) + 3, RoundingMode.HALF_UP);
		return lowestKappa;
	}
	
	/**
	 * Finds roots of the derivative of the given polynomial belonging to a given event and entrance state.
	 * The event and entrance are only used to find the upper/lower bounds of the roots.
	 * @param poly polynomial
	 * @param eventSPs synthesis parameter structures relevant to the given polynomial
	 * @return List of roots of the derivative of {@code poly}
	 */
	private List<BigDecimal> findDerivPolyRoots(Polynomial poly, List<SynthParam> eventSPs) throws PrismException {
		double lb = eventSPs.iterator().next().getLowerBound();
		double ub = eventSPs.iterator().next().getUpperBound();
		BigDecimal lowerBound = new BigDecimal(String.valueOf(lb), mc);
		BigDecimal upperBound = new BigDecimal(String.valueOf(ub), mc);
		
		Polynomial derivative = poly.derivative(mc);
		//TODO MAJO - this one didn't work for me!
		//List<BigDecimal> roots = PolynomialRootFinding.findRootsVAS(derivative, BigDecimalUtils.allowedError(mc.getPrecision()));
		List<BigDecimal> roots = PolynomialRootFinding.findRootsInIntervalVCAHalley(
				derivative,
				lowerBound,
				upperBound,
				BigDecimalUtils.allowedError(mc.getPrecision()));
		
		List<BigDecimal> boundedRoots = new ArrayList<BigDecimal>(roots);
		boundedRoots.removeIf(root-> root.compareTo(lowerBound) < 0 || root.compareTo(upperBound) > 0);
		
		return boundedRoots;
	}
	
}