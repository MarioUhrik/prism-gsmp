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
import java.util.Map;
import java.util.Set;

import ch.obermuhlner.math.big.BigDecimalMath;
import common.BigDecimalUtils;
import common.polynomials.Poly;
import common.polynomials.Polynomial;
import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * See parent class documentation for more basic info. {@link ACTMCPotato}, {@link ACTMCPotato_poly}
 * <br>
 * This extension implements high-precision precomputation
 * of Dirac-distributed potatoes using class BigDecimal.
 * This implementation is suitable for parameter synthesis.
 * <br>
 * HOW IT'S DONE:
 * Dirac distribution has one parameter - timeout t.
 * However, the data is evaluated without any specific parameter.
 * This yields a general expolynomial F(t) = P(t) * e^(-uniformizationRate * t).
 * Approximating e^(-uniformizationRate * t) with Taylor series yields polynomial T(t).
 * Evaluating F'(t) = P(t) * T(t) yields the desired results.
 */
public class ACTMCPotatoDirac_polyTaylor extends ACTMCPotato_poly
{
	
	/** {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)} */
	public ACTMCPotatoDirac_polyTaylor(ACTMCSimple actmc, GSMPEvent event, ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
	}
	
	public ACTMCPotatoDirac_polyTaylor(ACTMCPotato_poly other) {
		super(other);
	}
	
