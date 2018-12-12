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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.BigDecimalUtils;
import common.polynomials.Poly;
import common.polynomials.Polynomial;
import common.polynomials.PolynomialRootFinding;
import explicit.ProbModelChecker.TermCrit;
import explicit.rewards.ACTMCRewardsSimple;
import parser.ast.SynthParam;
import prism.PrismComponent;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismUtils;

/**
 * Specialized class for carrying out symbolic parameter synthesis of ACTMCs.
 * Based on {@link ACTMCReduction}.
 */
public class ACTMCSymbolicParameterSynthesis extends ACTMCReduction
{
	
	/** Map where the keys are string identifiers of the GSMPEvents that we are synthesizing,
	 *  and the values are corresponding ACTMCPotato_poly structures. */
	protected Map<String, ACTMCPotato_poly> polyPDMap;
	/** Map where the keys are string identifiers of the GSMPEvents that we are synthesizing,
	 *  and the values are corresponding synthesis parameter structures. */
	protected Map<String, List<SynthParam>> polySPMap;
	/** True if we are minimizing the rewards. Otherwise, if maximizing, this variable is false. */
	protected boolean min;
	/** Verified original list of event parameters to synthesize. */
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
		this.actmc = new ACTMCSimple(actmc); // use hard copy of the ACTMC, because we will be modifying it
		this.defaultEventMap = copyEventMap(actmc.getEventMap());
		this.synthParams = synthParams;
		this.min = min;
		this.polySPMap = new HashMap<String, List<SynthParam>>();
		this.polyPDMap = createPolyPDandSPMap(actmc, actmcRew, target);
		
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
	 * NOTE: The ACTMC is expected to be without unreachable states,
	 * and the synthesized events must be localized (with only one entrance).
	 * @return actmc events where the queried event parameters are kappa-optimal, mapped onto states of the actmc.
	 */
	public Map<Integer, GSMPEvent> reachabilityRewardParameterSynthesis() throws PrismException {
		// TODO MAJO - run a check to make sure the actmc has no unreachable states!
		ensureSynthesizedEventsAreLocalized();
		// set ACTMC event parameters to the their upper synthesis bounds to ensure enough precision
		Map<Integer, GSMPEvent> params = chooseUpperBoundParams();
		Map<Integer, GSMPEvent> newParams = chooseUpperBoundParams();
		actmc.setEventParameters(newParams);
		
		// adjust local MathContext
		setKappa(deduceKappa());
		getMinimumKappaAndSetMC();
		
		do {
			params = copyEventMap(newParams);
			actmc.setEventParameters(params);
			// POLICY EVALUATION
			Map<Integer, Double> reachRewards = computeReachRewards();
			for (int initState : actmc.getInitialStates()) {
				if (reachRewards.get(initState).isInfinite())
					return params; //return the current params, because it doesn't matter in this case
			}
			
			// POLICY IMPROVEMENT
			for (ACTMCPotato_poly actmcPotatoData : polyPDMap.values()) {
				//get relevant SynthParams
				List<SynthParam> eventSPs = polySPMap.get(actmcPotatoData.getEvent().getIdentifier());
				
				// create symbolic polynomial
				int entrance = (int)actmcPotatoData.getEntrances().toArray()[0]; // localized event entrance state
				Polynomial symbolicPolynomial = new Polynomial();
				symbolicPolynomial.add(actmcPotatoData.getMeanRewardsPolynomials().get(entrance), mc);
				for (int state = relevantStates.nextSetBit(0); state >= 0; state = relevantStates.nextSetBit(state+1)) {
				    double reachRew = reachRewards.get(state);
					Poly poly = (Polynomial)actmcPotatoData.getMeanDistributionsPolynomials().get(entrance).get(state);
					if (poly == null) {
						continue;
					}
					if (!(poly instanceof Polynomial)) {
						throw new PrismNotSupportedException("ACTMCParameterSynthesis does not yet support Polynomials"
								+ "with real exponents");
					}
					Polynomial polynomial = new Polynomial(((Polynomial)poly).coeffs);
					
					polynomial.multiplyWithScalar(new BigDecimal(String.valueOf(reachRew), mc), mc);
					symbolicPolynomial.add(polynomial, mc);
				}
				
				//find roots of the derivation of the symbolic polynomial
				List<BigDecimal> roots = findDerivPolyRoots(symbolicPolynomial, eventSPs);
				
				//Make a list of candidate optimal parameters
				List<BigDecimal> candidates = new ArrayList<BigDecimal>(roots);
				BigDecimal lowerBound = new BigDecimal(String.valueOf(eventSPs.get(0).getLowerBound()), mc);
				BigDecimal upperBound = new BigDecimal(String.valueOf(eventSPs.get(0).getUpperBound()), mc);
				candidates.add(lowerBound);
				candidates.add(upperBound);
				for (BigDecimal root : roots) {
					BigDecimal lRoot = root.subtract(epsilon, mc);
					BigDecimal rRoot = root.add(epsilon, mc);
					if (lRoot.compareTo(lowerBound) > 0 && lRoot.compareTo(upperBound) < 0) {
						candidates.add(lRoot);
					}
					if (rRoot.compareTo(lowerBound) > 0 && rRoot.compareTo(upperBound) < 0) {
						candidates.add(rRoot);
					}
				}
				
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
			
		} while(!parameterDifferenceWithinTerminationEpsilon(params, newParams));

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
	 * Creates and returns a map where the keys are string identifiers of the GSMPEvents
	 * whose parameters we are synthesizing, and the values are corresponding ACTMCPotato_poly structures.
	 * The ACTMCPotato_poly structures will then be used for parameter synthesis of individual events.
	 * Furthermore, fills up the {@link ACTMCSymbolicParameterSynthesis#polySPMap} with relevant data.
	 * @param actmc ACTMC model for which to create the ACTMCPotato_poly structures
	 * @param rew Optional rewards associated with {@code actmc}. May be null, but calls
	 *            to {@code ACTMCPotato.getMeanReward()} will throw an exception!
	 * @return {@link ACTMCSymbolicParameterSynthesis#polyPDMap} for the given parameters
	 */
	protected Map<String, ACTMCPotato_poly> createPolyPDandSPMap(ACTMCSimple actmc,
			ACTMCRewardsSimple rew, BitSet target) throws PrismException {
		Map<String, ACTMCPotato_poly> pdMap = new HashMap<String, ACTMCPotato_poly>();
		List<GSMPEvent> events = actmc.getEventList();
		
		for (GSMPEvent event: events) {
			//find relevant SynthParams
			List<SynthParam> eventSPs = lookUpSynthParams(event, synthParams);
			if (eventSPs.isEmpty()) {
				continue;
			}
			// TODO MAJO - which parameter index to care about? Taking the first one, for now.
			// TODO MAJO - duplicates within SynthParams will be harmful here! Taking the first one, for now.
			eventSPs.removeIf(sp-> sp.getParamIndex() != 1);
			this.polySPMap.put(event.getIdentifier(), eventSPs);
			
			ACTMCPotato_poly potatoData;
			
			switch (event.getDistributionType().getEnum()) { //Symbolic parameter synthesis requires the "poly" implementations!
			case DIRAC: // ... and preferably the polyTaylor implementations.
				potatoData = new ACTMCPotatoDirac_polyTaylor(actmc, event, rew, target);
				break;
			case ERLANG:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis does not yet support the Erlang distribution");
				// TODO MAJO - implement Erlang distributed event support
				//break;
			case EXPONENTIAL:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis not yet support the exponential distribution");
				// TODO MAJO - implement exponential distributed event support
				//break;
			case UNIFORM:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis does not yet support the uniform distribution");
				// TODO MAJO - implement uniform distributed event support
				//break;
			case WEIBULL:
				throw new UnsupportedOperationException("ACTMCSymbolicParameterSynthesis does not yet support the Weibull distribution");
				// TODO MAJO - implement Weibull distributed event support
				//break;
			default:
				throw new PrismException("ACTMCSymbolicParameterSynthesis received an event with unrecognized distribution");
			}
			
			pdMap.put(event.getIdentifier(), potatoData);
		}
		return pdMap;
	}
	
	/**
	 * Makes sure that the synthesized events of this.actmc are localized.
	 * <br>
	 * Events (or alarms) are localized iff they only have one state where the
	 * event/alarm timer is newly set.
	 * (i.e. ACTMCPotato deduces that the set of entrances {@link ACTMCPotato#entrances} is a singleton)
	 * @return returns true, otherwise throws an exception
	 * @throws PrismException
	 */
	protected boolean ensureSynthesizedEventsAreLocalized() throws PrismException {
		for (ACTMCPotato_poly actmcPotatoData : polyPDMap.values()) {
			if (actmcPotatoData.getEntrances().size() > 1) {
				throw new PrismException("ACTMC reachability reward parameter synthesis: event "
						+ actmcPotatoData.getEvent().getIdentifier()
						+ " has more than one entrance");
			}
			if (actmcPotatoData.getEntrances().size() < 1) {
				throw new PrismException("ACTMC reachability reward parameter synthesis: event "
						+ actmcPotatoData.getEvent().getIdentifier()
						+ " has more no entrances");
			}
		}
		return true;
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
		Set<GSMPEvent> eventSet = new HashSet<GSMPEvent>(eventMap.values());
		for (GSMPEvent otherEvent : eventSet) {
			GSMPEvent thisEvent = new GSMPEvent(otherEvent);
			BitSet active = thisEvent.getActive();
			for (int state = active.nextSetBit(0); state >= 0; state = active.nextSetBit(state+1)) {
				newEventMap.put(state, thisEvent);
			}
		}
		return newEventMap;
	}
	
	/**
	 * Returns true if all of the event parameters of the firstEM are within {@link ACTMCReduction#epsilon}
	 * of secondEM event parameters. This is used as a termination condition for the parameter synthesis.
	 * The comparison is done with respect to current PRISM settings of termination epsilon and termination criteria.
	 * firstEM and secondEM are maps where the keys are states are values are GSMPEvents active in the states.
	 * firstEM and secondEM are supposed to be equivalent EXCEPT of some difference in firstParameter and secondParameter.
	 * @param firstEM 
	 * @param secondEM
	 */
	private boolean parameterDifferenceWithinTerminationEpsilon(Map<Integer, GSMPEvent> firstEM, Map<Integer, GSMPEvent> secondEM) {
		// absolute or relative termination criteria ?
		boolean absolute = true;
		if (termCrit == TermCrit.RELATIVE) {
			absolute = false;
		}
		
		for (int state : firstEM.keySet()) {
			GSMPEvent first = firstEM.get(state);
			GSMPEvent second = secondEM.get(state);
			if (!PrismUtils.doublesAreClose(first.getFirstParameter(), second.getFirstParameter(), epsilon.doubleValue(), absolute)) {
				return false;
			}
			if (!PrismUtils.doublesAreClose(first.getSecondParameter(), second.getSecondParameter(), epsilon.doubleValue(), absolute)) {
				return false;
			}
		}
		return true;
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
		modelChecker.setTermCritParam(1.0E-15 * modelChecker.getTermCritParam()); // TODO MAJO - is this a good idea ?
		// TODO MAJO - I need to accurately compute this, but it often fails!
		modelChecker.setMaxIters(modelChecker.getMaxIters() + 100000000); // TODO MAJO - is this a good idea ?
		// TODO MAJO - I could optimize this by reusing some of the computed ACTMCPotato structures
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
				throw new PrismException("ACTMCSymbolicParameterSynthesis.getMinimumKappa: kappa not yet set for the potato");
			}
			if (kappa.compareTo(lowestKappa) < 0) {
				lowestKappa = kappa;
			}
		}
		mc = new MathContext(BigDecimalUtils.decimalDigits(lowestKappa) + 3 + BigDecimalUtils.decimalDigits(epsilon), RoundingMode.HALF_UP);
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