//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
//  * Mario Uhrik <433501@mail.muni.cz> (Masaryk University)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <forejt@fi.muni.cz> (Masaryk University)
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

import ch.obermuhlner.math.big.BigDecimalMath;
import common.BigDecimalUtils;
import prism.PrismException;

/**
 * BigDecimal version of {@link FoxGlynn}, allowing arbitrary {@code accuracy}.
 */
public final class FoxGlynn_BD
{
	// Math context for specifying BigDecimal accuracy
	private MathContext mc;
	private MathContext ceil;
	private int decimalPrecision;
	
	// constructor parameters
	private BigDecimal underflow;
	private BigDecimal overflow;
	private BigDecimal accuracy;
	private BigDecimal q_tmax;

	// returned values
	private int left, right;
	private BigDecimal totalWeight;
	private BigDecimal[] weights;

	public FoxGlynn_BD(BigDecimal qtmax, BigDecimal uf, BigDecimal of, BigDecimal acc) throws PrismException
	{
		decimalPrecision = BigDecimalUtils.decimalDigits(acc);
		mc = new MathContext(decimalPrecision, RoundingMode.HALF_UP);
		ceil = new MathContext(decimalPrecision, RoundingMode.CEILING);
		
		q_tmax = qtmax;
		underflow = uf;
		overflow = of;
		accuracy = acc;
		run();
	}

	public final BigDecimal[] getWeights()
	{
		return weights;
	}

	public final int getLeftTruncationPoint()
	{
		return left;
	}

	public final int getRightTruncationPoint()
	{
		return right;
	}

	public final BigDecimal getTotalWeight()
	{
		return totalWeight;
	}
	
	/** Use with caution! */
	public final void setTotalWeight(BigDecimal totalWeight)
	{
		this.totalWeight = totalWeight;
	}