	@Override
	public void setKappa(BigDecimal kappa) {
		// Adjust kappa by the possible distribution parameter values.
		int basePrecision = BigDecimalUtils.decimalDigits(kappa);
		int diracPrecision = basePrecision + 7*(int)event.getFirstParameter();

		BigDecimal diracKappa = BigDecimalUtils.allowedError(diracPrecision);
		super.setKappa(diracKappa);
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
		
		BigDecimal fgRate = new BigDecimal(String.valueOf(uniformizationRate), mc); // Compute FoxGlynn only for the uniformization rate
		foxGlynn = new FoxGlynn_BD(fgRate, new BigDecimal(1e-300), new BigDecimal(1e+300), kappa);
		if (foxGlynn.getRightTruncationPoint() < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation of the Poisson distribution!");
		}
		int left = foxGlynn.getLeftTruncationPoint();
		int right = foxGlynn.getRightTruncationPoint();
		BigDecimal[] weights = foxGlynn.getWeights();
		
		//Get rid of the e^-lambda part, i.e. divide everything by e^-lambda
		BigDecimal factor = BigDecimalMath.exp(fgRate.negate(), mc);
		for (int i = left; i <= right; i++) {
			weights[i - left] = weights[i - left].divide(factor, mc);
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
			weights_BD[i - left] = weights_BD[i - left].divide(totalWeight_BD.multiply(new BigDecimal(String.valueOf(uniformizationRate), mc), mc), mc);
		}
		
		//Prepare the e^(- uniformizationRate * timeout) part as Taylor series polynomial P(timeout)
		BigDecimal timeout = new BigDecimal(String.valueOf(event.getFirstParameter()), mc);
		Polynomial taylorPoisson = computeTaylorSeriesPoisson(right);
		
		for (int entrance : entrances) {
			
			// Prepare solution arrays
			double[] soln = new double[numStates];
			double[] soln2 = new double[numStates];
			double[] result = new double[numStates];
			Polynomial[] polynomials = new Polynomial[numStates];
			double[] tmpsoln = new double[numStates];

			// Initialize the solution array by assigning reward 1 to the entrance and 0 to all others.
			// Also, initialize the polynomials.
			for (int i = 0; i < numStates; i++) {
				soln[i] = 0;
				polynomials[i] = new Polynomial();
			}
			soln[ACTMCtoDTMC.get(entrance)] = 1;

			
			// do 0th element of summation (doesn't require any matrix powers), and initialize the coefficients
			result = new double[numStates];
			if (left == 0) {
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.add(left, BigDecimal.ZERO);
					for (int j = 1; j <= right; ++j) {
						polynomials[i].coeffs.add(j, new BigDecimal(soln[i], mc).multiply(weights_BD[j - left], mc));
					}
				}
			} else {
				for (int i = 0; i < numStates; i++) {
					for (int j = 0; j < left; ++j) {
						polynomials[i].coeffs.add(j, BigDecimal.ZERO);
					}
					for (int j = left; j <= right; ++j) {
						polynomials[i].coeffs.add(j, new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc));
					}
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
						for (int j = iters + 1; j < right; ++j) {
							BigDecimal tmp = polynomials[i].coeffs.get(j).add(new BigDecimal(soln[i], mc).multiply(weights_BD[j - left], mc), mc);
							polynomials[i].coeffs.set(j, tmp);
						}
					}
				} else {
					for (int i = 0; i < numStates; i++) {
						for (int j = left; j <= right; ++j) {
							BigDecimal tmp = polynomials[i].coeffs.get(j).add(new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc), mc);
							polynomials[i].coeffs.set(j, tmp);
						}
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
			
			//Factor the Taylor series representation into the polynomial
			for (int n = 0; n < numStates ; ++n) {
				polynomials[n].multiply(taylorPoisson, mc);
			}
			
			// Store the solution polynomials for later use.
			for (int n = 0; n < numStates ; ++n) {
				meanTimesPolynomials.get(entrance).put(DTMCtoACTMC.get(n), polynomials[n]);
			}
			
			//Evaluate the polynomial at requested timeout t
			for (int n = 0; n < numStates ; ++n) {
				BigDecimal res = polynomials[n].value(timeout, mc);
				result[n] = res.doubleValue();
			}
			
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
		
		//Prepare the e^(- uniformizationRate * timeout) part as Taylor series polynomial P(timeout)
		BigDecimal timeout = new BigDecimal(String.valueOf(event.getFirstParameter()), mc);
		Polynomial taylorPoisson = computeTaylorSeriesPoisson(right);
		
		for (int entrance : entrances) {
			
			// Prepare solution arrays
			double[] initDist = new double[numStates];
			double[] soln;
			double[] soln2 = new double[numStates];
			double[] result = new double[numStates];
			Polynomial[] polynomialsBeforeEvent = new Polynomial[numStates];
			Polynomial[] polynomialsAfterEvent = new Polynomial[numStates];
			double[] tmpsoln = new double[numStates];
			
			// Build the initial distribution for this potato entrance
			for (int s = 0; s < numStates  ; ++s) {
				initDist[s] = 0;
			}
			initDist[ACTMCtoDTMC.get(entrance)] = 1;
			soln = initDist;

			// Initialize the arrays
			for (int i = 0; i < numStates; i++) {
				result[i] = 0.0;
				polynomialsBeforeEvent[i] = new Polynomial();
				polynomialsAfterEvent[i] = new Polynomial();
			}

			// If necessary, compute the 0th element of summation
			// (doesn't require any matrix powers)
			if (left == 0) {
				for (int i = 0; i < numStates; i++) {
					polynomialsBeforeEvent[i].coeffs.add(0, new BigDecimal(soln[i], mc).multiply(weights_BD[0], mc));
				}
			} else {
				// Initialise new polynomial coefficient
				for (int i = 0; i < numStates; i++) {
					polynomialsBeforeEvent[i].coeffs.add(0, BigDecimal.ZERO);
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
						polynomialsBeforeEvent[i].coeffs.add(iters, new BigDecimal(soln[i], mc).multiply(weights_BD[iters - left], mc));
					}
				} else {
					// Initialize new polynomial coefficient
					for (int i = 0; i < numStates; i++) {
						polynomialsBeforeEvent[i].coeffs.add(iters, BigDecimal.ZERO);
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
			
			//Factor the Taylor series representation into the polynomial
			for (int n = 0; n < numStates ; ++n) {
				polynomialsBeforeEvent[n].multiply(taylorPoisson, mc);
			}
			
			// Store the solution polynomials for later use.
			for (int n = 0; n < numStates ; ++n) {
				meanDistributionsBeforeEventPolynomials.get(entrance).put(DTMCtoACTMC.get(n), polynomialsBeforeEvent[n]);
			}
			
			//Evaluate the polynomial at requested timeout t
			for (int n = 0; n < numStates ; ++n) {
				BigDecimal res = polynomialsBeforeEvent[n].value(timeout, mc);
				result[n] = res.doubleValue();
			}
			
			// Store the just-before-event result vector for later use by other methods
			Distribution resultBeforeEvent = new Distribution();
			for(int i = 0; i < numStates ; ++i ) {
				resultBeforeEvent.add(DTMCtoACTMC.get(i), result[i]);
			}
			meanDistributionsBeforeEvent.put(entrance, resultBeforeEvent);
			
			//Lastly, the actual event behavior is applied.
			//I.e. if there is some probability that the potatoDTMC would 
			//still be within the potato at the time of the event occurrence,
			//these probabilities must be redistributed into the successor states.
			//using the event-defined distribution on states.
			for (int n = 0; n < numStates  ; ++n) {
				int nIndex = DTMCtoACTMC.get(n);
				if (potato.contains(nIndex)) {
					Distribution distr = event.getTransitions(nIndex);
					Set<Integer> distrSupport = distr.getSupport();
					for ( int successor : distrSupport) {
						polynomialsBeforeEvent[n].multiplyWithScalar(new BigDecimal(distr.get(successor), mc),  mc);
						polynomialsAfterEvent[ACTMCtoDTMC.get(successor)].add(polynomialsBeforeEvent[n], mc);
						polynomialsBeforeEvent[n].multiplyWithScalar(BigDecimal.ONE.divide(new BigDecimal(distr.get(successor), mc), mc),  mc);
					}
				} else {
					polynomialsAfterEvent[n].add(polynomialsBeforeEvent[n], mc);
				}
			}
			
			// Store the solution polynomials for later use.
			for (int n = 0; n < numStates ; ++n) {
				meanDistributionsPolynomials.get(entrance).put(DTMCtoACTMC.get(n), polynomialsAfterEvent[n]);
			}
			
			//Evaluate the polynomial at requested timeout t
			for (int n = 0; n < numStates ; ++n) {
				BigDecimal res = polynomialsAfterEvent[n].value(timeout, mc);
				result[n] = res.doubleValue();
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
			weights_BD[i - left] = weights_BD[i - left].divide(totalWeight_BD.multiply(new BigDecimal(String.valueOf(uniformizationRate), mc), mc), mc);
		}
		
		//Prepare the e^(- uniformizationRate * timeout) part as Taylor series polynomial P(timeout)
		BigDecimal timeout = new BigDecimal(String.valueOf(event.getFirstParameter()), mc);
		Polynomial taylorPoisson = computeTaylorSeriesPoisson(right);
		
		// Prepare solution arrays
		double[] soln = new double[numStates];
		double[] soln2 = new double[numStates];
		double[] result = new double[numStates];
		Polynomial[] polynomialsBeforeEvent = new Polynomial[numStates];
		Polynomial[] polynomialsAfterEvent = new Polynomial[numStates];
		double[] tmpsoln = new double[numStates];
		
		// Initialize the solution array by assigning rewards to the potato states
		// Also initialize the polynomials
		for (int i = 0; i < numStates; i++) {
			int index = DTMCtoACTMC.get(i);
			if (potato.contains(index)) {
				soln[i] = rewards.getMergedStateReward(index);
			} else {
				soln[i] = 0;
			}
			polynomialsBeforeEvent[i] = new Polynomial();
		}

		// do 0th element of summation (doesn't require any matrix powers), and initialize the coefficients
		result = new double[numStates];
		if (left == 0) {
			for (int i = 0; i < numStates; i++) {
				polynomialsBeforeEvent[i].coeffs.add(left, BigDecimal.ZERO);
				for (int j = 1; j <= right; ++j) {
					polynomialsBeforeEvent[i].coeffs.add(j, new BigDecimal(soln[i], mc).multiply(weights_BD[j - left], mc));
				}
			}
		} else {
			for (int i = 0; i < numStates; i++) {
				for (int j = 0; j < left; ++j) {
					polynomialsBeforeEvent[i].coeffs.add(j, BigDecimal.ZERO);
				}
				for (int j = left; j <= right; ++j) {
					polynomialsBeforeEvent[i].coeffs.add(j, new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc));
				}
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
					for (int j = iters + 1; j < right; ++j) {
						BigDecimal tmp = polynomialsBeforeEvent[i].coeffs.get(j).add(new BigDecimal(soln[i], mc).multiply(weights_BD[j - left], mc), mc);
						polynomialsBeforeEvent[i].coeffs.set(j, tmp);
					}
				}
			} else {
				for (int i = 0; i < numStates; i++) {
					for (int j = left; j <= right; ++j) {
						BigDecimal tmp = polynomialsBeforeEvent[i].coeffs.get(j).add(new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc), mc);
						polynomialsBeforeEvent[i].coeffs.set(j, tmp);
					}
				}
			}
			iters++;
		}
		
		// Store the sol vector  using the original indexing for later use.
		for (int ps : potato) {
			double sol = soln[ACTMCtoDTMC.get(ps)];
			if (sol != 0.0) {
				meanRewardsSoln.add(ps, sol);
			}
		}
		
		//Factor the Taylor series representation into the polynomial
		for (int n = 0; n < numStates ; ++n) {
			polynomialsBeforeEvent[n].multiply(taylorPoisson, mc);
		}
		
		// Store the solution polynomials for later use.
		for (int n = 0; n < numStates ; ++n) {
			meanRewardsBeforeEventPolynomials.put(DTMCtoACTMC.get(n), polynomialsBeforeEvent[n]);
		}
		
		//Evaluate the polynomial at requested timeout t
		for (int n = 0; n < numStates ; ++n) {
			BigDecimal res = polynomialsBeforeEvent[n].value(timeout, mc);
			result[n] = res.doubleValue();
		}
		
		// Store the rewards just before the event behavior using the original indexing.
		for (int entrance : entrances) {
			meanRewardsBeforeEvent.add(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		//Now that we have the expected rewards for the underlying CTMC behavior,
		//event behavior is applied.
		polynomialsAfterEvent = (Polynomial[])getEventRewardsPoly(false);
		for (int n = 0; n < numStates ; ++n) {
			polynomialsAfterEvent[n].add(polynomialsBeforeEvent[n], mc);
		}
		
		// Store the solution polynomials for later use.
		for (int n = 0; n < numStates ; ++n) {
			meanRewardsPolynomials.put(DTMCtoACTMC.get(n), polynomialsAfterEvent[n]);
		}
		
		//Evaluate the polynomial at requested timeout t
		for (int n = 0; n < numStates ; ++n) {
			BigDecimal res = polynomialsAfterEvent[n].value(timeout, mc);
			result[n] = res.doubleValue();
		}
		
		// Store the finalized expected rewards using the original indexing.
		for (int entrance : entrances) {
			meanRewards.add(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		meanRewardsComputed = true;
	}
	
	/**
	 * Computes the Taylor series representation of e^(- uniformizationRate * t) where t is unknown
	 * @param i integer >1 of how many elements of the series to include
	 * @return polynomial that is the Taylor series representation of e^(- uniformizationRate * t)
	 */
	private Polynomial computeTaylorSeriesPoisson(int i) {
		BigDecimal powerElem = new BigDecimal(String.valueOf(uniformizationRate), mc).negate();
		Polynomial taylor = new Polynomial();
		
		BigDecimal revFact = BigDecimal.ONE; 
		BigDecimal power = BigDecimal.ONE;
		BigDecimal augment = BigDecimal.ONE;
		for (int n = 0; n <= i; ++n) {
			taylor.coeffs.add(augment);
			
			revFact = revFact.divide(new BigDecimal(n+1, mc), mc);
			power = power.multiply(powerElem, mc);
			augment = revFact.multiply(power, mc);
		}
		
		return taylor;
	}
	
	/**
	 * Reevaluates stored computed polynomials for the current value of event.getFirstParameter() (in case it has changed),
	 * and overwrites the old values of meanTimes, meanDistributions, and meanRewards.
	 * E.g. after this is called, the following holds. <br>
	 * {@link #getMeanRewards()}.get(s) = {@link #getMeanRewardsPolynomials()}.get(s).value(event.getFirstParameter()).
	 */
	public void reevaluatePolynomials() throws PrismException { // TODO MAJO - potential small errors in the ranges of loops
		if (!meanTimesComputed) {
			computeMeanTimes();
		}
		if (!meanDistributionsComputed) {
			computeMeanDistributions();
		}
		if (!meanRewardsComputed) {
			computeMeanRewards();
		}
		
		BigDecimal timeout = new BigDecimal(String.valueOf(event.getFirstParameter()), mc);
		
		// reevaluate meanTimes
		for (Map.Entry<Integer, Map<Integer, Poly>> entry1 : meanTimesPolynomials.entrySet()) {
			int entrance = entry1.getKey();
			Map<Integer, Poly> entranceMap = entry1.getValue();
			Distribution meanTimesForEntrance = meanTimes.get(entrance);
			meanTimesForEntrance.clear();
			
			for (int s : potato) {
				Poly polynomial = entranceMap.get(s);
				
				double val = polynomial.value(timeout, mc).doubleValue();
				if (val != 0.0) {
					meanTimesForEntrance.add(s, val);
				}
			}
		}
		
		// reevaluate meanDistributionsBeforeEvent
		for (Map.Entry<Integer, Map<Integer, Poly>> entry1 : meanDistributionsBeforeEventPolynomials.entrySet()) {
			int entrance = entry1.getKey();
			Map<Integer, Poly> entranceMap = entry1.getValue();
			Distribution meanDistributionForEntrance = meanDistributionsBeforeEvent.get(entrance);
			meanDistributionForEntrance.clear();
			
			for (Map.Entry<Integer, Poly> entry2 : entranceMap.entrySet()) {
				int destination = entry2.getKey();
				Poly polynomial = entry2.getValue();
				
				double val = polynomial.value(timeout, mc).doubleValue();
				meanDistributionForEntrance.add(destination, val);
			}
		}
		
		// reevaluate meanDistributions
		for (Map.Entry<Integer, Map<Integer, Poly>> entry1 : meanDistributionsPolynomials.entrySet()) {
			int entrance = entry1.getKey();
			Map<Integer, Poly> entranceMap = entry1.getValue();
			Distribution meanDistributionForEntrance = meanDistributions.get(entrance);
			meanDistributionForEntrance.clear();
			
			for (int s : successors) {
				Poly polynomial = entranceMap.get(s);
				
				double val = polynomial.value(timeout, mc).doubleValue();
				if (val != 0.0) {
					meanDistributionForEntrance.add(s, val);
				}
			}
			//normalize the distribution
			double probSum = meanDistributionForEntrance.sum();
			for (Integer destination : meanDistributionForEntrance.getSupport()) {
				meanDistributionForEntrance.set(destination, meanDistributionForEntrance.get(destination) / probSum);
			}
		}
		
		// reevaluate meanRewardsBeforeEvent
		for (int s : entrances) {
			Poly polynomial = meanRewardsBeforeEventPolynomials.get(s);
			meanRewardsBeforeEvent.set(s, polynomial.value(timeout, mc).doubleValue());
		}

		// reevaluate meanRewards
		for (int s : entrances) {
			Poly polynomial = meanRewardsPolynomials.get(s);
			meanRewards.set(s, polynomial.value(timeout, mc).doubleValue());
		}
		
	}

}