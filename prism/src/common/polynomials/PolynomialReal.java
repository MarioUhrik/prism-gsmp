//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
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

package common.polynomials;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.obermuhlner.math.big.BigDecimalMath;
import prism.PrismException;

/**
 * Class representing polynomials with real exponents. // TODO MAJO - Not yet tested.
 * This implementation might also be more efficient for very sparse polynomials with integer exponents.
 * <br>
 * Based on {@link Polynomial}
 */
public class PolynomialReal {

	/**
	 * Map of coefficients mapped onto their exponents, coeff.get(i) represents coefficients of x^i
	 */
	public Map<BigDecimal, BigDecimal> coeffs;
	/**
	 * Derivative of this polynomial
	 */
	private PolynomialReal derivative;
	
	/**
	 * Constructor for an empty polynomial
	 */
	public PolynomialReal(){
		Map<BigDecimal, BigDecimal> newCoeffs = new HashMap<BigDecimal, BigDecimal>();
		this.coeffs = newCoeffs;
	}
	
	/**
	 * Constructor with a map of coefficients
	 * @param	coeffs	coefficients map
	 */
	public PolynomialReal(Map<BigDecimal, BigDecimal> coeffs){
		Map<BigDecimal, BigDecimal> newCoeffs = new HashMap<BigDecimal, BigDecimal>(coeffs);
		this.coeffs = newCoeffs;
	}
	
	/**
	 * Constructor with list of coefficients from {@link Polynomial}
	 * @param	coeffs	coefficients list
	 */
	public PolynomialReal(List<BigDecimal> coeffs){
		Map<BigDecimal, BigDecimal> newCoeffs = new HashMap<BigDecimal, BigDecimal>();
		for (int i = 0; i < coeffs.size() ; ++i) {
			newCoeffs.put(new BigDecimal(String.valueOf(i)), coeffs.get(i));
		}
		this.coeffs = newCoeffs;
	}
	
	/**
	 * Constructor with a single absolute coefficient
	 * @param	constant	absolute coeff.
	 */
	public PolynomialReal(BigDecimal constant){
		Map<BigDecimal, BigDecimal> newCoeffs = new HashMap<BigDecimal, BigDecimal>();
		newCoeffs.put(BigDecimal.ZERO, constant);
		this.coeffs = newCoeffs;
	}
	
	/**
	 * Constructor with string
	 * @param	s	string must be format ax^n+bx^m+..+cx+d, exponent must be positive int
	 *  //TODO MAJO - not yet supported for non-integer exponents.
	 */
	public PolynomialReal(String s){
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
		List<BigDecimal> coefList = new ArrayList<>(splitNegativePositive.size());
		coefList.add(BigDecimal.ZERO);
		for(String curString : splitNegativePositive){
			String parseString = new String(curString);
			if(parseString.startsWith("-x")) parseString = "-1"+parseString.substring(1);
			if(parseString.startsWith("x")) parseString = "1"+parseString;
			Pattern p = Pattern.compile("(-?(\\d*\\.)?\\d+)?(E(\\+|-)\\d+)?x(\\^(-?(\\d*\\.)?\\d+))?");
			Matcher m = p.matcher(parseString);
			if(!m.find()){//no "x" found, so this is the x^0 element
				coefList.set(0, new BigDecimal(parseString));
			}else{
				int exponent = new BigDecimal(m.group(6) == null ? "1" : m.group(6)).intValue();
				while(coefList.size() < exponent + 1){
					coefList.add(BigDecimal.ZERO);
				}
				coefList.set(exponent, new BigDecimal(m.group(1) +( (m.group(3) != null )? m.group(3) : "")));
			}
		}
		
		Map<BigDecimal, BigDecimal> newCoeffs = new HashMap<BigDecimal, BigDecimal>();
		for (int i = 0; i < coefList.size() ; ++i) {
			newCoeffs.put(new BigDecimal(String.valueOf(i)), coefList.get(i));
		}
		this.coeffs = newCoeffs;
	}
	
	/**
	 * Copy constructor
	 * @param other Polynomial
	 */
	public PolynomialReal(PolynomialReal other) {
		this.coeffs = new HashMap<BigDecimal, BigDecimal>(other.coeffs);
		//this.derivative = other.derivative;
	}
	
	/**
	 * Compute the derivative of this 
	 * @return derivative of this
	 */
	public PolynomialReal derivative(){
		if(derivative != null) return derivative;

		Map<BigDecimal, BigDecimal> derivCoeffs = new HashMap<BigDecimal, BigDecimal>();
		
		for (Map.Entry<BigDecimal, BigDecimal> entry : coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			BigDecimal coeff = entry.getValue();
			
			if (exponent.compareTo(BigDecimal.ZERO) == 0) {
				
			} else {
				derivCoeffs.put(exponent.subtract(BigDecimal.ONE), coeff.multiply(exponent));
			}
		}
		
		derivative = new PolynomialReal(derivCoeffs);
		return derivative;
	}
	