	private final void run() throws PrismException
	{
		if (q_tmax.compareTo(BigDecimal.ZERO) == 0) {
			throw new PrismException("Overflow: TA parameter qtmax = time * maxExitRate = 0.");
		}
		else if (q_tmax.compareTo(new BigDecimal("400.0")) < 0)
		{ //here naive approach should have better performance than Fox Glynn
			final BigDecimal expcoef = BigDecimalMath.exp(q_tmax.negate(), mc); //the "e^-lambda" part of p.m.f. of Poisson dist.
			int k; //denotes that we work with event "k steps occur"
			BigDecimal lastval; //(probability that exactly k events occur)/expcoef
			BigDecimal accum; //(probability that 0 to k events occur)/expcoef
			BigDecimal desval = accuracy.divide(new BigDecimal("2.0"), mc).negate().add(BigDecimal.ONE).divide(expcoef, mc); //value that we want to accumulate in accum before we stop
			java.util.Vector<BigDecimal> w = new java.util.Vector<BigDecimal>(); //stores weights computed so far.
			
			//k=0 is simple
			lastval = BigDecimal.ONE;
			accum = new BigDecimal(lastval.toString());
			w.add(lastval.multiply(expcoef));
			
			//add further steps until you have accumulated enough
			k = 1;
			do {
				lastval = lastval.multiply(q_tmax.divide(new BigDecimal(k), mc), mc); // invariant: lastval = q_tmax^k / k!
				accum = accum.add(lastval);
				w.add(lastval.multiply(expcoef));
				k++;
			} while (accum.compareTo(desval) < 0);

			//store all data
			this.left=0;
			this.right=k-1;
			this.weights = new BigDecimal[k];

			for(int i = 0; i < w.size(); i++)
			{
				this.weights[i] = w.get(i);			
			}

			//we return actual weights, so no reweighting should be done
			this.totalWeight = BigDecimal.ONE;
		}
		else
		{ //use actual Fox Glynn for q_tmax>400
			final BigDecimal factor = new BigDecimal(1e+10); //factor from the paper, it has no real explanation there
			final int m = q_tmax.intValue(); //mode
			//run FINDER to get left, right and weight[m]
			{
				final BigDecimal sqrtpi = BigDecimalMath.sqrt(BigDecimalMath.pi(mc), mc); //square root of PI
				final BigDecimal sqrt2 = BigDecimalMath.sqrt(new BigDecimal("2.0"), mc); //square root of 2
				final BigDecimal sqrtq = BigDecimalMath.sqrt(q_tmax, mc);
				final BigDecimal aq = (BigDecimal.ONE.add(BigDecimal.ONE.divide(q_tmax, mc))).multiply(BigDecimalMath.exp(new BigDecimal("0.0625"), mc).multiply(sqrt2)); //a_\lambda from the paper			
				final BigDecimal bq = (BigDecimal.ONE.add(BigDecimal.ONE.divide(q_tmax, mc))).multiply(BigDecimalMath.exp(new BigDecimal("0.125").divide(q_tmax, mc), mc)); //b_\lambda from the paper

				//use Corollary 1 to find right truncation point
				final BigDecimal lower_k_1 = BigDecimal.ONE.divide(sqrt2.multiply(q_tmax).multiply(new BigDecimal("2.0")), mc); //lower bound on k from Corollary 1
				final BigDecimal upper_k_1 = sqrtq.divide(sqrt2.multiply(new BigDecimal("2.0")), mc); //upper bound on k from Corollary 1
				BigDecimal k;

				//justification for increment is in the paper:
				//"increase k through the positive integers greater than 3"
				for(k=lower_k_1; k.compareTo(upper_k_1) <= 0;
					k=(k.compareTo(lower_k_1) == 0)? k.add(new BigDecimal("4.0")) : k.add(BigDecimal.ONE) )
				{
					BigDecimal dkl = BigDecimal.ONE.divide(BigDecimal.ONE.subtract(BigDecimalMath.exp((new BigDecimal("2.0").divide(new BigDecimal("9.0"), mc)).multiply((k.multiply(sqrt2, mc).multiply(sqrtq, mc).add(new BigDecimal("1.5"))), mc).negate(), mc), mc), mc); //d(k,\lambda) from the paper
					BigDecimal res = aq.multiply(dkl, mc).multiply(BigDecimalMath.exp(k.multiply(k, mc).divide(new BigDecimal("2.0"), mc).negate(), mc), mc).divide(k.multiply(sqrt2, mc).multiply(sqrtpi, mc), mc); //right hand side of the equation in Corollary 1
					if (res.compareTo(accuracy.divide(new BigDecimal("2.0"), mc)) <= 0)
					{
						break;
					}
				}

				if (k.compareTo(upper_k_1) > 0)
					k=upper_k_1;

				this.right = k.multiply(sqrt2).multiply(sqrtq).add(new BigDecimal(m)).add(new BigDecimal("1.5")).round(ceil).intValue();

				//use Corollary 2 to find left truncation point
				//NOTE: the original implementation used some upper bound on k,
				//      however, I didn't find it in the paper and I think it is not needed
				final BigDecimal lower_k_2 = BigDecimal.ONE.divide(sqrt2.multiply(sqrtq), mc); //lower bound on k from Corollary 2

				BigDecimal res;
				k=lower_k_2;
				do
				{
					res = bq.multiply(BigDecimalMath.exp(k.multiply(k).divide(new BigDecimal("2.0"), mc).negate(), mc), mc).divide(k.multiply(sqrt2, mc).multiply(sqrtpi, mc), mc); //right hand side of the equation in Corollary 2
					k = k.add(BigDecimal.ONE);			
				}
				while (res.compareTo(accuracy.divide(new BigDecimal("2.0"), mc)) > 0);
				
				this.left = m - (k.multiply(sqrtq).subtract(new BigDecimal("1.5")).intValue());
				
				//According to the paper, we should check underflow of lower bound.
				//However, it seems that for no reasonable values this can happen.
				//And neither the original implementation checked it
				// TODO MAJO - perhaps this is necessary for arbitrary precision implementation!
				
				BigDecimal wm = overflow.divide(factor.multiply(new BigDecimal(this.right - this.left)), mc);

				this.weights = new BigDecimal[(this.right-this.left+1)];
				this.weights[m-this.left] = wm;
			}
			//end of FINDER

			//compute weights
			//(at this point this.left, this.right and this.weight[m] is known)
			
			//Down from m
			for(int j=m; j>this.left; j--)
				this.weights[j-1-this.left] = (new BigDecimal(j).divide(q_tmax, mc)).multiply(this.weights[j-this.left], mc);
			//Up from m
			for(int j=m; j<this.right; j++)
				this.weights[j+1-this.left] = (q_tmax.divide(new BigDecimal(j+1), mc)).multiply(this.weights[j-this.left], mc);

			//Compute totalWeight (i.e. W in the paper)
			//instead of summing from left to right, start from smallest
			//and go to highest weights to prevent roundoff
			this.totalWeight = BigDecimal.ZERO;
			int s = this.left;
			int t = this.right;
			while (s<t)
			{
				if(this.weights[s - this.left].compareTo(this.weights[t - this.left]) <= 0)
				{
					this.totalWeight = this.totalWeight.add(this.weights[s-this.left], mc);
					s++;
				}
				else
				{
					this.totalWeight = this.totalWeight.add(this.weights[t-this.left], mc);
					t--;
				}
			}
			this.totalWeight = this.totalWeight.add(this.weights[s-this.left], mc);
		}
	}

