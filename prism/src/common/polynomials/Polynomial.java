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
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing polynomials
 * @author Adrian E.
 */

public class Polynomial {

	/**
	 * Coefficients, coeff.get(i) represents coefficients of x^i
	 */
	public List<BigDecimal> coeffs;//should be from lowest to highest (but not guaranteed)
	/**
	 * Derivative of this polynomial
	 */
	private Polynomial derivative;
	
	/**
	 * Constructor with list of coefficients
	 * @param	coeffs	coefficients list
	 */
	public Polynomial(List<BigDecimal> coeffs){
		List<BigDecimal> newCoeffs = new ArrayList<>(coeffs);
		this.coeffs = newCoeffs;
	}
	
	/**
	 * Constructor with a single absolute coefficient
	 * @param	constant	absolute coeff.
	 */
	public Polynomial(BigDecimal constant){
		List<BigDecimal> newCoeffs = new ArrayList<>();
		newCoeffs.add(constant);
		this.coeffs = newCoeffs;
	}
	
	/**
	 * Constructor with string
	 * @param	s	string must be format ax^n+bx^m+..+cx+d, exponent must be positive int
	 */
	public Polynomial(String s){
		//negative exponents cause failure
		ArrayList<String> splitNegative = new ArrayList<>();		
		for(String curString : s.split("(?<![E])-")){
			if(curString.isEmpty()) continue;
			splitNegative.add("-"+curString);
		}
		if(!s.startsWith("-")){
			splitNegative.set(0, splitNegative.get(0).substring(1));
		}
		List<String> splitNegativePositive = new ArrayList<>();
		for(String curMinusString : splitNegative){
			for( String curString : curMinusString.split("(?<![E])\\+")){
				if(curString.isEmpty()) continue;
				splitNegativePositive.add(curString);
			}
		}
		List<BigDecimal> newCoefs = new ArrayList<>(splitNegativePositive.size());
		newCoefs.add(BigDecimal.ZERO);
		for(String curString : splitNegativePositive){
			String parseString = new String(curString);
			if(parseString.startsWith("-x")) parseString = "-1"+parseString.substring(1);
			if(parseString.startsWith("x")) parseString = "1"+parseString;
			Pattern p = Pattern.compile("(-?(\\d*\\.)?\\d+)?(E(\\+|-)\\d+)?x(\\^(-?(\\d*\\.)?\\d+))?");
			Matcher m = p.matcher(parseString);
			if(!m.find()){//no "x" found, so this is the x^0 element
				newCoefs.set(0, new BigDecimal(parseString));
			}else{
				int exponent = new BigDecimal(m.group(6) == null ? "1" : m.group(6)).intValue();
				while(newCoefs.size() < exponent + 1){
					newCoefs.add(BigDecimal.ZERO);
				}
				newCoefs.set(exponent, new BigDecimal(m.group(1) +( (m.group(3) != null )? m.group(3) : "")));
			}
		}
		
		this.coeffs = newCoefs;
	}
	
	/**
	 * Compute the derivative of this 
	 * @return derivative of this
	 */
	public Polynomial derivative(){
		if(derivative != null) return derivative;

		List<BigDecimal> derivCoeffs = new ArrayList<>();
		
		for(int i = 1; i < coeffs.size(); i++){
			derivCoeffs.add(coeffs.get(i).multiply(new BigDecimal(i)));
		}
		derivative = new Polynomial(derivCoeffs);
		return derivative;
	}
	
	/**
	 * Evaluate this polynomial in x
	 * @param	x	x to evaluate
	 * @return 	value of this in x
	 */
	public BigDecimal value(BigDecimal x){
		BigDecimal sum = new BigDecimal("0");
		BigDecimal xpower = BigDecimal.ONE;
		for(int i = 0; i < coeffs.size(); i++){
			sum = sum.add(coeffs.get(i).multiply(xpower));
			xpower = xpower.multiply(x);
		}
		return sum;
	}
	
	@Override
	public boolean equals(Object other){
		if(!other.getClass().equals(this.getClass())) return false;
		Polynomial otherPoly = (Polynomial) other;
		if(this.coeffs.size() != otherPoly.coeffs.size()) return false;
		for(int i = 0; i < this.coeffs.size(); i++){
			if(!this.coeffs.get(i).equals(otherPoly.coeffs.get(i))) return false;
		}
		return true;
	}
	
	/**
	 * Compare this polynomial to other
	 * @param	other	other polynomial
	 * @return 	-1 if this  &lt; other, 0 if this == other, 1 if this  &gt; other
	 */
	public int compareTo(Polynomial other){	//return -1 if this < other, 0 if equal, 1 if this > other
		if(coeffs.size() > other.coeffs.size()){
			for(int i = other.coeffs.size(); i < coeffs.size(); i++)
				if(coeffs.get(i).compareTo(BigDecimal.ZERO) != 0) return 1;
		}
		if(coeffs.size() < other.coeffs.size()){
			for(int i = coeffs.size(); i < other.coeffs.size(); i++)
				if(other.coeffs.get(i).compareTo(BigDecimal.ZERO) != 0) return -1;
		}
		for(int i = Math.min(coeffs.size(), other.coeffs.size()) - 1; i >= 0; i--){
			if (coeffs.get(i).compareTo(other.coeffs.get(i)) < 0){
				return - 1;
			} else if (coeffs.get(i).compareTo(other.coeffs.get(i)) > 0){
				return 1;
			}
		}
		return 0;
	}

