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

package common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import ch.obermuhlner.math.big.BigDecimalMath;

/**
 * Utility class for BigDecimal.
 * However, BigDecimalMath.jar should be used
 * for all serious mathematical functions instead.
 */
public class BigDecimalUtils {
	
	/**
	 * Computes and returns the Euler's number up to precision {@code precision}.
	 * @param precision required precision
	 */
	@Deprecated
	public static BigDecimal e(BigDecimal precision) {
		BigDecimal e = BigDecimal.ONE; // 1
		BigDecimal factorial = BigDecimal.ONE; // 1
		
		int decimalDigitsPrecision = decimalDigits(precision) + 1;
		for (int i = 1 ; i < decimalDigitsPrecision ; ++i) {
			factorial = factorial.multiply(new BigDecimal(i));
			factorial.round(MathContext.UNLIMITED);
			e = e.add(BigDecimal.ONE.divide(factorial, (int)decimalDigitsPrecision, RoundingMode.UP));
		}
		
		return e;
	}
	
	/**
	 * Compute the number of decimal digits corresponding to floating point {@code allowedError}.
	 * The returned number is increased by 1 to make up for potential errors.
	 * @param allowedError supposed to be a number greater than 0 but smaller than 1. For example, 0.0001 or 1.0e-10.
	 * <br>
	 * IMPORTANT NOTE: Does not work for precision = 0
	 */
	public static int decimalDigits(BigDecimal allowedError) {
		BigDecimal inverse = BigDecimal.ONE.divide(allowedError, RoundingMode.UP);
		// TODO MAJO - maybe the increase by 3 is not so nice in general
		// TODO MAJO - rewrite to decimal logarithm
		BigInteger inverseInt = inverse.toBigInteger();
		int decimalDigits = 3;
		for (  ; inverseInt.compareTo(BigInteger.ONE) >= 0 ; ++decimalDigits) {
			inverseInt = inverseInt.divide(BigInteger.TEN);
		}
		return decimalDigits;
	}
	
	/**
	 * Compute precision corresponding to a number of decimal floating point digits {@code digits}.
	 * Input digits are increased by 1 to make up for potential errors.
	 * @param decimalDigits supposed to be a number of decimal floating point digits, e.g. 100 should return 1E-100.
	 */
	public static BigDecimal allowedError(int decimalDigits) {
		// TODO MAJO - maybe the increase by 3 is not so nice in general
		MathContext mc = new MathContext(decimalDigits, RoundingMode.HALF_UP);
		return BigDecimalMath.pow(BigDecimal.TEN, new BigDecimal(decimalDigits + 3, mc).negate(), mc);
	}
	
	/**
	 * Returns a number with the smallest value.
	 */
	public static BigDecimal min(BigDecimal a, BigDecimal b) {
		if (a.compareTo(b) < 0) {
			return a;
		} else {
			return b;
		}
	}
	
	/**
	 * Returns a number with the highest value.
	 */
	public static BigDecimal max(BigDecimal a, BigDecimal b) {
		if (a.compareTo(b) > 0) {
			return a;
		} else {
			return b;
		}
	}
}
