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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import explicit.rewards.ACTMCRewardsSimple;
import explicit.rewards.GSMPRewards;
import explicit.rewards.GSMPRewardsSimple;
import parser.ast.SynthParam;
import parser.type.TypeDistributionExponential;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Explicit-state model checker for generalized semi-Markov processes (GSMPs).
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
			return computeReachRewardsACTMC(actmc, actmcRew, target);
		} else {
			return computeReachRewardsGSMP(gsmp, rew, target);
		}
	}

	/**
	 * Gateway method to initiate computation of expected steady-state rewards
	 * @param gsmp the GSMP model
	 * @param rew the rewards structure belonging to the model
	 * @return expected long-run rewards
	 */
	public ModelCheckerResult doSteadyStateRewards(GSMP gsmp, GSMPRewards rew) throws PrismException {
		if (isACTMC(gsmp) && gsmp instanceof GSMPSimple && rew instanceof GSMPRewardsSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			ACTMCRewardsSimple actmcRew = new ACTMCRewardsSimple((GSMPRewardsSimple)rew, gsmp);
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
	 * @return //TODO MAJO - which should this return anyway? optimal list of parameter values?
	 */ // TODO MAJO - javadoc return value
	public ModelCheckerResult doReachParameterSynthesis(GSMP gsmp, GSMPRewards rew, BitSet target, boolean min) throws PrismException {
		List<SynthParam> paramList = new ArrayList<SynthParam>(getParamList());
		boolean isACTMC = isACTMC(gsmp);
		validateParamList(paramList, gsmp, isACTMC);
		
		if (isACTMC && gsmp instanceof GSMPSimple && rew instanceof GSMPRewardsSimple) {
			ACTMCSimple actmc = new ACTMCSimple((GSMPSimple)gsmp);
			ACTMCRewardsSimple actmcRew = new ACTMCRewardsSimple((GSMPRewardsSimple)rew, gsmp);
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
				if (event.getIdentifier().contains(param.getEventName())) {
					param.setEventName(event.getIdentifier()); // update the event name, as they change during build time
					
					// TODO MAJO - should we restrict ACTMC synthesis parameters to non-exponential events?
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
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing steady state-analysis for ACTMCs is not yet implemented!");
	}
	
	private StateValues computeTransientACTMC(ACTMCSimple actmc, double time, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing transient analysis for ACTMCs is not yet implemented!");
	}
	
	private ModelCheckerResult computeReachRewardsACTMC(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, BitSet target) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing reachability rewards for ACTMCs is not yet implemented!");
	}
	
	private ModelCheckerResult computeSteadyStateRewardsACTMC(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew) throws PrismException {
		// TODO MAJO - implement
		throw new PrismNotSupportedException("Computing steady-state rewards for ACTMCs is not yet implemented!");
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