	/**
	 * Compute the derivative of this using the given MathContext
	 * @param mc MathContext to use
	 * @return derivative of this
	 */
	public PolynomialReal derivative(MathContext mc){
		if(derivative != null) return derivative;

		Map<BigDecimal, BigDecimal> derivCoeffs = new HashMap<BigDecimal, BigDecimal>();
		
		for (Map.Entry<BigDecimal, BigDecimal> entry : coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			BigDecimal coeff = entry.getValue();
			
			if (exponent.compareTo(BigDecimal.ZERO) == 0) {
				
			} else {
				derivCoeffs.put(exponent.subtract(BigDecimal.ONE, mc), coeff.multiply(exponent, mc));
			}
		}
		
		derivative = new PolynomialReal(derivCoeffs);
		return derivative;
	}
	
	/**
	 * Compute the antiderivative of this using the given MathContext
	 * @param mc MathContext to use
	 * @return antiderivative of this
	 * @throws PrismExpception if 
	 */
	public PolynomialReal antiderivative(MathContext mc) throws PrismException {
		Map<BigDecimal, BigDecimal> antiderivCoeffs = new HashMap<BigDecimal, BigDecimal>();
		
		for (Map.Entry<BigDecimal, BigDecimal> entry : coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			BigDecimal coeff = entry.getValue();
			
			if (exponent.compareTo(BigDecimal.ONE.negate()) == 0) {
				throw new PrismException("Refusing to compute antiderivative of a c*x^(-1)");
			} else {
				antiderivCoeffs.put(exponent.add(BigDecimal.ONE, mc), coeff.divide(exponent, mc));
			}
		}
		
		PolynomialReal antiderivative = new PolynomialReal(antiderivCoeffs);
		antiderivative.derivative = this;
		return antiderivative;
	}
	
	/**
	 * Evaluate this polynomial in x
	 * @param	x	x to evaluate
	 * @return 	value of this in x
	 */
	public BigDecimal value(BigDecimal x){
		BigDecimal sum = BigDecimal.ZERO;
		
		//Declare some mathContext, because the pow call requires it
		MathContext mc = new MathContext(x.precision() + 100, RoundingMode.HALF_UP);
		
		for (Map.Entry<BigDecimal, BigDecimal> entry : coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			BigDecimal coeff = entry.getValue();
			
			sum = sum.add(BigDecimalMath.pow(coeff, exponent, mc));
		}
		
		return sum;
	}
	
	/**
	 * Evaluate this polynomial in x using the given MathContext
	 * @param	x	x to evaluate
	 * @param   mc MathContext to use for the evaluation
	 * @return 	value of this in x
	 */
	public BigDecimal value(BigDecimal x, MathContext mc){
		BigDecimal sum = BigDecimal.ZERO;
		
		for (Map.Entry<BigDecimal, BigDecimal> entry : coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			BigDecimal coeff = entry.getValue();
			
			sum = sum.add(BigDecimalMath.pow(coeff, exponent, mc));
		}
		
		return sum;
	}
	
