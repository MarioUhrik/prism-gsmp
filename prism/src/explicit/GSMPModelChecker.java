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

import parser.type.TypeDistributionExponential;
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

	// ACTMC model checking functions

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
				if (activeEvent.getDistributionType() != TypeDistributionExponential.getInstance()) {
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

}
