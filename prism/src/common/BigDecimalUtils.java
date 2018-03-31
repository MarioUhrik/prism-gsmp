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
		
		int decimalDigitsPrecision = decimalDigitsPrecision(precision) + 1;
		for (int i = 1 ; i < decimalDigitsPrecision ; ++i) {
			factorial = factorial.multiply(new BigDecimal(i));
			factorial.round(MathContext.UNLIMITED);
			e = e.add(BigDecimal.ONE.divide(factorial, (int)decimalDigitsPrecision, RoundingMode.UP));
		}
		
		return e;
	}
	
	/**
	 * Compute the number of decimal digits corresponding to floating point {@code precision}.
	 * This number is increased by 3 to provide extra accuracy when the requested precision
	 * is very low.
	 * <br>
	 * IMPORTANT NOTE: Does not work for precision = 0
	 */
	public static int decimalDigitsPrecision(BigDecimal precision) {
		BigDecimal inverse = BigDecimal.ONE.divide(precision, RoundingMode.UP);
		BigInteger inverseInt = inverse.toBigInteger();
		int decimalDigits = 3;
		for (  ; inverseInt.compareTo(BigInteger.ONE) >= 0 ; ++decimalDigits) {
			inverseInt = inverseInt.divide(BigInteger.TEN);
		}
		return decimalDigits;
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
}