	/**
	 * Testing method for comparison with double-precision {@link FoxGlynn}.
	 */
	public static void main(String args[])
	{
		
		FoxGlynn_BD fg_BD = null;
		FoxGlynn fg = null;
		MathContext mc = null;
		long stopwatch_fg_BD = 0;
		long stopwatch_fg = 0;
		try {
			// q = maxDiagRate, time = time parameter (a U<time b)
			double q = 4, time = 3; // ADJUST AT WILL!
			double uf = 1.0e-300; // ADJUST AT WILL!
			double of = 1.0e-300; // ADJUST AT WILL!
			double acc = 1.0e-10; // ADJUST AT WILL!
			mc = new MathContext(BigDecimalUtils.decimalDigits(new BigDecimal(acc)), RoundingMode.HALF_UP);
			
			stopwatch_fg_BD = System.currentTimeMillis();
			fg_BD = new FoxGlynn_BD(
						new BigDecimal(q * time),
						new BigDecimal(uf),
						new BigDecimal(of),
						new BigDecimal(acc));
			stopwatch_fg_BD = System.currentTimeMillis() - stopwatch_fg_BD;
			
			stopwatch_fg = System.currentTimeMillis();
			fg = new FoxGlynn(q*time, uf, of, acc);
			stopwatch_fg = System.currentTimeMillis() - stopwatch_fg;
			
		} catch (PrismException e) {
			// ...
		}
		
		BigDecimal probSum_BD = BigDecimal.ZERO;
		Double probSum_Double = 0.0;
		
		System.out.println("BigDecimal Poisson probs:");
		for (int i = 0; i <= fg_BD.getRightTruncationPoint() - fg_BD.getLeftTruncationPoint() ; ++i) {
			BigDecimal prob = fg_BD.getWeights()[i].divide(fg_BD.getTotalWeight(), mc);
			System.out.println(prob);
			probSum_BD = probSum_BD.add(prob, mc);
		}
		if (fg != null) {
			System.out.println("---------------------------------------------");
			System.out.println("---------------------------------------------");
			System.out.println("---------------------------------------------");
			System.out.println("Double Poisson probs:");
			for (int i = 0; i <= fg.getRightTruncationPoint() - fg.getLeftTruncationPoint()  ; ++i) {
				double prob = fg.getWeights()[i] / fg.getTotalWeight();
				System.out.println(prob);
				probSum_Double = probSum_Double + prob;
			}
		}
		System.out.println("---------------------------------------------");
		System.out.println("---------------------------------------------");
		System.out.println("---------------------------------------------");
		System.out.println("BigDecimal time taken: " + stopwatch_fg_BD/1000.0 + " seconds.");
		if (fg != null) {
			System.out.println("Double time taken: " + stopwatch_fg/1000.0 + " seconds.");
		}
		System.out.println("BigDecimal total weight:\n" + fg_BD.getTotalWeight());
		if (fg != null) {
			System.out.println("Double total weight:\n" + fg.getTotalWeight());
		}
		System.out.println("BigDecimal sum of probabilities:\n" + probSum_BD);
		if (fg != null) {
			System.out.println("Double sum of probabilities:\n" + probSum_Double);
		}
		if (fg == null) {
			System.out.println("Double precision FoxGlynn failed completely!");
		}
	}
	

}
