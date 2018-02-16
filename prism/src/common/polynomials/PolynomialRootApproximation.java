//==============================================================================
//	
//	Copyright (c) 2017-
//	Authors:
//  * Adrian Elgyutt <396222@mail.muni.cz> (Masaryk University)
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

package common.polynomials;

import java.math.BigDecimal;
import java.math.RoundingMode;

import prism.Pair;

/**
 * Class containing root approximation methods
 * @author Adrian E.
 *
 */
public class PolynomialRootApproximation {

	/**
	 * Bisection algorithm, finds one root within interval
	 * @param p		polynomial to find root of
	 * @param a		start of interval
	 * @param b		end of interval
	 * @param eps	allowed error of precision
	 * @return		root of p with allowed error eps
	 */
	public static BigDecimal bisection(Polynomial p, BigDecimal a, BigDecimal b, BigDecimal eps){
		BigDecimal c = a.add(b).divide(PolynomialRootFinding.TWO, eps.scale()+1, RoundingMode.HALF_UP);
		//System.out.print(" & " +c.subtract(a).setScale(eps.scale(), RoundingMode.HALF_UP).toString() + " & ");
		//System.out.print(c.setScale(eps.scale(), RoundingMode.HALF_UP).toString() + " & ");
		//System.out.println(p.value(c).setScale(eps.scale(), RoundingMode.HALF_UP) + " \\\\");
		if(c.subtract(a).compareTo(eps) < 0) return c;
		if(p.value(a).multiply(p.value(c)).compareTo(BigDecimal.ZERO) < 0){
			return bisection(p, a, c, eps);
		} else {
			return bisection(p, c, b, eps);
		}
	}

	/**
	 * Approximate a root using Newtons method using (begin+end)/2 as starting point. if during computation this method jumps out of this interval, depending on needsToBeInside may return fail
	 * @param p			polynomial to search root in
	 * @param precision	allowed precision error
	 * @param begin		start of interval
	 * @param end		end of interval
	 * @param needsToBeInside	true if root needs to be inside interval
	 * @return			Pair of root of p within [a,b] and bool indicating success or failure
	 */
	public static Pair<BigDecimal, Boolean> findRootNewtonInInterval(Polynomial p, BigDecimal precision, BigDecimal begin, BigDecimal end, boolean needsToBeInside){
		Boolean success = false;
		BigDecimal x0 = end.subtract(begin).divide(PolynomialRootFinding.TWO, precision.scale() + 1, RoundingMode.HALF_UP).add(begin);
		BigDecimal x1 = end.subtract(begin).divide(PolynomialRootFinding.TWO, precision.scale() + 1, RoundingMode.HALF_UP).add(begin);
		int maxIterations = 5000;
		for(int i = 0; i < maxIterations; i++)
		{
			BigDecimal y = p.value(x0);
			BigDecimal yprime = p.derivative().value(x0);		
	
			try{
				x1 = x0.subtract(y.divide(yprime, precision.scale() + 1, RoundingMode.HALF_UP));
			}
			catch(ArithmeticException e){
				return new Pair<>(x1, success);
			}
			if((x1.subtract(x0).abs().compareTo(x1.abs().multiply(precision)) <=  0)
					&& (x1.subtract(x0).abs().compareTo(precision) <=  0)){
				success = true;
				break;  
			}
			if(x1.compareTo(begin) < 0 || x1.compareTo(end) > 0 && needsToBeInside) return new Pair<>(x1, success);//important because of root skipping!
	    	x0 = x1;
		}
		return new Pair<>(x1.setScale(precision.scale(), RoundingMode.HALF_UP), success);
	}

	/**
	 * Approximate a root using Halleys method using (begin+end)/2 as starting point. if during computation this method jumps out of this interval, return as fail
	 * @param p			polynomial to search root in
	 * @param precision	allowed precision error
	 * @param begin		start of interval
	 * @param end		end of interval
	 * @param needsToBeInside	true if root needs to be inside interval
	 * @return			Pair of root of p within [a,b] and bool indicating success or failure
	 */
	public static Pair<BigDecimal, Boolean> findRootHalleyInInterval(Polynomial p, BigDecimal precision, BigDecimal begin, BigDecimal end, boolean needsToBeInside){
		Boolean success = false;
		BigDecimal x0 = end.subtract(begin).divide(PolynomialRootFinding.TWO, precision.scale() + 1, RoundingMode.HALF_UP).add(begin);
		BigDecimal x1 = end.subtract(begin).divide(PolynomialRootFinding.TWO, precision.scale() + 1, RoundingMode.HALF_UP).add(begin);
		int maxIterations = 5000;
		for(int i = 0; i < maxIterations; i++)
		{
			BigDecimal y = p.value(x0);
			BigDecimal yprime = p.derivative().value(x0);		
			BigDecimal yprimeprime = p.derivative().derivative().value(x0);
			
			try{
				BigDecimal numerator = PolynomialRootFinding.TWO.multiply(y).multiply(yprime);
				BigDecimal denominator = yprime.multiply(yprime).multiply(PolynomialRootFinding.TWO).subtract(yprimeprime.multiply(y));
				x1 = x0.subtract(numerator.divide(denominator, precision.scale() + 1, RoundingMode.HALF_UP));
			}
			catch(ArithmeticException e){
				return new Pair<>(x1, success);
			}
			if(x1.subtract(x0).abs().compareTo(x1.abs().multiply(precision)) <=  0){
				success = true;
				break;  
			}
			if(x1.compareTo(begin) < 0 || x1.compareTo(end) > 0 && needsToBeInside) return new Pair<>(x1, success);//important because of root skipping!
	    	x0 = x1;
		}
		return new Pair<>(x1.setScale(precision.scale() + 1, RoundingMode.HALF_UP), success);
	}

}
