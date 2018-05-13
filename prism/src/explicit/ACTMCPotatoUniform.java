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
import java.util.Map;
import java.util.Set;

import ch.obermuhlner.math.big.BigDecimalMath;
import common.polynomials.Polynomial;
import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * Class for storage and computation of single uniformly distributed potato-related data for ACTMCs,
 * I.e. this implementation treats the event as uniformly distributed.
 * <br>
 * Potato is a subset of states of an ACTMC in which a given event is active.
 * <br><br>
 * This data is fundamental for ACTMC model checking methods based on reduction
 * of ACTMC to DTMC. The reduction works by pre-computing the expected behavior
 * (rewards, spent time, resulting distribution...) occurring between
 * entering and leaving a potato. Then, these expected values are used in
 * regular CTMC/DTMC model checking methods.
 */
public class ACTMCPotatoUniform extends ACTMCPotato
{
	private MathContext mc;
	
	/** {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)} */
	public ACTMCPotatoUniform(ACTMCSimple actmc, GSMPEvent event, ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
	}
	
	public ACTMCPotatoUniform(ACTMCPotato other) {
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
		
		BigDecimal fgRate = new BigDecimal(uniformizationRate); // Compute FoxGlynn only for the uniformization rate
		foxGlynn = new FoxGlynn_BD(fgRate, new BigDecimal(1e-300), new BigDecimal(1e+300), kappa);
		if (foxGlynn.getRightTruncationPoint() < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation of the Poisson distribution!");
		}
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		BigDecimal[] weights = foxGlynn.getWeights();
		
		//Get rid of the e^-lambda part, i.e. divide everything by e^-lambda
		mc = new MathContext(foxGlynn.getWeights()[0].precision(), RoundingMode.HALF_UP); // TODO MAJO - test precision
		BigDecimal factor = BigDecimalMath.exp(fgRate.negate(), mc);
		for (int i = left; i <= right; i++) {
			weights[i - left] = weights[i - left].divide(factor, mc);
		}
		
		foxGlynnComputed = true;
	}

	//TODO MAJO - implement
	@Override
	protected void computeMeanTimes() throws PrismException {
		throw new PrismException("Not yet implemented!");
		/*
		
		if (!foxGlynnComputed) {
			computeFoxGlynn();
		}
		
		int numStates = potatoDTMC.getNumStates();
		
		// Prepare the FoxGlynn data
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		///// Conversion from BigDecimal to Double!!! // TODO MAJO - convert EVERYTHING to BigDecimal
		BigDecimal[] weights_BD = foxGlynn.getWeights().clone();
		double[] weights = new double[weights_BD.length];
		for (int i = 0 ; i < weights.length ; ++i) {
			weights[i] = weights_BD[i].doubleValue();
		}
		BigDecimal totalWeight_BD = foxGlynn.getTotalWeight();
		double totalWeight = totalWeight_BD.doubleValue();
		/////
		for (int i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}
		for (int i = left+1; i <= right; i++) {
			weights[i - left] += weights[i - 1 - left];
		}
		for (int i = left; i <= right; i++) {
			weights[i - left] = (1 - weights[i - left]) / uniformizationRate;
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
					for (int i = 0; i < numStates; i++)
						result[i] += weights[iters - left] * soln[i];
				} else {
					for (int i = 0; i < numStates; i++)
						result[i] += soln[i] / uniformizationRate;
				}
				iters++;
			}
			
			// We are done. 
			// Convert the result to a distribution with original indexing and store it.
			Distribution resultDistr = new Distribution();
			for (int ps : potato) {
				double time = result[ACTMCtoDTMC.get(ps)];
				if (time != 0.0) {
					resultDistr.add(ps, Math.abs(time)); // TODO MAJO - remove this abs() eventually
				}
			}
			meanTimes.put(entrance, resultDistr);
		}
		meanTimesComputed = true;
		*/
	}
	