	@Override
	public boolean equals(Object other){
		if(!other.getClass().equals(this.getClass())) return false;
		PolynomialReal otherPoly = (PolynomialReal) other;
		if(this.coeffs.size() != otherPoly.coeffs.size()) return false;
		for (Map.Entry<BigDecimal, BigDecimal> entry : coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			BigDecimal coeff = entry.getValue();
			
			BigDecimal otherCoeff = otherPoly.coeffs.get(exponent);
			if (coeff.compareTo(otherCoeff) != 0) {
				return false;
			}
		}
		for (Map.Entry<BigDecimal, BigDecimal> entry : otherPoly.coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			BigDecimal coeff = entry.getValue();
			
			BigDecimal thisCoeff = coeffs.get(exponent);
			if (coeff.compareTo(thisCoeff) != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns degree of this polynomial
	 * @return	degree
	 */
	public BigDecimal degree(){
		BigDecimal d = BigDecimal.ZERO;
		for (Map.Entry<BigDecimal, BigDecimal> entry : coeffs.entrySet()) {
			BigDecimal exponent = entry.getKey();
			
			if (exponent.compareTo(d) > 0) {
				d = exponent;
			}
		}
	    return d;
	}
	/**
	 * Returns leading coefficient
	 * @return	leading coefficient
	 */
	public BigDecimal getHighestCoeff(){
		return coeffs.get(degree());
	}
	/**
	 * Adds other polynomial to this
	 * @param other	polynomial
	 */
	public void add(PolynomialReal other){
		for (Map.Entry<BigDecimal, BigDecimal> entry : other.coeffs.entrySet()) {
			BigDecimal otherExponent = entry.getKey();
			BigDecimal otherCoeff = entry.getValue();
			
			BigDecimal thisCoeff = coeffs.get(otherExponent);
			if (thisCoeff == null) {
				coeffs.put(otherExponent, otherCoeff);
			} else {
				coeffs.put(otherExponent, thisCoeff.add(otherCoeff));
			}
		}
	}
	/**
	 * Adds other polynomial to this using the given MathContext
	 * @param other	polynomial
	 * @param mc MathContext to use for the addition
	 */
	public void add(PolynomialReal other, MathContext mc){
		for (Map.Entry<BigDecimal, BigDecimal> entry : other.coeffs.entrySet()) {
			BigDecimal otherExponent = entry.getKey();
			BigDecimal otherCoeff = entry.getValue();
			
			BigDecimal thisCoeff = coeffs.get(otherExponent);
			if (thisCoeff == null) {
				coeffs.put(otherExponent, otherCoeff);
			} else {
				coeffs.put(otherExponent, thisCoeff.add(otherCoeff, mc));
			}
		}
	}
	/**
	 * Substracts other polynomial from this
	 * @param other	polynomial
	 */
	public void subtract(PolynomialReal other){
		for (Map.Entry<BigDecimal, BigDecimal> entry : other.coeffs.entrySet()) {
			BigDecimal otherExponent = entry.getKey();
			BigDecimal otherCoeff = entry.getValue();
			
			BigDecimal thisCoeff = coeffs.get(otherExponent);
			if (thisCoeff == null) {
				coeffs.put(otherExponent, otherCoeff);
			} else {
				coeffs.put(otherExponent, thisCoeff.subtract(otherCoeff));
			}
		}
	}
	
	/**
	 * Multiplies this polynomial with other
	 * @param other other polynomial
	 */
	public void multiply(PolynomialReal other){
		Map<BigDecimal, BigDecimal> newc = new HashMap<BigDecimal, BigDecimal>();
		
		for (Map.Entry<BigDecimal, BigDecimal> thisEntry : this.coeffs.entrySet()) {
			BigDecimal thisExponent = thisEntry.getKey();
			BigDecimal thisCoeff = thisEntry.getValue();
			for (Map.Entry<BigDecimal, BigDecimal> otherEntry : other.coeffs.entrySet()) {
				BigDecimal otherExponent = otherEntry.getKey();
				BigDecimal otherCoeff = otherEntry.getValue();
				
				BigDecimal multExponent = thisExponent.multiply(otherExponent);
				BigDecimal multCoeff = thisCoeff.multiply(otherCoeff);
				
				if (newc.get(multExponent) == null) {
					newc.put(multExponent, multCoeff);
				} else {
					newc.put(otherExponent, newc.get(multExponent).add(multCoeff));
				}
			}
		}
		
		this.coeffs = newc;
	}
	
	/**
	 * Multiplies this polynomial with other using the given MathContext
	 * @param other other polynomial
	 * @param mc MathContext to use
	 */
	public void multiply(PolynomialReal other, MathContext mc){
		Map<BigDecimal, BigDecimal> newc = new HashMap<BigDecimal, BigDecimal>();
		
		for (Map.Entry<BigDecimal, BigDecimal> thisEntry : this.coeffs.entrySet()) {
			BigDecimal thisExponent = thisEntry.getKey();
			BigDecimal thisCoeff = thisEntry.getValue();
			for (Map.Entry<BigDecimal, BigDecimal> otherEntry : other.coeffs.entrySet()) {
				BigDecimal otherExponent = otherEntry.getKey();
				BigDecimal otherCoeff = otherEntry.getValue();
				
				BigDecimal multExponent = thisExponent.multiply(otherExponent, mc);
				BigDecimal multCoeff = thisCoeff.multiply(otherCoeff, mc);
				
				if (newc.get(multExponent) == null) {
					newc.put(multExponent, multCoeff);
				} else {
					newc.put(otherExponent, newc.get(multExponent).add(multCoeff, mc));
				}
			}
		}
		
		this.coeffs = newc;
	}
	
	/**
	 * Multiplies this polynomial with scalar
	 * @param scalar scalar to multiply with
	 */
	public void multiplyWithScalar(BigDecimal scalar){
		for (Map.Entry<BigDecimal, BigDecimal> thisEntry : this.coeffs.entrySet()) {
			BigDecimal thisExponent = thisEntry.getKey();
			BigDecimal thisCoeff = thisEntry.getValue();
			
			this.coeffs.put(thisExponent, thisCoeff.multiply(scalar));
		}
	}
	/**
	 * Multiplies this polynomial with scalar using the given MathContext
	 * @param scalar scalar to multiply with
	 * @param mc MathContext to use for the multiplication
	 */
	public void multiplyWithScalar(BigDecimal scalar, MathContext mc){
		for (Map.Entry<BigDecimal, BigDecimal> thisEntry : this.coeffs.entrySet()) {
			BigDecimal thisExponent = thisEntry.getKey();
			BigDecimal thisCoeff = thisEntry.getValue();
			
			this.coeffs.put(thisExponent, thisCoeff.multiply(scalar, mc));
		}
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		Set<BigDecimal> orderedExponents = new TreeSet<BigDecimal>(coeffs.keySet());

		for(BigDecimal exponent : orderedExponents){
			BigDecimal coeff = coeffs.get(exponent);
			if(coeff.compareTo(BigDecimal.ZERO) != 0){
				if(coeff.compareTo(new BigDecimal("0")) > 0)
					sb.append("+");
				sb.append(coeff.toString());
				sb.append('x');
				sb.append("^" + exponent);
			}
		}
		if(sb.length() == 0) sb.append(0);
		return sb.toString();
	}	
}