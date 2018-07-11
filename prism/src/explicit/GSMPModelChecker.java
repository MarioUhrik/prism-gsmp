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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import explicit.rewards.ACTMCRewardsSimple;
import explicit.rewards.GSMPRewards;
import explicit.rewards.GSMPRewardsSimple;
import explicit.rewards.MCRewards;
import parser.ast.SynthParam;
import parser.type.TypeDistributionExponential;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Explicit-state model checker for generalized semi-Markov processes (GSMPs).
 * ACTMCs are treated as special cases, and solved differently.
 */
public class GSMPModelChecker extends ProbModelChecker
{
	/**
	 * Create a new GSMPModelChecker, inherit basic state from parent (unless null).
	 */
	public GSMPModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}
	
	// model checking initiation methods
	
	/**
	 * Gateway method to initiate computation of steady-state probabilities
	 * @param gsmp
	 * @param initDistr initial probability distribution on states. if null, default for the model is used
	 * @return steady state probability distribution on states
	 */
	public StateValues doSteadyState(GSMP gsmp, StateValues initDistr) throws PrismException {
		if (initDistr == null) {
			initDistr = buildInitialDistribution(gsmp);
		}
		if (isACTMC(gsmp) && gsmp instanceof GSMPSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			return computeSteadyStateACTMC(actmc, initDistr);
		} else {
			return computeSteadyStateGSMP(gsmp, initDistr);
		}
	}
	
	/**
	 * Gateway method to initiate computation of transient probabilities
	 * @param gsmp
	 * @param time positive value
	 * @param initDistr initial probability distribution on states. if null, default for the model is used
	 * @return probability distribution on states at the specified time
	 */
	public StateValues doTransient(GSMP gsmp, double time, StateValues initDistr) throws PrismException {
		if (initDistr == null) {
			initDistr = buildInitialDistribution(gsmp);
		}
		if (isACTMC(gsmp) && gsmp instanceof GSMPSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			return computeTransientACTMC(actmc, time, initDistr);
		} else {
			return computeTransientGSMP(gsmp, time, initDistr);
		}
	}
	
	/**
	 * Gateway method to initiate computation of expected reachability rewards
	 * @param gsmp the GSMP model
	 * @param rew the rewards structure belonging to the model
	 * @param target bitset of target states
	 * @return expected rewards accumulated before reaching a state in {@code target}
	 */
	public ModelCheckerResult doReachRewards(GSMP gsmp, GSMPRewards rew, BitSet target) throws PrismException {
		if (isACTMC(gsmp) && gsmp instanceof GSMPSimple && rew instanceof GSMPRewardsSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			ACTMCRewardsSimple actmcRew = new ACTMCRewardsSimple((GSMPRewardsSimple)rew, gsmp);
			actmcRew = actmcRew.mergeCTMCTransitionRewards(actmc);
			return computeReachRewardsACTMC(actmc, actmcRew, target);
		} else {
			return computeReachRewardsGSMP(gsmp, rew, target);
		}
	}

	/**
	 * Gateway method to initiate computation of expected steady-state rewards
	 * Model default initial distribution is used!
	 * @param gsmp the GSMP model
	 * @param rew the rewards structure belonging to the model
	 * @return expected long-run average reward
	 */
	public ModelCheckerResult doSteadyStateRewards(GSMP gsmp, GSMPRewards rew) throws PrismException {
		if (isACTMC(gsmp) && gsmp instanceof GSMPSimple && rew instanceof GSMPRewardsSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			ACTMCRewardsSimple actmcRew = new ACTMCRewardsSimple((GSMPRewardsSimple)rew, gsmp);
			actmcRew = actmcRew.mergeCTMCTransitionRewards(actmc);
			return computeSteadyStateRewardsACTMC(actmc, actmcRew);
		} else {
			return computeSteadyStateRewardsGSMP(gsmp, rew);
		}
	}

	/**
	 * Gateway method to initiate computation of optimal parameters for expected reachability rewards
	 * @param gsmp the GSMP model
	 * @param rew the rewards built for the GSMP model
	 * @param target target states
	 * @param min true iff we are minimizing the expected rewards. Else we are maximizing them.
	 * @param paramList list of parameters to synthesize. Extracted from ProbModelChecker.
	 * @return //TODO MAJO - what should this return anyway? optimal list of parameter values?
	 */ // TODO MAJO - javadoc return value
	public ModelCheckerResult doReachParameterSynthesis(GSMP gsmp, GSMPRewards rew, BitSet target, boolean min) throws PrismException {
		List<SynthParam> paramList = new ArrayList<SynthParam>(getParamList());
		boolean isACTMC = isACTMC(gsmp);
		validateParamList(paramList, gsmp, isACTMC);
		
		if (isACTMC && gsmp instanceof GSMPSimple && rew instanceof GSMPRewardsSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			ACTMCRewardsSimple actmcRew = new ACTMCRewardsSimple((GSMPRewardsSimple)rew, gsmp);
			// TODO MAJO - process transition rewards?
			return computeReachParameterSynthesisACTMC(actmc, actmcRew, target, min, paramList);
		} else {
			return computeReachParameterSynthesisGSMP(gsmp, rew, target, min, paramList);
		}
	}

	/**
	 * Gateway method to initiate computation of optimal parameters for expected steady-state rewards
	 * @param gsmp the GSMP model
	 * @param rew the rewards built for the GSMP model
	 * @param min true iff we are minimizing the expected rewards. Else we are maximizing them.
	 * @param paramList list of parameters to synthesize. Extracted from ProbModelChecker.
	 * @return //TODO MAJO - what should this return anyway? optimal list of parameter values?
	 */ // TODO MAJO - javadoc return value
	public ModelCheckerResult doSteadyStateParameterSynthesis(GSMP gsmp, GSMPRewards rew, boolean min) throws PrismException {
		List<SynthParam> paramList = new ArrayList<SynthParam>(getParamList());
		boolean isACTMC = isACTMC(gsmp);
		validateParamList(paramList, gsmp, isACTMC);
		
		if (isACTMC && gsmp instanceof GSMPSimple && rew instanceof GSMPRewardsSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			ACTMCRewardsSimple actmcRew = new ACTMCRewardsSimple((GSMPRewardsSimple)rew, gsmp);
			// TODO MAJO - process transition rewards?
			return computeSteadyStateParameterSynthesisACTMC(actmc, actmcRew, min, paramList);
		} else {
			return computeSteadyStateParameterSynthesisGSMP(gsmp, rew, min, paramList);
		}
	}

	/**
	 * Prerequisite test for ACTMC-only model-checking methods.
	 * @param gsmp
	 * @return  True iff the provided GSMP is an ACTMC (Alarm Continuous-Time Markov Chain),
	 * 			i.e. at most one non-exponential event is active in any given state.
	 */
	public boolean isACTMC(GSMP gsmp) {
		int numStates = gsmp.getNumStates();
		for ( int s = 0 ; s < numStates ; ++s) {
			List<GSMPEvent> activeEvents = gsmp.getActiveEvents(s);
			boolean foundNonExponentialEvent = false;
			for (GSMPEvent activeEvent : activeEvents) {
				if (!activeEvent.isExponential()) {
					// a non-exponential event active in this state has been found
					if (foundNonExponentialEvent) {
						// this is the second active non-exponential event => not an ACTMC
						return false;
					} else {
						// this is the first active non-exponential event so far => still tolerable
						foundNonExponentialEvent = true;
					}
				}
			}
		}
		mainLog.println("The model was recognized to be an ACTMC, so ACTMC method will be used!");
		return true;
	}

	/**
	 * Checks and possibly fixes the list of synthesis parameters so that it is ready for further usage.
	 * If there are unfixable problems with the list, exception is thrown.
	 * @param paramList list of synthesis parameters to validate
	 * @param gsmp reference to the model associated with the paramList
	 * @param isACTMC True iff the gsmp model fits the definition of ACTMCs
	 * @throws PrismException If the parameter list has unfixable problems.
	 */
	private void validateParamList(List<SynthParam> paramList, GSMP gsmp, boolean isACTMC) throws PrismException {
		// TODO MAJO - think of more things to check. Maybe merge duplicates?
		
		if (paramList.isEmpty()) {
			throw new PrismException("Optimal parameter synthesis - list of parameters to synthesize is empty!");
		}
		
		List<GSMPEvent> events = gsmp.getEventList();
		for (SynthParam param : paramList) {
			for (GSMPEvent event : events) {
				if (event.getOriginalIdentifier().equals(param.getEventName())) {
					// TODO MAJO - fix this! exponential events should be allowed for synthesis!
					// TODO MAJO - remember to fix ACTMC constructor and ACTMCRew constructor as well.
					if (isACTMC && event.getDistributionType() instanceof TypeDistributionExponential) {
						throw new PrismException("ACTMC optimal parameter synthesis - all events in the list of parameters must be non-exponential!");
					}
					break;
				}
			}
		}
		
		mainLog.print("\nSynthesis parameter list is valid. Proceeding with the computation. The list is:\n" + paramList +  "\n");
	}

	// ACTMC model checking functions (fast alternative for GSMPs that are ACTMCs)
	
	private StateValues computeSteadyStateACTMC(ACTMCSimple actmc, StateValues initDistr) throws PrismException {
		long reduceTime = System.currentTimeMillis();
		// Initialize necessary data structures
		ACTMCReduction reduction = new ACTMCReduction(actmc, null, null, true, this);
		Map<String, ACTMCPotato> pdMap = reduction.getPotatoData();
		Map<Integer, Distribution> timesWithinPotatoes = new HashMap<Integer, Distribution>();
		for (Map.Entry<String, ACTMCPotato> pdEntry : pdMap.entrySet()) {
			timesWithinPotatoes.putAll(pdEntry.getValue().getMeanTimes());
		}
		
		// Reduce the ACTMC to an equivalent DTMC.
		DTMCSimple dtmc = reduction.getDTMC();
		
		reduceTime = System.currentTimeMillis() - reduceTime;
		long computeTime = System.currentTimeMillis();
		
		// Compute the steady-state distribution for the equivalent DTMC
		DTMCModelChecker mc = new DTMCModelChecker(this);
		StateValues result = mc.doSteadyState(dtmc, initDistr);
		
		// Lastly, in order to reintroduce non-regenerative states to the result,
		// the result is weighted by the average time spent in each state of the potato
		// for each given entrance.
		double[] weightedResult = new double[dtmc.getNumStates()];
		for (int s = 0 ; s < dtmc.getNumStates() ; ++s) {
			Distribution timeDistr = timesWithinPotatoes.get(s);
			if (timeDistr == null) {
				weightedResult[s] += result.valuesD[s];
			} else {
				double prob = result.valuesD[s];
				if (prob == 0) {
					continue; //optimization
				}
				double theta = timeDistr.sum();
				Set<Integer> distrSupport = timeDistr.getSupport();
				for ( int t : distrSupport) {
					weightedResult[t] += prob * (timeDistr.get(t) / theta);
				}
			}
		}
		result.valuesD = weightedResult;
		
		computeTime = System.currentTimeMillis() - computeTime;
		mainLog.println("\nReducing ACTMC to equivalent DTMC "
				+ "took " + reduceTime/1000.0 + " seconds.");
		mainLog.println("Computing steady-state probabilities for the equivalent DTMC "
				+ "took " + computeTime/1000.0 + " seconds.");
		
		return result;
	}
	
	private StateValues computeTransientACTMC(ACTMCSimple actmc, double time, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing transient analysis for ACTMCs is not yet implemented!");
	}
	
	private ModelCheckerResult computeReachRewardsACTMC(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, BitSet target) throws PrismException {
		long reduceTime = System.currentTimeMillis();
		// Initialize necessary data structures
		ACTMCReduction reduction = new ACTMCReduction(actmc, actmcRew, target, false, this);
		
		// Reduce the ACTMC to an equivalent DTMC.
		DTMCSimple dtmc = reduction.getDTMC();
		
		// Compute the new state reward values (including the event behavior)
		MCRewards dtmcRew = reduction.getDTMCRew();
		
		reduceTime = System.currentTimeMillis() - reduceTime;
		
		// Compute the reachability rewards for the equivalent DTMC
		DTMCModelChecker mc = new DTMCModelChecker(this);
		ModelCheckerResult result = mc.computeReachRewards(dtmc, dtmcRew, target);
		
		result.timeTaken += result.timePre;
		result.timePre = reduceTime/1000.0;
		mainLog.println("\nReducing ACTMC to equivalent DTMC "
				+ "took " + result.timePre + "seconds.");
		mainLog.println("Computing reachability rewards for the equivalent DTMC "
				+ "took " + result.timeTaken + "seconds.");
		
		return result;
	}
	
	private ModelCheckerResult computeSteadyStateRewardsACTMC(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew) throws PrismException {
		// TODO MAJO - reuse the code from Steady State probabilities
		long reduceTime = System.currentTimeMillis();
		// Initialize necessary data structures
		ACTMCReduction reduction = new ACTMCReduction(actmc, actmcRew, null, true, this);
		Map<String, ACTMCPotato> pdMap = reduction.getPotatoData();
		Map<Integer, Distribution> timesWithinPotatoes = new HashMap<Integer, Distribution>();
		for (Map.Entry<String, ACTMCPotato> pdEntry : pdMap.entrySet()) {
			timesWithinPotatoes.putAll(pdEntry.getValue().getMeanTimes());
		}
		
		// Reduce the ACTMC to an equivalent DTMC.
		DTMCSimple dtmc = reduction.getDTMC();
		
		// Compute the new state reward values (including the event behavior)
		MCRewards dtmcRew = reduction.getDTMCRew();
		
		reduceTime = System.currentTimeMillis() - reduceTime;
		long computeTime = System.currentTimeMillis();
		
		// Compute the steady-state distribution for the equivalent DTMC
		DTMCModelChecker mc = new DTMCModelChecker(this);
		StateValues result = mc.doSteadyState(dtmc, buildInitialDistribution(dtmc));
		
		// In order to reintroduce non-regenerative states to the result,
		// the result is weighted by the average time spent in each state of the potato
		// for each given entrance.
		double[] weightedResult = new double[dtmc.getNumStates()];
		for (int s = 0 ; s < dtmc.getNumStates() ; ++s) {
			Distribution timeDistr = timesWithinPotatoes.get(s);
			if (timeDistr == null) {
				weightedResult[s] += result.valuesD[s];
			} else {
				double prob = result.valuesD[s];
				if (prob == 0) {
					continue; //optimization
				}
				double theta = timeDistr.sum();
				Set<Integer> distrSupport = timeDistr.getSupport();
				for ( int t : distrSupport) {
					weightedResult[t] += prob * (timeDistr.get(t) / theta);
				}
			}
		}
		result.valuesD = weightedResult;
		
		double rewardSum = 0;
		// Weight the steady-state probabilities by the new state reward values
		for (int s = 0; s < dtmc.getNumStates() ; ++s) {
			double reward = result.valuesD[s] * dtmcRew.getStateReward(s);
			result.valuesD[s] = reward;
			rewardSum += reward;
		}
		
		//---------------------------------------------------------------
		// TODO MAJO - ugly workaround!
		// Since PRISM expects this method to return StateValues where
		// there is only one element containing the expected reward, I fold
		// the vector to compute the sum and then put this sum into each element.
		for (int s = 0; s < dtmc.getNumStates() ; ++s) {
			result.valuesD[s] = rewardSum;
		}
		//---------------------------------------------------------------
		
		computeTime = System.currentTimeMillis() - computeTime;
		mainLog.println("\nReducing ACTMC to equivalent DTMC "
				+ "took " + reduceTime/1000.0 + "seconds.");
		mainLog.println("Computing steady-state rewards for the equivalent DTMC "
				+ "took " + computeTime/1000.0 + "seconds.");
		
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = result.valuesD;
		res.timePre = reduceTime/1000.0;
		res.timeTaken = computeTime/1000.0;
		return res;
	}
	
	private ModelCheckerResult computeReachParameterSynthesisACTMC(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, BitSet target, boolean min, List<SynthParam> paramList) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Parameter synthesis via reachability rewards for ACTMCs is not yet implemented!");
	}
	
	private ModelCheckerResult computeSteadyStateParameterSynthesisACTMC(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, boolean min, List<SynthParam> paramList) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Parameter synthesis via steady-state rewards for ACTMCs is not yet implemented!");
	}
	
	// general GSMP model checking functions (works for any GSMP, but slow)
	
	private StateValues computeSteadyStateGSMP(GSMP gsmp, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing steady-state analysis for GSMPs is not yet implemented!");
	}
	
	private StateValues computeTransientGSMP(GSMP gsmp, double time, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing transient analysis for GSMPs is not yet implemented!");
	}
	
	private ModelCheckerResult computeReachRewardsGSMP(GSMP gsmp, GSMPRewards rew, BitSet target) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing reachability rewards for GSMPs is not yet implemented!");
	}
	
	private ModelCheckerResult computeSteadyStateRewardsGSMP(GSMP gsmp, GSMPRewards rew) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing steady-state rewards for GSMPs is not yet implemented!");
	}
	
	private ModelCheckerResult computeReachParameterSynthesisGSMP(GSMP gsmp, GSMPRewards rew, BitSet target, boolean min, List<SynthParam> paramList) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Parameter synthesis via reachability rewards for GSMPs is not yet implemented!");
	}
	
	private ModelCheckerResult computeSteadyStateParameterSynthesisGSMP(GSMP gsmp, GSMPRewards rew, boolean min, List<SynthParam> paramList) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Parameter synthesis via steady-state rewards for GSMPs is not yet implemented!");
	}

}
