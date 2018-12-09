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
import java.util.BitSet;
import java.util.Set;

import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * See parent class documentation for more basic info. {@link ACTMCPotato}
 * <br>
 * This extension implements high-precision precomputation
 * of Dirac-distributed potatoes using class BigDecimal.
 * <br>
 * HOW IT'S DONE:
 * Dirac distribution has one parameter, timeout t.
 * The data is computed directly for the given timeout t
 * instead of computing a general polynomial P(t), so it is more efficient.
 * Basically, these algorithms are high-precision BigDecimal versions of
 * {@link CTMCModelChecker#computeTransientProbs(CTMC, double, double[])} and
 * {@link CTMCModelChecker#computeCumulativeRewards(CTMC, explicit.rewards.MCRewards, double)}.
 */
public class ACTMCPotatoDirac_direct extends ACTMCPotato // TODO MAJO - this class should use BigDecimal arithmetics internally!
{
	/** {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)} */
	public ACTMCPotatoDirac_direct(ACTMCSimple actmc, GSMPEvent event, ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
	}
	
	public ACTMCPotatoDirac_direct(ACTMCPotato other) {
		super(other);
	}

	@Override
	protected void computeFoxGlynn() throws PrismException {
		if (!potatoDTMCComputed) {
			computePotatoDTMC();
		}
		
		if (kappa == null) {
			// Precision must be specified by setKappa()
			throw new PrismException("No precision specified for FoxGlynn!");
		}
		
		BigDecimal fgRate = new BigDecimal(String.valueOf(uniformizationRate), mc).multiply(new BigDecimal(String.valueOf(event.getFirstParameter()), mc), mc);
		foxGlynn = new FoxGlynn_BD(fgRate, new BigDecimal(1e-300), new BigDecimal(1e+300), kappa);
		if (foxGlynn.getRightTruncationPoint() < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation of the Poisson distribution!");
		}
		
		foxGlynnComputed = true;
	}

	@Override
	protected void computeMeanTimes() throws PrismException {
		if (!foxGlynnComputed) {
			computeFoxGlynn();
		}
		
		int numStates = potatoDTMC.getNumStates();
		
		// Prepare the FoxGlynn data
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		BigDecimal[] weights_BD = foxGlynn.getWeights().clone();
		BigDecimal totalWeight_BD = foxGlynn.getTotalWeight();
		for (int i = left; i <= right; i++) {
			weights_BD[i - left] = weights_BD[i - left].divide(totalWeight_BD, mc);
		}
		for (int i = left+1; i <= right; i++) {
			weights_BD[i - left] = weights_BD[i - left].add(weights_BD[i - 1 - left], mc);
		}
		for (int i = left; i <= right; i++) {
			weights_BD[i - left] = (BigDecimal.ONE.subtract(weights_BD[i - left], mc)).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc);
		}
		double[] weights = new double[weights_BD.length];
		for (int i = 0 ; i < weights.length ; ++i) {
			weights[i] = weights_BD[i].doubleValue();
		}
		
		for (int entrance : entrances) {
			
			// Prepare solution arrays
			double[] soln = new double[numStates];
			double[] soln2 = new double[numStates];
			double[] result = new double[numStates];
			double[] tmpsoln = new double[numStates];

			// Initialize the solution array by assigning reward
			// 1 to the entrance and 0 to all others.
			for (int i = 0; i < numStates; i++) {
				soln[i] = 0;
			}
			soln[ACTMCtoDTMC.get(entrance)] = 1;

			// do 0th element of summation (doesn't require any matrix powers)
			result = new double[numStates];
			if (left == 0) {
				for (int i = 0; i < numStates; i++) {
					result[i] += weights[0] * soln[i];
				}
			} else {
				for (int i = 0; i < numStates; i++) {
					result[i] += soln[i] / uniformizationRate;
				}
			}

			// Start iterations
			int iters = 1;
			while (iters <= right) {
				// Matrix-vector multiply
				potatoDTMC.vmMult(soln, soln2);
				// Swap vectors for next iter
				tmpsoln = soln;
				soln = soln2;
				soln2 = tmpsoln;
				// Add to sum
				if (iters >= left) {
					for (int i = 0; i < numStates; i++) {
						result[i] += weights[iters - left] * soln[i];
					}
				} else {
					for (int i = 0; i < numStates; i++) {
						result[i] += soln[i] / uniformizationRate;
					}
				}
				iters++;
			}
			
			// Store the sol vector using the original indexing for later use.
			Distribution solnDistr = new Distribution();
			for (int ps : potato) {
				double sol = soln[ACTMCtoDTMC.get(ps)];
				if (sol != 0.0) {
					solnDistr.add(ps, sol);
				}
			}
			meanTimesSoln.put(entrance, solnDistr);
			
			// Convert the result to a distribution with original indexing and store it.
			Distribution resultDistr = new Distribution();
			for (int ps : potato) {
				double time = result[ACTMCtoDTMC.get(ps)];
				if (time != 0.0) {
					resultDistr.add(ps, time);
				}
			}
			meanTimes.put(entrance, resultDistr);
		}
		meanTimesComputed = true;
	}
	
	@Override
	protected void computeMeanDistributions() throws PrismException {
		if (!foxGlynnComputed) {
			computeFoxGlynn();
		}
		
		int numStates = potatoDTMC.getNumStates();
		
		// Prepare the FoxGlynn data
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		BigDecimal[] weights_BD = foxGlynn.getWeights().clone();
		BigDecimal totalWeight_BD = foxGlynn.getTotalWeight();
		for (int i = left; i <= right; i++) {
			weights_BD[i - left] = weights_BD[i - left].divide(totalWeight_BD, mc);
		}
		double[] weights = new double[weights_BD.length];
		for (int i = 0 ; i < weights.length ; ++i) {
			weights[i] = weights_BD[i].doubleValue();
		}
		
		for (int entrance : entrances) {
			
			// Prepare solution arrays
			double[] initDist = new double[numStates];
			double[] soln;
			double[] soln2 = new double[numStates];
			double[] result = new double[numStates];
			double[] tmpsoln = new double[numStates];
			
			// Build the initial distribution for this potato entrance
			for (int s = 0; s < numStates  ; ++s) {
				initDist[s] = 0;
			}
			initDist[ACTMCtoDTMC.get(entrance)] = 1;
			soln = initDist;

			// Initialize the result array
			for (int i = 0; i < numStates; i++) {
				result[i] = 0.0;
			}

			// If necessary, compute the 0th element of summation
			// (doesn't require any matrix powers)
			if (left == 0) {
				for (int i = 0; i < numStates; i++) {
					result[i] += weights[0] * soln[i];
				}
			}

			// Start iterations
			int iters = 1;
			while (iters <= right) {
				// Matrix-vector multiply
				potatoDTMC.vmMult(soln, soln2);
				// Swap vectors for next iter
				tmpsoln = soln;
				soln = soln2;
				soln2 = tmpsoln;
				// Add to sum
				if (iters >= left) {
					for (int i = 0; i < numStates; i++) {
						result[i] += weights[iters - left] * soln[i];
					}
				}
				iters++;
			}
			
			// Store the sol vector using the original indexing for later use.
			Distribution solnDistr = new Distribution();
			for (int ps : potato) {
				double sol = soln[ACTMCtoDTMC.get(ps)];
				if (sol != 0.0) {
					solnDistr.add(ps, sol);
				}
			}
			meanDistributionsSoln.put(entrance, solnDistr);
			
			// Store the result vector for later use.
			Distribution resultBeforeEvent = new Distribution();
			for(int i = 0; i < numStates ; ++i ) {
				resultBeforeEvent.add(DTMCtoACTMC.get(i), result[i]);
			}
			meanDistributionsBeforeEvent.put(entrance, resultBeforeEvent);
			
			// Lastly, if there is some probability that the potatoDTMC would 
			// still be within the potato at the time of the event occurrence,
			// these probabilities must be redistributed into the successor states
			// using the event-defined distribution on states.
			// (I.e. the actual event behavior is applied)
			tmpsoln = result.clone();
			for ( int ps : potato) {
				result[ACTMCtoDTMC.get(ps)] = 0;
			}
			for ( int ps : potato) {
				int psIndex = ACTMCtoDTMC.get(ps);
				if (tmpsoln[psIndex] > 0) {
					Distribution distr = event.getTransitions(ps);
					Set<Integer> distrSupport = distr.getSupport();
					for ( int successor : distrSupport) {
						result[ACTMCtoDTMC.get(successor)] += tmpsoln[psIndex] * distr.get(successor);
					}
				}
			}
			
			// Normalize the result array (it may not sum to 1 due to inaccuracy).
			double probSum = 0;
			for (int succState : successors) {
				probSum += result[ACTMCtoDTMC.get(succState)];
			}
			// Convert the result to a distribution with original indexing and store it.
			Distribution resultDistr = new Distribution();
			for (int succState : successors) {
				double prob = result[ACTMCtoDTMC.get(succState)];
				if (prob != 0.0) {
					resultDistr.add(succState, prob / probSum); 
				}
			}
			meanDistributions.put(entrance, resultDistr);
		}
		meanDistributionsComputed = true;
	}
	
	@Override
	protected void computeMeanRewards() throws PrismException {
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		
		int numStates = potatoDTMC.getNumStates();
		
		// Prepare the FoxGlynn data
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		BigDecimal[] weights_BD = foxGlynn.getWeights().clone();
		BigDecimal totalWeight_BD = foxGlynn.getTotalWeight();
		for (int i = left; i <= right; i++) {
			weights_BD[i - left] = weights_BD[i - left].divide(totalWeight_BD, mc);
		}
		for (int i = left+1; i <= right; i++) {
			weights_BD[i - left] = weights_BD[i - left].add(weights_BD[i - 1 - left], mc);
		}
		for (int i = left; i <= right; i++) {
			weights_BD[i - left] = (BigDecimal.ONE.subtract(weights_BD[i - left], mc)).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc);
		}
		double[] weights = new double[weights_BD.length];
		for (int i = 0 ; i < weights.length ; ++i) {
			weights[i] = weights_BD[i].doubleValue();
		}
		
		// Prepare solution arrays
		double[] soln = new double[numStates];
		double[] soln2 = new double[numStates];
		double[] result = new double[numStates];
		double[] tmpsoln = new double[numStates];

		// Initialize the solution array by assigning rewards to the potato states
		for (int s = 0; s < numStates; s++) {
			int index = DTMCtoACTMC.get(s);
			if (potato.contains(index)) {
				soln[s] = rewards.getMergedStateReward(index);
			} else {
				soln[s] = 0;
			}
		}

		// do 0th element of summation (doesn't require any matrix powers)
		result = new double[numStates];
		if (left == 0) {
			for (int i = 0; i < numStates; i++) {
				result[i] += weights[0] * soln[i];
			}
		} else {
			for (int i = 0; i < numStates; i++) {
				result[i] += soln[i] / uniformizationRate;
			}
		}

		// Start iterations
		int iters = 1;
		while (iters <= right) {
			// Matrix-vector multiply
			potatoDTMC.mvMult(soln, soln2, null, false);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			// Add to sum
			if (iters >= left) {
				for (int i = 0; i < numStates; i++) {
					result[i] += weights[iters - left] * soln[i];
				}
			} else {
				for (int i = 0; i < numStates; i++) {
					result[i] += soln[i] / uniformizationRate; 
				}
			}
			iters++;
		}
		
		// Store the sol vector using the original indexing for later use.
		for (int ps : potato) {
			double sol = soln[ACTMCtoDTMC.get(ps)];
			if (sol != 0.0) {
				meanRewardsSoln.add(ps, sol);
			}
		}
		
		// Store the rewards just before the event behavior using the original indexing for later use.
		for (int entrance : entrances) {
			meanRewardsBeforeEvent.add(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		//Now that we have the expected rewards for the underlying CTMC behavior,
		//event behavior is applied.
		applyEventRewards(result, false);
		
		// Store the finalized expected rewards using the original indexing.
		for (int entrance : entrances) {
			meanRewards.add(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		meanRewardsComputed = true;
	}

}