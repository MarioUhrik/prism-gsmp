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

/**
 * Class containing methods to compute the upper bounds
 * @author Ado
 *
 */
public class Bounds {

	//a(n)*a(0) =/= 0
	/**
	 * Upper bounds on polynomial roots
	 * @param p			polynomial such that a(n) * a(0) != 0
	 * @param dScale	division scale
	 * @return			upper bound on roots
	 */
	public static BigDecimal rootBound(Polynomial p, int dScale){
		BigDecimal max = BigDecimal.ZERO;
		if(p.coeffs.get(0).compareTo(BigDecimal.ZERO) == 0) return max;
		for(int i = 1; i <= p.degree(); i++){
			if (p.coeffs.get(i).abs().compareTo(max) > 0) {
				max = p.coeffs.get(i).abs();
			}
		}
		return BigDecimal.ONE.add(max.divide(p.coeffs.get(0).abs(), dScale, RoundingMode.HALF_UP));
	}

	/**
	 * Computes cauchy bounds on roots of p
	 * @param p			polynomial
	 * @param dScale	division scale
	 * @return			cauchy bound
	 */
	public static BigDecimal cauchyBound(Polynomial p, int dScale){
		BigDecimal max = p.coeffs.get(0).divide(p.getHighestCoeff(), RoundingMode.HALF_UP).abs();
		System.out.println("cauchy computation:" + max);
		for(int i = p.degree() - 1; i >= 0; i--){
			if (p.coeffs.get(i).divide(p.getHighestCoeff(), RoundingMode.HALF_UP).abs().compareTo(max) > 0) {
				max = p.coeffs.get(i).divide(p.getHighestCoeff(), RoundingMode.HALF_UP).abs();
				System.out.println("cauchy computation:" + max);
			}
		}
		return BigDecimal.ONE.add(max);
	}

	/**
	 * Computes Kojima Bound on roots of polynomial p
	 * @param p			polynomial
	 * @param dScale	division scale
	 * @return			upper bounds on p
	 */
	public static BigDecimal kojimaBound(Polynomial p, int dScale){
		BigDecimal max = BigDecimal.ZERO;
		if(p.degree() > 0){
			try{
				max = p.coeffs.get(0).divide(PolynomialRootFinding.TWO.multiply(p.coeffs.get(1)), dScale, RoundingMode.HALF_UP).abs();
			}
			catch(ArithmeticException e){
			}
		}
		else {
			return BigDecimal.ZERO;
		}
		for(int i = 1; i < p.degree(); i++){
			BigDecimal cur;
			try{
				cur = p.coeffs.get(i).divide(p.coeffs.get(i+1), dScale, RoundingMode.HALF_UP).abs();
			}
			catch(ArithmeticException e){
				continue;
			}
			if (cur.compareTo(max) > 0) {
				max = cur;
			}
		}
		return max.multiply(PolynomialRootFinding.TWO);
	}

	/**
	 * Computes upper bounds on positive roots of polynomial p using vigklas local max upper bounds
	 * @param p		polynomial
	 * @param scale	required decimal digits
	 * @return		upper bounds on positive roots of p
	 */
	public static BigDecimal vigklasBound2(Polynomial p, int scale){
		boolean changed = false;
		if (p.getHighestCoeff().compareTo(BigDecimal.ZERO) < 0){
			p.multiplyWithScalar(BigDecimal.ONE.negate());
			changed = true;
		}
		BigDecimal upperBound = BigDecimal.ZERO;
		int n = p.degree();
		if(n + 1 <= 1) return upperBound;
		int j = n;
		int t = 1;
		for(int i = n - 1; i >= 0; i--){
			if(p.coeffs.get(i).compareTo(BigDecimal.ZERO) < 0){
				BigDecimal tempUb = p.coeffs.get(i).divide(p.coeffs.get(j), scale, RoundingMode.HALF_UP);
				tempUb = tempUb.negate().multiply(PolynomialRootFinding.TWO.pow(t));
				tempUb = PolynomialRootFinding.nthRoot(tempUb, j - i, scale);
				if(tempUb.compareTo(upperBound) > 0) upperBound = tempUb;
				t = t + 1;
			}
			else {
				if(p.coeffs.get(i).compareTo(p.coeffs.get(j)) > 0){
					j = i;
					t = 1;
				}
			}
		}
		if (changed){
			p.multiplyWithScalar(BigDecimal.ONE.negate());
		}
		return upperBound;
	}

	/**
	 * Computes upper bounds on roots of p using Fujiwara bound
	 * @param p			polynomial
	 * @param scale		required decimal digits
	 * @return			upper bounds on roots of p
	 */
	public static BigDecimal fujiwaraBound(Polynomial p, int scale){
		int n = p.degree();
		BigDecimal bound = PolynomialRootFinding.nthRoot(p.coeffs.get(0).divide(PolynomialRootFinding.TWO.multiply(p.coeffs.get(n)), scale, RoundingMode.HALF_UP).abs(), n, scale);
		for(int i = 1; i <= n-1; i++){
			BigDecimal a = PolynomialRootFinding.nthRoot(p.coeffs.get(n-i).divide(p.coeffs.get(n), scale, RoundingMode.HALF_UP).abs(), i, scale);
			if(bound.compareTo(a) < 0) bound = a;
		}
		
		return PolynomialRootFinding.TWO.multiply(bound);
	}

}