	//TODO MAJO - implement
	@Override
	protected void computeMeanDistributions() throws PrismException {
		
		if (!foxGlynnComputed) {
			computeFoxGlynn();
		}
		
		int numStates = potatoDTMC.getNumStates();
		
		// Prepare the FoxGlynn data
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		///// Conversion from BigDecimal to Double!!! // TODO MAJO - convert EVERYTHING to BigDecimal
		BigDecimal[] weights_BD = foxGlynn.getWeights().clone();
		double[] weights = new double[weights_BD.length];
		for (int i = 0 ; i < weights.length ; ++i) {
			weights[i] = weights_BD[i].doubleValue();
		}
		BigDecimal totalWeight_BD = foxGlynn.getTotalWeight();
		double totalWeight = totalWeight_BD.doubleValue();
		/////
		for (int i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}
		
		for (int entrance : entrances) {
			
			// Prepare solution arrays // TODO MAJO - optimize, reuse the arrays!
			double[] initDist = new double[numStates];
			double[] soln;
			double[] soln2 = new double[numStates];
			double[] result = new double[numStates];
			Polynomial[] polynomials = new Polynomial[numStates];
			Polynomial[] antiderivatives = new Polynomial[numStates];
			double[] tmpsoln = new double[numStates];
			
			// Build the initial distribution for this potato entrance
			for (int s = 0; s < numStates  ; ++s) {
				initDist[s] = 0;
			}
			initDist[ACTMCtoDTMC.get(entrance)] = 1;
			soln = initDist;

			// Initialize the result p
			for (int i = 0; i < numStates; i++) {
				polynomials[i] = new Polynomial(new ArrayList<BigDecimal>());
				antiderivatives[i] = new Polynomial(new ArrayList<BigDecimal>());
			}

			// If necessary, compute the 0th element of summation
			// (doesn't require any matrix powers)
			if (left == 0) {
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.add(0, new BigDecimal(weights[0] * soln[i]));
				}
			}

			// Compute the potatoDTMC solution polynomial just before the event occurs
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
					for (int i = 0; i < numStates; i++)
						polynomials[i].coeffs.add(iters - left, new BigDecimal(weights[iters - left] * soln[i]));
				}
				iters++;
			}
			
			//Compute antiderivatives of (e^(-lambda*time) * polynomial) for each polynomial using integration by parts
			for (int n = 0; n < numStates ; ++n) {
				Polynomial poly = polynomials[n];
				int polyDegree = poly.degree();
				
				Polynomial derivative = antiderivatives[n];
				for (int i = 0; i <= polyDegree ; i++) {
					Polynomial tmp = new Polynomial(new ArrayList<BigDecimal>(poly.coeffs));
					BigDecimal factor = new BigDecimal(-1/uniformizationRate);
					tmp.multiplyWithScalar(factor);
					factor = BigDecimalMath.pow(new BigDecimal(1/uniformizationRate), i, mc); // TODO MAJO - optimize
					tmp.multiplyWithScalar(factor);
					
					derivative.add(tmp);		
					poly = poly.derivative();
				}
			}
			
			//Compute the definite integral using the obtained antiderivative
			for (int n = 0; n < numStates ; ++n) {
				Polynomial antiderivative = antiderivatives[n];
				double a = event.getFirstParameter();
				double b = event.getSecondParameter();
				double aFactor = Math.exp(-uniformizationRate * a);
				double bFactor = Math.exp(-uniformizationRate * b);
				double aVal = antiderivative.value(new BigDecimal(a)).doubleValue() * aFactor;
				double bVal = antiderivative.value(new BigDecimal(b)).doubleValue() * bFactor;
				double prob = 1 / (b - a);
				
				result[n] = prob * (bVal - aVal);
			}
			
			
			// Store the DTMC solution vector for later use by other methods
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
			
			// We are done.
			// Normalize the result array (it may not sum to 1 due to inaccuracy).
			double probSum = 0;
			for (int succState : successors) {
				probSum += result[ACTMCtoDTMC.get(succState)];
			}
			// Convert the result to a distribution with original indexing and store it.
			Distribution resultDistr = new Distribution();
			for (int succState : successors) {
				double prob = result[ACTMCtoDTMC.get(succState)];
				resultDistr.add(succState, Math.abs(prob) / probSum); // TODO MAJO - remove this abs() eventually
			}
			meanDistributions.put(entrance, resultDistr);
		}
		meanDistributionsComputed = true;
	}
	
	//TODO MAJO - implement
	@Override
	protected void computeMeanRewards() throws PrismException {
		throw new PrismException("Not yet implemented!");
		/*
		
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		
		int numStates = potatoDTMC.getNumStates();
		
		// Prepare the FoxGlynn data
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		///// Conversion from BigDecimal to Double!!! // TODO MAJO - convert EVERYTHING to BigDecimal
		BigDecimal[] weights_BD = foxGlynn.getWeights().clone();
		double[] weights = new double[weights_BD.length];
		for (int i = 0 ; i < weights.length ; ++i) {
			weights[i] = weights_BD[i].doubleValue();
		}
		BigDecimal totalWeight_BD = foxGlynn.getTotalWeight();
		double totalWeight = totalWeight_BD.doubleValue();
		/////
		for (int i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}
		for (int i = left+1; i <= right; i++) {
			weights[i - left] += weights[i - 1 - left];
		}
		for (int i = left; i <= right; i++) {
			weights[i - left] = (1 - weights[i - left]) / uniformizationRate;
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
				// NOTE: transition rewards have already been merged into state rewards
				soln[s] = rewards.getStateReward(index);
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
				for (int i = 0; i < numStates; i++)
					result[i] += weights[iters - left] * soln[i];
			} else {
				for (int i = 0; i < numStates; i++)
					result[i] += soln[i] / uniformizationRate;
			}
			iters++;
		}
		
		//Now that we have the expected rewards for the underlying CTMC behavior,
		//event behavior is applied.
		applyEventRewards(result, false);
		// Store the finalized expected rewards using the original indexing.
		for (int entrance : entrances) {
			meanRewards.put(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		meanRewardsComputed = true;
		*/
	}

}