	/**
	 * Returns degree of this polynomial
	 * @return	degree
	 */
	public int degree(){
	    int d = 0;
	    for (int i = coeffs.size() - 1; i >= 0; i--)
	        if (coeffs.get(i).compareTo(new BigDecimal("0")) != 0){ 
	        	d = i;
	        	break;
	        }
	    return d;
	}
	/**
	 * Returns leading coefficient
	 * @return	leading coefficient
	 */
	public BigDecimal getHighestCoeff(){
		BigDecimal hc = new BigDecimal("0");
	    for (int i = coeffs.size() - 1; i >= 0; i--)
	        if (coeffs.get(i).compareTo(new BigDecimal("0")) != 0){ 
	        	hc = coeffs.get(i);
	        	break;
	        }
	    return hc;
	}
	/**
	 * Adds other polynomial to this
	 * @param other	polynomial
	 */
	public void add(Polynomial other){
		for(int i = 0; i < coeffs.size() && i < other.coeffs.size(); i++){
			coeffs.set(i, coeffs.get(i).add(other.coeffs.get(i)));
		}
		if (coeffs.size() < other.coeffs.size()){
			for(int i = coeffs.size(); i < other.coeffs.size(); i++){
				coeffs.add(other.coeffs.get(i));
			}
		}
	}
	/**
	 * Substracts other polynomial from this
	 * @param other	polynomial
	 */
	public void subtract(Polynomial other){
		for(int i = 0; i < coeffs.size() && i < other.coeffs.size(); i++){
			coeffs.set(i, coeffs.get(i).subtract(other.coeffs.get(i)));
		}
		if (coeffs.size() < other.coeffs.size()){
			for(int i = coeffs.size(); i < other.coeffs.size(); i++){
				coeffs.add(other.coeffs.get(i).negate());
			}
		}
	}
	
	/**
	 * Multipies this polynomial with other
	 * @param other other polynomial
	 */
	public void multiply(Polynomial other){
		List<BigDecimal> newc = new ArrayList<>();
		for(int i = 0; i < coeffs.size() + other.coeffs.size() - 1; i++)
			newc.add(new BigDecimal("0").setScale(10, RoundingMode.HALF_UP));
		for(int i = 0; i < coeffs.size(); i++){
			for(int j = 0; j < other.coeffs.size(); j++){
				newc.set(i+j, newc.get(i+j).add(coeffs.get(i).multiply(other.coeffs.get(j))));
			}
		}
		this.coeffs = newc;
	}
	/**
	 * Multipies this polynomial with scalar
	 * @param scalar scalar to multiply with
	 */
	public void multiplyWithScalar(BigDecimal scalar){
		for(int i = 0; i < coeffs.size(); i++){
			coeffs.set(i, coeffs.get(i).multiply(scalar));
		}
	}
	/**
	 * Polynomial Long division
	 * @param n dividend
	 * @param d divisor, must not be 0 polynomial
	 * @param divScale scale of division
	 * @return	Quotient and Remainder in list, get(0) and get(1) respectively
	 */
	public static List<Polynomial> longDivision(Polynomial n, Polynomial d, int divScale){
		RoundingMode rounding = RoundingMode.DOWN;
		Polynomial q = new Polynomial("0");
		Polynomial r = new Polynomial(n.toString());
		r.coeffs = new ArrayList<>(n.coeffs);
	
		Polynomial zeroP = new Polynomial(BigDecimal.ZERO.setScale(divScale, rounding).toString());
		
		while(zeroP.compareTo(r) != 0 && r.degree() >= d.degree()){
			BigDecimal rCoef = new BigDecimal("0");
			for (int i = r.coeffs.size() - 1; i >= 0; i--)
		        if (r.coeffs.get(i).compareTo(new BigDecimal("0")) != 0){ 
		        	rCoef = r.coeffs.get(i);
		        	break;
		        }
			BigDecimal dCoef = new BigDecimal("0");
			for (int i = d.coeffs.size() - 1; i >= 0; i--)
		        if (d.coeffs.get(i).compareTo(new BigDecimal("0")) != 0){ 
		        	dCoef = d.coeffs.get(i);
		        	break;
		        }
			BigDecimal tCoef = rCoef.divide(dCoef, divScale * 2, rounding);
			int tExpon = r.degree() - d.degree();
			Polynomial t = new Polynomial(tCoef + "x^" + tExpon);
			q.add(t);
			t.multiply(d);
			r.subtract(t);
			for(int i = 0; i < r.coeffs.size(); i++){
				r.coeffs.set(i, r.coeffs.get(i).setScale(divScale, rounding));
			}
		}
		
		List<Polynomial> returnList = new ArrayList<>();
		returnList.add(q);
		returnList.add(r);
		return returnList;
	}	
	/**
	 * Returns polynomial where x is substituted with -x
	 * @return	substituted poly
	 */
	public Polynomial substituteWithMinusX(){
		List<BigDecimal> newc = new ArrayList<>(coeffs);
		for(int i = 0; i < newc.size(); i++){
			if(i % 2 == 1) newc.set(i, newc.get(i).negate());
		}
		return new Polynomial(newc);
	}
	/**
	 * Substitutes x in this polynomial with q
	 * @param q	polynomial to substitute
	 * @return substituted polynomial
	 */
	public Polynomial substituteWithPolynomial(Polynomial q){
		Polynomial toReturn = new Polynomial(BigDecimal.ZERO.toString());
		/*
		for(int i = 0; i <= degree(); i++){
			if(coeffs.get(i).compareTo(BigDecimal.ZERO) != 0){
				BigDecimal c = coeffs.get(i);
				Polynomial toAdd = new Polynomial("1");
				for(int j = 0; j < i; j++){
					toAdd.multiply(q);
				}
				toAdd.multiplyWithScalar(c);
				toReturn.plus(toAdd);
			}
		}
		*/
		/*for(int j = 0; j < coeffs.size(); j++){
			coeffs.set(j, coeffs.get(j).stripTrailingZeros());
		}*/
		for(int i = degree(); i > 0; i--){
			toReturn.add(new Polynomial(coeffs.get(i)));
			toReturn.multiply(q);
			/*for(int j = 0; j < toReturn.coeffs.size(); j++){
				toReturn.coeffs.set(j, toReturn.coeffs.get(j).stripTrailingZeros());
			}*/
		}
		toReturn.add(new Polynomial(coeffs.get(0)));
		return toReturn;
	}
	/**
	 * Transforms this polynomial p into (x+1)^degree * p(1/x+1) 
	 */
	public void substituteThisWithOne_div_X_plus_1_multiplied_x_n(){
		this.reverseCoefficients();
		Polynomial transform = new Polynomial(BigDecimal.ZERO.toString());
		for(int i = 0; i <= degree(); i++){
			if(coeffs.get(i).compareTo(BigDecimal.ZERO) != 0){
				Polynomial toAdd = new Polynomial(binomialLine(BigInteger.valueOf(i)));
				toAdd.multiplyWithScalar(coeffs.get(i));
				transform.add(toAdd);
			}
		}
		this.coeffs = transform.coeffs;
	}
	
