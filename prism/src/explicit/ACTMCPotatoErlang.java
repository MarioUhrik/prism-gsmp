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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Set;

import ch.obermuhlner.math.big.BigDecimalMath;
import common.BigDecimalUtils;
import common.polynomials.Polynomial;
import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * Class for storage and computation of single Erlang distributed potato-related data for ACTMCs,
 * I.e. this implementation treats the event as Erlang distributed.
 * <br>
 * Potato is a subset of states of an ACTMC in which a given event is active.
 * <br><br>
 * This data is fundamental for ACTMC model checking methods based on reduction
 * of ACTMC to DTMC. The reduction works by pre-computing the expected behavior
 * (rewards, spent time, resulting distribution...) occurring between
 * entering and leaving a potato. Then, these expected values are used in
 * regular CTMC/DTMC model checking methods.
 */
public class ACTMCPotatoErlang extends ACTMCPotato
{
	
	/** {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)} */
	public ACTMCPotatoErlang(ACTMCSimple actmc, GSMPEvent event, ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
	}
	
	public ACTMCPotatoErlang(ACTMCPotato other) {
		super(other);
	}
	
	@Override
	public void setKappa(BigDecimal kappa) {
		// ACTMCPotatoErlang usually requires better precision, dependent on the distribution parameters.
		// This is because precise computation of expressions such as (e^(-(erlangRate + CTMCRate) * [upper_bound])
		// and ([upper_bound]^(foxGlynn.right + k)) is performed.
		int basePrecision = BigDecimalUtils.decimalDigits(kappa); // TODO MAJO - I think [upper_bound]*(kappa + lambda) is needed, but thats extremely high!
		int erlangPrecision = basePrecision + ((int)Math.ceil(Math.log(((event.getFirstParameter() + uniformizationRate) * basePrecision * event.getSecondParameter()))));
		
		BigDecimal erlangKappa = BigDecimalUtils.allowedError(erlangPrecision);
		super.setKappa(erlangKappa);
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
		
		for (int entrance : entrances) {
			
			// Prepare solution arrays
			double[] soln = new double[numStates];
			double[] soln2 = new double[numStates];
			double[] result = new double[numStates];
			Polynomial[] polynomials = new Polynomial[numStates];
			Polynomial[] antiderivatives = new Polynomial[numStates];
			double[] tmpsoln = new double[numStates];

			// Initialize the solution array by assigning reward 1 to the entrance and 0 to all others.
			// Also, initialize the polynomials.
			for (int i = 0; i < numStates; i++) {
				soln[i] = 0;
				polynomials[i] = new Polynomial(new ArrayList<BigDecimal>());
				antiderivatives[i] = new Polynomial(new ArrayList<BigDecimal>());
			}
			soln[ACTMCtoDTMC.get(entrance)] = 1;

			// do 0th element of summation (doesn't require any matrix powers)
			result = new double[numStates];
			if (left == 0) {
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.add(0, new BigDecimal(soln[i], mc).multiply(weights_BD[0], mc));
				}
			} else {
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.add(0, new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc));
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
				// Initialize new polynomial coefficient
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.add(iters, BigDecimal.ZERO);
				}
				// Add to sum
				if (iters >= left) {
					for (int i = 0; i < numStates; i++) {
						polynomials[i].coeffs.set(iters, new BigDecimal(soln[i], mc).multiply(weights_BD[iters - left], mc));
						for (int j = left ; j < iters; ++j) {
							BigDecimal tmp = polynomials[i].coeffs.get(j).add(new BigDecimal(soln[i], mc).multiply(weights_BD[j - left], mc), mc);
							polynomials[i].coeffs.set(j, tmp);
						}
					}
				} else {
					for (int i = 0; i < numStates; i++) {
						BigDecimal tmp = new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc).add(polynomials[i].coeffs.get(0), mc);
						polynomials[i].coeffs.set(0, tmp);
					}
				}
				iters++;
			}
			
			//Increase the polynomial degrees by t^(k-1)
			for (int n = 0; n < numStates  ; ++n) {
				for (int k = 0; k < (int)(event.getSecondParameter() - 1); ++k) {
					polynomials[n].coeffs.add(0, BigDecimal.ZERO);
				}
			}
			
			//Compute antiderivative of (e^(-(lambda + erlangRate) * time) * polynomial) using integration by parts
			for (int n = 0; n < numStates ; ++n) {
				antiderivatives[n] = computeAntiderivative(polynomials[n]);
			}
			
			//Compute the definite integral using the obtained antiderivative
			for (int n = 0; n < numStates ; ++n) {
				Polynomial antiderivative = antiderivatives[n];
				BigDecimal a = BigDecimal.ZERO;
				BigDecimal b = new BigDecimal(BigDecimalUtils.decimalDigits(kappa) * 2.5, mc); // sufficiently high upper bound (supposed to be infinity
				BigDecimal lambdas = new BigDecimal(String.valueOf(uniformizationRate), mc).add(new BigDecimal(String.valueOf(event.getFirstParameter()), mc), mc);
				BigDecimal aFactor = BigDecimalMath.exp(lambdas.negate().multiply(a, mc), mc);
				BigDecimal bFactor = BigDecimalMath.exp(lambdas.negate().multiply(b, mc), mc);
				BigDecimal aVal = antiderivative.value(a, mc).multiply(aFactor, mc);
				BigDecimal bVal = antiderivative.value(b, mc).multiply(bFactor, mc);
				BigDecimal firstFactor = BigDecimal.ONE.divide(BigDecimalMath.factorial((int)(event.getSecondParameter() - 1)), mc);
				BigDecimal secondFactor = BigDecimalMath.pow(new BigDecimal(String.valueOf(event.getFirstParameter()), mc), new BigDecimal(String.valueOf(event.getSecondParameter()), mc), mc);
				BigDecimal totalFactor = firstFactor.multiply(secondFactor, mc);
				
				BigDecimal res = polynomials[n].coeffs.get((int)(event.getSecondParameter() - 1)).subtract(bVal.subtract(aVal, mc).multiply(totalFactor, mc), mc);
				result[n] = res.doubleValue();
			}
			
			// We are done. 
			// Convert the result to a distribution with original indexing and store it.
			// Also, store the solution vector using the original indexing.
			Distribution resultDistr = new Distribution();
			Distribution solnDistr = new Distribution();
			for (int ps : potato) {
				double time = result[ACTMCtoDTMC.get(ps)];
				if (time != 0.0) {
					resultDistr.add(ps, time);
				}
				double sol = soln[ACTMCtoDTMC.get(ps)];
				if (sol != 0.0) {
					solnDistr.add(ps, sol);
				}
			}
			meanTimes.put(entrance, resultDistr);
			meanTimesSoln.put(entrance, solnDistr);
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

			// Initialize the arrays
			for (int i = 0; i < numStates; i++) {
				result[i] = 0.0;
				polynomials[i] = new Polynomial(new ArrayList<BigDecimal>());
				antiderivatives[i] = new Polynomial(new ArrayList<BigDecimal>());
			}

			// If necessary, compute the 0th element of summation
			// (doesn't require any matrix powers)
			if (left == 0) {
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.add(0, new BigDecimal(soln[i], mc).multiply(weights_BD[0], mc));
				}
			} else {
				// Initialise new polynomial coefficient
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.add(0, BigDecimal.ZERO);
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
						polynomials[i].coeffs.add(iters, new BigDecimal(soln[i], mc).multiply(weights_BD[iters - left], mc));
					}
				} else {
					// Initialize new polynomial coefficient
					for (int i = 0; i < numStates; i++) {
						polynomials[i].coeffs.add(iters, BigDecimal.ZERO);
					}
				}
				iters++;
			}
			
			//Increase the polynomial degrees by t^(k-1)
			for (int n = 0; n < numStates  ; ++n) {
				for (int k = 0; k < (int)(event.getSecondParameter() - 1); ++k) {
					polynomials[n].coeffs.add(0, BigDecimal.ZERO);
				}
			}
			
			//Compute antiderivative of (e^(-(lambda + erlangRate) * time) * polynomial) using integration by parts
			for (int n = 0; n < numStates ; ++n) {
				antiderivatives[n] = computeAntiderivative(polynomials[n]);
			}
			
			//Compute the definite integral using the obtained antiderivative
			for (int n = 0; n < numStates ; ++n) {
				Polynomial antiderivative = antiderivatives[n];
				BigDecimal a = BigDecimal.ZERO;
				BigDecimal b = new BigDecimal(BigDecimalUtils.decimalDigits(kappa) * 2.5, mc); // sufficiently high upper bound (supposed to be infinity
				BigDecimal lambdas = new BigDecimal(String.valueOf(uniformizationRate), mc).add(new BigDecimal(String.valueOf(event.getFirstParameter()), mc), mc);
				BigDecimal aFactor = BigDecimalMath.exp(lambdas.negate().multiply(a, mc), mc);
				BigDecimal bFactor = BigDecimalMath.exp(lambdas.negate().multiply(b, mc), mc);
				BigDecimal aVal = antiderivative.value(a, mc).multiply(aFactor, mc);
				BigDecimal bVal = antiderivative.value(b, mc).multiply(bFactor, mc);
				BigDecimal firstFactor = BigDecimal.ONE.divide(BigDecimalMath.factorial((int)(event.getSecondParameter() - 1)), mc);
				BigDecimal secondFactor = BigDecimalMath.pow(new BigDecimal(String.valueOf(event.getFirstParameter()), mc), new BigDecimal(String.valueOf(event.getSecondParameter()), mc), mc);
				BigDecimal totalFactor = firstFactor.multiply(secondFactor, mc);
				
				BigDecimal res = bVal.subtract(aVal, mc).multiply(totalFactor, mc);
				result[n] = res.doubleValue();
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
			// Store the solution vector using the original indexing.
			Distribution solnDistr = new Distribution();
			for (int ps : potato) {
				double sol = soln[ACTMCtoDTMC.get(ps)];
				if (sol != 0.0) {
					solnDistr.add(ps, sol);
				}
			}
			meanDistributionsSoln.put(entrance, solnDistr);
			
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
		
		// Prepare solution arrays
		double[] soln = new double[numStates];
		double[] soln2 = new double[numStates];
		double[] result = new double[numStates];
		Polynomial[] polynomials = new Polynomial[numStates];
		Polynomial[] antiderivatives = new Polynomial[numStates];
		double[] tmpsoln = new double[numStates];

		// Initialize the solution array by assigning rewards to the potato states
		// Also initialize the polynomials
		for (int s = 0; s < numStates; s++) {
			int index = DTMCtoACTMC.get(s);
			if (potato.contains(index)) {
				// NOTE: transition rewards have already been merged into state rewards
				soln[s] = rewards.getStateReward(index);
			} else {
				soln[s] = 0;
			}
			polynomials[s] = new Polynomial(new ArrayList<BigDecimal>());
			antiderivatives[s] = new Polynomial(new ArrayList<BigDecimal>());
		}

		// do 0th element of summation (doesn't require any matrix powers)
		result = new double[numStates];
		if (left == 0) {
			for (int i = 0; i < numStates; i++) {
				polynomials[i].coeffs.add(0, new BigDecimal(soln[i], mc).multiply(weights_BD[0], mc));
			}
		} else {
			for (int i = 0; i < numStates; i++) {
				polynomials[i].coeffs.add(0, new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc));
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
			// Initialize new polynomial coefficient
			for (int i = 0; i < numStates; i++) {
				polynomials[i].coeffs.add(iters, BigDecimal.ZERO);
			}
			// Add to sum
			if (iters >= left) {
				for (int i = 0; i < numStates; i++) {
					polynomials[i].coeffs.set(iters, new BigDecimal(soln[i], mc).multiply(weights_BD[iters - left], mc));
					for (int j = left ; j < iters; ++j) {
						BigDecimal tmp = polynomials[i].coeffs.get(j).add(new BigDecimal(soln[i], mc).multiply(weights_BD[j - left], mc), mc);
						polynomials[i].coeffs.set(j, tmp);
					}
				}
			} else {
				for (int i = 0; i < numStates; i++) {
					BigDecimal tmp = new BigDecimal(soln[i], mc).divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc).add(polynomials[i].coeffs.get(0), mc);
					polynomials[i].coeffs.set(0, tmp);
				}
			}
			iters++;
		}
		
		//Increase the polynomial degrees by t^(k-1)
		for (int n = 0; n < numStates  ; ++n) {
			for (int k = 0; k < (int)(event.getSecondParameter() - 1); ++k) {
				polynomials[n].coeffs.add(0, BigDecimal.ZERO);
			}
		}
		
		//Compute antiderivative of (e^(-(lambda + erlangRate) * time) * polynomial) using integration by parts
		for (int n = 0; n < numStates ; ++n) {
			antiderivatives[n] = computeAntiderivative(polynomials[n]);
		}
		
		//Compute the definite integral using the obtained antiderivative
		for (int n = 0; n < numStates ; ++n) {
			Polynomial antiderivative = antiderivatives[n];
			BigDecimal a = BigDecimal.ZERO;
			BigDecimal b = new BigDecimal(BigDecimalUtils.decimalDigits(kappa) * 2.5, mc); // sufficiently high upper bound (supposed to be infinity
			BigDecimal lambdas = new BigDecimal(String.valueOf(uniformizationRate), mc).add(new BigDecimal(String.valueOf(event.getFirstParameter()), mc), mc);
			BigDecimal aFactor = BigDecimalMath.exp(lambdas.negate().multiply(a, mc), mc);
			BigDecimal bFactor = BigDecimalMath.exp(lambdas.negate().multiply(b, mc), mc);
			BigDecimal aVal = antiderivative.value(a, mc).multiply(aFactor, mc);
			BigDecimal bVal = antiderivative.value(b, mc).multiply(bFactor, mc);
			BigDecimal firstFactor = BigDecimal.ONE.divide(BigDecimalMath.factorial((int)(event.getSecondParameter() - 1)), mc);
			BigDecimal secondFactor = BigDecimalMath.pow(new BigDecimal(String.valueOf(event.getFirstParameter()), mc), new BigDecimal(String.valueOf(event.getSecondParameter()), mc), mc);
			BigDecimal totalFactor = firstFactor.multiply(secondFactor, mc);
			
			BigDecimal res = polynomials[n].coeffs.get((int)(event.getSecondParameter() - 1)).subtract(bVal.subtract(aVal, mc).multiply(totalFactor, mc), mc);
			result[n] = res.doubleValue();
		}
		
		// Store the rewards just before the event behavior using the original indexing.
		for (int entrance : entrances) {
			meanRewardsBeforeEvent.add(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		//Now that we have the expected rewards for the underlying CTMC behavior,
		//event behavior is applied.
		applyEventRewards(result, false);
		
		// Store the solution vector  using the original indexing.
		for (int ps : potato) {
			double sol = soln[ACTMCtoDTMC.get(ps)];
			if (sol != 0.0) {
				meanRewardsSoln.add(ps, sol);
			}
		}
		
		// Store the finalized expected rewards using the original indexing.
		for (int entrance : entrances) {
			meanRewards.add(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		meanRewardsComputed = true;
	}
	
	/**
	 * Computes antiderivative of (e^(-(lambda + erlangRate) * time) * polynomial) using integration by parts
	 * @param polynomial
	 * @return antiderivative polynomial of (e^(-(lambda + erlangRate) * time) * polynomial)
	 */
	private Polynomial computeAntiderivative(Polynomial polynomial) {
		//Unoptimized version
		/*for (int n = 0; n < numStates ; ++n) {
			Polynomial poly = new Polynomial(new ArrayList<BigDecimal>(polynomials[n].coeffs));
			Polynomial antiderivative = antiderivatives[n];
			
			BigDecimal factor = new BigDecimal((-1/(uniformizationRate + event.getFirstParameter())), mc);
			int polyDegree = poly.degree();
			for (int i = 0; i <= polyDegree ; i++) {
				poly.multiplyWithScalar(factor, mc);
				antiderivative.add(poly, mc);	
				
				poly.multiplyWithScalar(BigDecimal.ONE.negate(), mc);
				poly = poly.derivative(mc);
			}
		}*/
		
		Polynomial poly = new Polynomial(new ArrayList<BigDecimal>(polynomial.coeffs));
		Polynomial antiderivative = new Polynomial(new ArrayList<BigDecimal>());
		BigDecimal factor = BigDecimal.ONE.negate().divide
				(new BigDecimal(String.valueOf(uniformizationRate), mc).add(new BigDecimal(String.valueOf(event.getFirstParameter()),mc), mc), mc);
		
		int polyDegree = poly.degree();
		for (int e = 0 ; e <= polyDegree ; ++e) {
			BigDecimal coeff = poly.coeffs.get(e);
			antiderivative.coeffs.add(e, BigDecimal.ZERO);
			for ( int i = e ; i >= 0 ; --i) {
				coeff = coeff.multiply(factor, mc);
				if (coeff.compareTo(BigDecimal.ZERO) >= 0) {
					coeff = coeff.multiply(BigDecimal.ONE.negate(), mc);
				}
				antiderivative.coeffs.set(i, antiderivative.coeffs.get(i).add(coeff, mc));
				
				coeff = coeff.multiply(new BigDecimal(i, mc), mc);
			}
		}
		
		return antiderivative;
	}

}