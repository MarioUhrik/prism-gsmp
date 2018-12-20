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
 * See parent class documentation for more basic info. {@link ACTMCPotato}, {@link ACTMCPotato_poly}
 * <br>
 * This extension implements high-precision precomputation
 * of uniformly-distributed potatoes using class BigDecimal.
 * <br>
 * HOW IT'S DONE:
 * Uniform distribution has two parameters - lower bound a, and upper bound b.
 * First, Dirac behavior of the time before a is evaluated using
 * {@link ACTMCPotatoDirac_direct} with timeout a.
 * After that, the uniform behavior is evaluated without specific distribution parameters.
 * This yields a general polynomial P(t), where t is the firing time.
 * Then, let expolynomial F(t) = P(t) * e^(-uniformizationRate * t).
 * Now, computing Riemann integral from 0 to (b-a) of (F(t) * dt)
 * yields the desired results.
 */
public class ACTMCPotatoUniform_poly extends ACTMCPotato_poly
{
	
	/**
	 * Internal Dirac distributed potato (with timeout = a).
	 * It is used to precompute the deterministic behavior when a>0.
	 */
	private ACTMCPotatoDirac_direct dirac;
	/**
	 * If this variable is true, firstParameter > 0 and Dirac precomputation will be done
	 */
	private boolean diracPrecompute = false;
	
	/** {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)} */
	public ACTMCPotatoUniform_poly(ACTMCSimple actmc, GSMPEvent event, ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
		if (event.getFirstParameter() > 0) {
			diracPrecompute = true;
			computePotatoDTMC();
			this.dirac = new ACTMCPotatoDirac_direct(this);
		}
	}
	
	public ACTMCPotatoUniform_poly(ACTMCPotato_poly other) {
		super(other);
		if (event.getFirstParameter() > 0) {
			diracPrecompute = true;
			computePotatoDTMC();
			this.dirac = new ACTMCPotatoDirac_direct(this);
		}
	}
	