	/**
	 * Creates binomial coefficient n over k
	 * @param n
	 * @param k
	 * @return	binomial coeff.
	 */
	public static BigInteger binomialCoeff(BigInteger n, BigInteger k){
		BigInteger l = (k.compareTo((n.subtract(k))) > 0) ? k : (n.subtract(k));
		BigInteger numerator = BigInteger.ONE;
		for(BigInteger i = n; i.compareTo(l) > 0; i = i.subtract(BigInteger.ONE)){
			numerator = numerator.multiply(i);
		}
		BigInteger denominator = BigInteger.ONE;
		for(BigInteger i = BigInteger.ONE; i.compareTo(n.subtract(l)) <= 0; i = i.add(BigInteger.ONE)){
			denominator = denominator.multiply(i);
		}
		return numerator.divide(denominator);
	}
	/**
	 * Creates list of coefficients of nth line of binomial triangle
	 * @param n
	 * @return list of coefficients
	 */
	public static List<BigDecimal> binomialLine(BigInteger n){
		List<BigDecimal> blist = new ArrayList<>();
		for(BigInteger i = BigInteger.ZERO; i.compareTo(n.divide(new BigInteger("2"))) <= 0; i = i.add(BigInteger.ONE)){
		 blist.add(new BigDecimal(Polynomial.binomialCoeff(n, i)));
		}
		List<BigDecimal> blist2 = new ArrayList<>(blist);
		if(n.mod(new BigInteger("2")).compareTo(BigInteger.ZERO) == 0) blist2.remove(blist2.size() - 1);
		Collections.reverse(blist2);
		blist.addAll(blist2);
		return blist;
	}
	/**
	 * Reverses coeffiecients of this
	 */
	public void reverseCoefficients(){
		Collections.reverse(coeffs);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();

		for(int i = coeffs.size() - 1; i >= 0; i--){
			if(coeffs.get(i).compareTo(BigDecimal.ZERO) != 0){
				if(i < coeffs.size() - 1){
					if(coeffs.get(i).compareTo(new BigDecimal("0")) > 0)
						sb.append("+");
				}
				sb.append(coeffs.get(i).toString());
				if(i >= 1){
					sb.append('x');
					if(i > 1) sb.append(i != 1 ? "^"+i : "");
				}
			}
		}
		if(sb.length() == 0) sb.append(0);
		return sb.toString();
	}	
}