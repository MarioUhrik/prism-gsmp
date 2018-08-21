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

import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Interface representing polynomials from {@link common.polynomials}
 */
public interface Poly {

	/**
	 * Compute the derivative of this 
	 * @return derivative of this
	 */
	public abstract Poly derivative();
	
	/**
	 * Compute the derivative of this using the given MathContext
	 * @param mc MathContext to use
	 * @return derivative of this
	 */
	public abstract Poly derivative(MathContext mc);
	
	/**
	 * Compute the antiderivative of this using the given MathContext
	 * @param mc MathContext to use
	 * @return antiderivative of this
	 * @throws PrismException in some extreme cases - look into the individual implementations
	 */
	public abstract Poly antiderivative(MathContext mc) throws PrismException;
	
	/**
	 * Evaluate this polynomial in x
	 * @param	x	x to evaluate
	 * @return 	value of this in x
	 */
	public abstract BigDecimal value(BigDecimal x);
	
	/**
	 * Evaluate this polynomial in x using the given MathContext
	 * @param	x	x to evaluate
	 * @param   mc MathContext to use for the evaluation
	 * @return 	value of this in x
	 */
	public abstract BigDecimal value(BigDecimal x, MathContext mc);
	
	/**
	 * Returns leading coefficient
	 * @return	leading coefficient
	 */
	public abstract BigDecimal getHighestCoeff();

	
	/**
	 * Multiplies this polynomial with scalar
	 * @param scalar scalar to multiply with
	 */
	public abstract void multiplyWithScalar(BigDecimal scalar);
	
	/**
	 * Multiplies this polynomial with scalar using the given MathContext
	 * @param scalar scalar to multiply with
	 * @param mc MathContext to use for the multiplication
	 */
	public abstract void multiplyWithScalar(BigDecimal scalar, MathContext mc);
	
	/**
	 * Adds other polynomial to this
	 * @param other	polynomial
	 * @throws PrismNotSupportedException when Poly other has unsupported type
	 */
	public abstract void add(Poly other) throws PrismNotSupportedException;
	
	/**
	 * Adds other polynomial to this using the given MathContext
	 * @param other	polynomial
	 * @param mc MathContext to use for the addition
	 * @throws PrismNotSupportedException when Poly other has unsupported type
	 */
	public abstract void add(Poly other, MathContext mc) throws PrismNotSupportedException;
	
	/**
	 * Subtracts other polynomial from this
	 * @param other	polynomial
	 * @throws PrismNotSupportedException when Poly other has unsupported type
	 */
	public abstract void subtract(Poly other) throws PrismNotSupportedException;
	
	/**
	 * Multiplies this polynomial with other
	 * @param other other polynomial
	 * @throws PrismNotSupportedException when Poly other has unsupported type
	 */
	public abstract void multiply(Poly other) throws PrismNotSupportedException;
	
	/**
	 * Multiplies this polynomial with other using the given MathContext
	 * @param other other polynomial
	 * @param mc MathContext to use
	 * @throws PrismNotSupportedException when Poly other has unsupported type
	 */
	public abstract void multiply(Poly other, MathContext mc) throws PrismNotSupportedException;
	
}