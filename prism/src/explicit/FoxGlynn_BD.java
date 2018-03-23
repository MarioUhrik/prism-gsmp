//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <forejt@fi.muni.cz> (Masaryk University)
//  * Mario Uhrik <433501@mail.muni.cz> (Masaryk University)
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

import org.nevec.rjm.BigDecimalMath;

import common.BigDecimalUtils;
import prism.PrismException;

/**
 * BigDecimal version of {@link FoxGlynn}, allowing arbitrary {@code accuracy}.
 */
public final class FoxGlynn_BD
{
	// Math context for specifying BigDecimal accuracy
	MathContext mc;
	MathContext ceil;
	
	// constructor parameters
	//private BigDecimal underflow;
	private BigDecimal overflow, accuracy;
	private BigDecimal q_tmax;

	// returned values
	private int left, right;
	private BigDecimal totalWeight;
	private BigDecimal[] weights;

	public FoxGlynn_BD(BigDecimal qtmax, BigDecimal uf, BigDecimal of, BigDecimal acc) throws PrismException
	{
		int decimalPrecision = BigDecimalUtils.decimalDigitsPrecision(acc);
		
		q_tmax = qtmax;
		q_tmax = q_tmax.setScale(decimalPrecision);
		//underflow = uf;
		overflow = of;
		accuracy = acc;
		mc = new MathContext(decimalPrecision, RoundingMode.HALF_UP);
		ceil = new MathContext(decimalPrecision, RoundingMode.CEILING);
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

	private final void run() throws PrismException
	{
		if (q_tmax.compareTo(BigDecimal.ZERO) == 0) {
			throw new PrismException("Overflow: TA parameter qtmax = time * maxExitRate = 0.");
		}
		else if (q_tmax.compareTo(new BigDecimal("400.0")) < 0)
		{ //here naive approach should have better performance than Fox Glynn
			final BigDecimal expcoef = BigDecimalMath.exp(q_tmax.negate()); //the "e^-lambda" part of p.m.f. of Poisson dist.
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
				final BigDecimal sqrtpi = BigDecimalMath.sqrt(BigDecimalMath.pi(mc)); //square root of PI
				final BigDecimal sqrt2 = BigDecimalMath.sqrt(new BigDecimal("2.0")); //square root of 2
				final BigDecimal sqrtq = BigDecimalMath.sqrt(q_tmax);
				final BigDecimal aq = (BigDecimal.ONE.add(BigDecimal.ONE.divide(q_tmax))).multiply(BigDecimalMath.exp(new BigDecimal("0.0625").setScale(accuracy.scale())).multiply(sqrt2)); //a_\lambda from the paper			
				final BigDecimal bq = (BigDecimal.ONE.add(BigDecimal.ONE.divide(q_tmax))).multiply(BigDecimalMath.exp(new BigDecimal("0.125").divide(q_tmax))); //b_\lambda from the paper

				//use Corollary 1 to find right truncation point
				final BigDecimal lower_k_1 = BigDecimal.ONE.divide(sqrt2.multiply(q_tmax).multiply(new BigDecimal("2.0"))); //lower bound on k from Corollary 1
				final BigDecimal upper_k_1 = sqrtq.divide(sqrt2.multiply(new BigDecimal("2.0"))); //upper bound on k from Corollary 1
				BigDecimal k;

				//justification for increment is in the paper:
				//"increase k through the positive integers greater than 3"
				for(k=lower_k_1; k.compareTo(upper_k_1) <= 0;
					k=(k.compareTo(lower_k_1) == 0)? k.add(new BigDecimal("4.0")) : k.add(BigDecimal.ONE) )
				{
					BigDecimal dkl = BigDecimal.ONE.divide(BigDecimal.ONE.subtract(BigDecimalMath.exp((new BigDecimal("2.0").divide(new BigDecimal("9.0"))).multiply((k.multiply(sqrt2).multiply(sqrtq).add(new BigDecimal("1.5")))).negate()))); //d(k,\lambda) from the paper
					BigDecimal res = aq.multiply(dkl).multiply(BigDecimalMath.exp(k.multiply(k).divide(new BigDecimal("2.0")).negate())).divide(k.multiply(sqrt2).multiply(sqrtpi)); //right hand side of the equation in Corollary 1
					if (res.compareTo(accuracy.divide(new BigDecimal("2.0"))) <= 0)
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
				final BigDecimal lower_k_2 = BigDecimal.ONE.divide(sqrt2.multiply(sqrtq)); //lower bound on k from Corollary 2

				BigDecimal res;
				k=lower_k_2;
				do
				{
					res = bq.multiply(BigDecimalMath.exp(k.multiply(k).divide(new BigDecimal("2.0"))).divide(k.multiply(sqrt2).multiply(sqrtpi))); //right hand side of the equation in Corollary 2
					k.add(BigDecimal.ONE);			
				}
				while (res.compareTo(accuracy.divide(new BigDecimal("2.0"))) > 0);
				
				this.left = m - (k.multiply(sqrtq).subtract(new BigDecimal("1.5")).intValue());
				
				//According to the paper, we should check underflow of lower bound.
				//However, it seems that for no reasonable values this can happen.
				//And neither the original implementation checked it
				
				BigDecimal wm = overflow.divide(factor.multiply(new BigDecimal(this.right - this.left)));

				this.weights = new BigDecimal[(this.right-this.left+1)];
				this.weights[m-this.left] = wm;
			}
			//end of FINDER

			//compute weights
			//(at this point this.left, this.right and this.weight[m] is known)
			
			//Down from m
			for(int j=m; j>this.left; j--)
				this.weights[j-1-this.left] = (new BigDecimal(j).divide(q_tmax)).multiply(this.weights[j-this.left]);
			//Up from m
			for(int j=m; j<this.right; j++)
				this.weights[j+1-this.left] = (q_tmax.divide(new BigDecimal(j+1))).multiply(this.weights[j-this.left]);

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
					this.totalWeight = this.totalWeight.add(this.weights[s-this.left]);
					s++;
				}
				else
				{
					this.totalWeight = this.totalWeight.add(this.weights[t-this.left]);
					t--;
				}
			}
			this.totalWeight = this.totalWeight.add(this.weights[s-this.left]);
		}
	}

	/**
	 * Testing method for comparison with double-precision {@link FoxGlynn}.
	 */
	public static void main(String args[])
	{
		
		FoxGlynn_BD fg_BD = null;
		FoxGlynn fg = null;
		long stopwatch_fg_BD = 0;
		long stopwatch_fg = 0;
		try {
			// q = maxDiagRate, time = time parameter (a U<time b)
			double q = 2, time = 150;
			double uf = 1.0e-300;
			double of = 1.0e-300;
			double acc = 1.0e-30;
			
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
		
		BigDecimal sum_BD = BigDecimal.ZERO;
		Double sum_Double = 0.0;
		
		System.out.println("BigDecimal Weights:");
		for (int i = fg_BD.getLeftTruncationPoint(); i <= fg_BD.getRightTruncationPoint()  ; ++i) {
			System.out.println(fg_BD.getWeights()[i]);
			sum_BD = sum_BD.add(fg_BD.getWeights()[i]);
		}
		System.out.println("---------------------------------------------");
		System.out.println("---------------------------------------------");
		System.out.println("---------------------------------------------");
		System.out.println("Double Weights:");
		for (int i = fg.getLeftTruncationPoint(); i <= fg.getRightTruncationPoint()  ; ++i) {
			System.out.println(fg.getWeights()[i]);
			sum_Double = sum_Double + fg.getWeights()[i];
		}
		System.out.println("---------------------------------------------");
		System.out.println("---------------------------------------------");
		System.out.println("---------------------------------------------");
		System.out.println("BigDecimal time taken: " + stopwatch_fg_BD/1000.0 + " seconds.");
		System.out.println("Double time taken: " + stopwatch_fg/1000.0 + " seconds.");
		System.out.println("BigDecimal weight sum:\n" + sum_BD);
		System.out.println("Double weight sum:\n" + sum_Double);
	}
	

}