	@Override
	public void setKappa(BigDecimal kappa) {
		if (diracPrecompute && this.dirac != null) {
			this.dirac.setKappa(kappa);
		}
		// ACTMCPotatoUniform usually requires better precision, dependent on the distribution parameters.
		// This is because precise computation of expressions such as (e^(-lambda * b)
		// and (b^(foxGlynn.right)) is performed.
		// So, adjust kappa by the possible distribution parameter values.
		int basePrecision = BigDecimalUtils.decimalDigits(kappa); // TODO MAJO - I think b*(kappa + lambda) is needed, but thats extremely high!
		int uniformPrecision = basePrecision + (int)actmc.getMaxExitRate() +
				(int)(Math.ceil(Math.log(basePrecision * actmc.getMaxExitRate() * (event.getSecondParameter() - event.getFirstParameter())))
						*  (event.getSecondParameter() - event.getFirstParameter()));

		BigDecimal uniformKappa = BigDecimalUtils.allowedError(uniformPrecision);
		super.setKappa(uniformKappa);
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

			if (diracPrecompute) {
				// Initialize the solution array by the result of the Dirac precomputation
				// Also, initialize the polynomials.
				for (int i = 0; i < numStates; i++) {
					dirac.getMeanDistributions();
					soln[i] = dirac.meanDistributionsBeforeEvent.get(entrance).get(DTMCtoACTMC.get(i));
					polynomials[i] = new Polynomial();
				}
			} else {
				// Initialize the solution array by assigning reward 1 to the entrance and 0 to all others.
				// Also, initialize the polynomials.
				for (int i = 0; i < numStates; i++) {
					soln[i] = 0;
					polynomials[i] = new Polynomial();
				}
				soln[ACTMCtoDTMC.get(entrance)] = 1;
			}

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
			
			//Compute antiderivative of (e^(-lambda*time) * polynomial) using integration by parts
			for (int n = 0; n < numStates ; ++n) {
				antiderivatives[n] = computeAntiderivative(polynomials[n]);
			}
			
			// Store the solution polynomials for later use.
			for (int n = 0; n < numStates ; ++n) {
				meanTimesPolynomials.get(entrance).put(DTMCtoACTMC.get(n), antiderivatives[n]);
			}
			
			//Compute the definite integral using the obtained antiderivative
			for (int n = 0; n < numStates ; ++n) {
				double diracAddition = 0;
				if (diracPrecompute) { //Get the Dirac-behavior increment (if there is one)
					diracAddition = dirac.getMeanTimes().get(entrance).get(DTMCtoACTMC.get(n));
				}
				result[n] = evaluateAntiderivative(antiderivatives[n]).doubleValue() + diracAddition;
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
		
		for (int entrance : entrances) {
			
			// Prepare solution arrays
			double[] initDist = new double[numStates];
			double[] soln;
			double[] soln2 = new double[numStates];
			double[] result = new double[numStates];
			Polynomial[] polynomialsBeforeEvent = new Polynomial[numStates];
			Polynomial[] polynomialsAfterEvent = new Polynomial[numStates];
			Polynomial[] antiderivatives = new Polynomial[numStates];
			double[] tmpsoln = new double[numStates];
			
			// Build the initial distribution for this potato entrance
			if (diracPrecompute) {
				for (int s = 0; s < numStates  ; ++s) {
					dirac.getMeanDistributions();
					initDist[s] = dirac.meanDistributionsBeforeEvent.get(entrance).get(DTMCtoACTMC.get(s));
				}
			} else {
				for (int s = 0; s < numStates  ; ++s) {
					initDist[s] = 0;
				}
				initDist[ACTMCtoDTMC.get(entrance)] = 1;
			}
			soln = initDist;

			// Initialize the arrays
			for (int i = 0; i < numStates; i++) {
				result[i] = 0.0;
				polynomialsBeforeEvent[i] = new Polynomial();
				polynomialsAfterEvent[i] = new Polynomial(BigDecimal.ZERO);
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
			
			//Compute antiderivative of (e^(-lambda*time) * polynomial) using integration by parts
			for (int n = 0; n < numStates ; ++n) {
				antiderivatives[n] = computeAntiderivative(polynomialsBeforeEvent[n]);
			}
			
			// Store the solution polynomials for later use.
			for (int n = 0; n < numStates ; ++n) {
				meanDistributionsBeforeEventPolynomials.get(entrance).put(DTMCtoACTMC.get(n), antiderivatives[n]);
			}
			
			//Compute the definite integral using the obtained antiderivative
			for (int n = 0; n < numStates ; ++n) {
				result[n] = evaluateAntiderivative(antiderivatives[n]).doubleValue();
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
			
			//Compute antiderivative of (e^(-lambda*time) * polynomial) using integration by parts
			for (int n = 0; n < numStates ; ++n) {
				antiderivatives[n] = computeAntiderivative(polynomialsAfterEvent[n]);
			}
			
			// Store the solution polynomials for later use.
			for (int n = 0; n < numStates ; ++n) {
				meanDistributionsPolynomials.get(entrance).put(DTMCtoACTMC.get(n), antiderivatives[n]);
			}
			
			//Compute the definite integral using the obtained antiderivative
			for (int n = 0; n < numStates ; ++n) {
				result[n] = evaluateAntiderivative(antiderivatives[n]).doubleValue();
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
		
		// Prepare solution arrays
		double[] soln = new double[numStates];
		double[] soln2 = new double[numStates];
		double[] result = new double[numStates];
		Polynomial[] polynomialsBeforeEvent = new Polynomial[numStates];
		Polynomial[] polynomialsAfterEvent = new Polynomial[numStates];
		Polynomial[] antiderivatives = new Polynomial[numStates];
		double[] tmpsoln = new double[numStates];
		
		if (diracPrecompute) {
			// Initialize the solution array by the solution array of the Dirac precomputation
			// Also, initialize the polynomials.
			for (int i = 0; i < numStates; i++) {
				dirac.getMeanRewards();
				soln[i] = dirac.meanRewardsSoln.get(DTMCtoACTMC.get(i));
				polynomialsBeforeEvent[i] = new Polynomial();
			}
		} else {
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
				meanRewardsSoln.set(ps, sol);
			}
		}
		
		//Compute antiderivative of (e^(-lambda*time) * polynomial) using integration by parts
		for (int n = 0; n < numStates ; ++n) {
			antiderivatives[n] = computeAntiderivative(polynomialsBeforeEvent[n]);
		}
		
		// Store the solution polynomials for later use.
		for (int n = 0; n < numStates ; ++n) {
			meanRewardsBeforeEventPolynomials.put(DTMCtoACTMC.get(n), antiderivatives[n]);
		}
		
		//Compute the definite integral using the obtained antiderivative
		for (int n = 0; n < numStates ; ++n) {
			double diracAddition = 0;
			if (diracPrecompute) { //Get the Dirac-behavior increment (if there is one)
				diracAddition = dirac.meanRewardsBeforeEvent.get(DTMCtoACTMC.get(n));
			}
			result[n] = evaluateAntiderivative(antiderivatives[n]).doubleValue() + diracAddition;
		}
		
		// Store the rewards just before the event behavior using the original indexing.
		for (int entrance : entrances) {
			meanRewardsBeforeEvent.set(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		//Now that we have the expected rewards for the underlying CTMC behavior,
		//event behavior is applied.
		polynomialsAfterEvent = (Polynomial[])getEventRewardsPoly(false);
		antiderivatives = antiderivatives.clone();
		for (int n = 0; n < numStates ; ++n) {
			antiderivatives[n] = new Polynomial(antiderivatives[n].coeffs);
			antiderivatives[n].add(polynomialsAfterEvent[n], mc);
		}
		
		// Store the solution polynomials for later use.
		for (int n = 0; n < numStates ; ++n) {
			meanRewardsPolynomials.put(DTMCtoACTMC.get(n), antiderivatives[n]);
		}
		
		//Compute the definite integral using the obtained antiderivative
		for (int n = 0; n < numStates ; ++n) {
			double diracAddition = 0;
			if (diracPrecompute) { //Get the Dirac-behavior increment (if there is one)
				diracAddition = dirac.meanRewardsBeforeEvent.get(DTMCtoACTMC.get(n));
			}
			result[n] = evaluateAntiderivative(antiderivatives[n]).doubleValue() + diracAddition;
		}
		
		// Store the finalized expected rewards using the original indexing.
		for (int entrance : entrances) {
			meanRewards.set(entrance, result[ACTMCtoDTMC.get(entrance)]);
		}
		
		meanRewardsComputed = true;
	}
	
	/**
	 * Computes antiderivative of (e^(-lambda*time) * polynomial) using integration by parts
	 * @param polynomial
	 * @return antiderivative polynomial of (e^(-lambda*time) * polynomial)
	 */
	private Polynomial computeAntiderivative(Polynomial polynomial) {
		//Unoptimized version
		/*for (int n = 0; n < numStates ; ++n) {
			Polynomial poly = new Polynomial(new ArrayList<BigDecimal>(polynomials[n].coeffs));
			Polynomial antiderivative = antiderivatives[n];
			
			BigDecimal factor = new BigDecimal(-1/uniformizationRate, mc);
			int polyDegree = poly.degree();
			for (int i = 0; i <= polyDegree ; i++) {
				poly.multiplyWithScalar(factor, mc);
				antiderivative.add(poly, mc);	
				
				poly.multiplyWithScalar(BigDecimal.ONE.negate(), mc);
				poly = poly.derivative(mc);
			}
		}*/
		
		Polynomial poly = new Polynomial(new ArrayList<BigDecimal>(polynomial.coeffs));
		Polynomial antiderivative = new Polynomial();
		BigDecimal factor = BigDecimal.ONE.negate().divide(new BigDecimal(String.valueOf(uniformizationRate), mc), mc);
		
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
	
	/**
	 * Evaluates the given antiderivative to compute the definite (Riemann) integral.
	 * <br>
	 * In other words, this actually does the F(b)-F(a) part of Riemann integral required for this specific distribution.
	 * @param antiderivative Polynomial obtained from {@code computeAntiderivative()}
	 * @return result BigDecimal number, actually the mean time,distribution or reward for given entrance and state
	 */
	private BigDecimal evaluateAntiderivative(Polynomial antiderivative) {
		BigDecimal a = BigDecimal.ZERO;
		BigDecimal b = new BigDecimal(String.valueOf(event.getSecondParameter()), mc).subtract(new BigDecimal(String.valueOf(event.getFirstParameter())), mc);
		BigDecimal aFactor = BigDecimalMath.exp(new BigDecimal(uniformizationRate, mc).negate().multiply(a, mc), mc);
		BigDecimal bFactor = BigDecimalMath.exp(new BigDecimal(uniformizationRate, mc).negate().multiply(b, mc), mc);
		BigDecimal aVal = antiderivative.value(a, mc).multiply(aFactor, mc);
		BigDecimal bVal = antiderivative.value(b, mc).multiply(bFactor, mc);
		BigDecimal prob = BigDecimal.ONE.divide(b.subtract(a, mc), mc);
		
		BigDecimal res = prob.multiply(bVal.subtract(aVal, mc), mc);
		return res;
	}

}