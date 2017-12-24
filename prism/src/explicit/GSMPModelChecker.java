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

import java.util.List;

import prism.PrismComponent;
import prism.PrismException;

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
	
	// forking methods
	
	/**
	 * Main method to initiate steady-state analysis computation.
	 * @param gsmp
	 * @param initDistr initial probability distribution on states. if null, default for the model is used
	 * @return steady state probability distribution on states
	 * @throws PrismException
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
	 * Main method to initiate transient analysis computation.
	 * @param gsmp
	 * @param time positive value
	 * @param initDistr initial probability distribution on states. if null, default for the model is used
	 * @return probability distribution on states at the specified time
	 * @throws PrismException
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

	// ACTMC model checking functions (fast alternative for GSMPs that are ACTMCs)

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
				// exponential events have not been merged.
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
	
	private StateValues computeSteadyStateACTMC(ACTMCSimple actmc, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		return buildInitialDistribution(actmc);
	}
	
	private StateValues computeTransientACTMC(ACTMCSimple actmc, double time, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		return buildInitialDistribution(actmc);
	}
	
	// general GSMP model checking functions (works for any GSMP, but slow)
	
	private StateValues computeSteadyStateGSMP(GSMP gsmp, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		return buildInitialDistribution(gsmp);
	}
	
	private StateValues computeTransientGSMP(GSMP gsmp, double time, StateValues initDistr) throws PrismException {
		// TODO MAJO - implement
		return buildInitialDistribution(gsmp);
	}

}
