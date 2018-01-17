//==============================================================================
//	
//	Copyright (c) 2017-
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

package parser.type;

import parser.Values;
import parser.ast.Expression;
import prism.PrismLangException;

/**
 * Class intended to represent the type of continuous Weibull probability distribution.
 */
public class TypeDistributionWeibull extends TypeDistribution {

	private static TypeDistributionWeibull singleton;
	
	static
	{
		singleton = new TypeDistributionWeibull();
	}
	
	private TypeDistributionWeibull()
	{		
	}	
	
	@Override
	public String getTypeString()
	{
		return "Weibull distribution";
	}
	
	@Override
	public Object defaultValue()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static TypeDistributionWeibull getInstance()
	{
		return singleton;
	}
	
	@Override
	public int canAssign(Type firstType, Type secondType)
	{
		if (!(firstType instanceof TypeDouble || firstType instanceof TypeInt)) {
			return 1;
		}
		if (!(secondType instanceof TypeDouble || secondType instanceof TypeInt)) {
			return 2;
		}
		return 0;
	}
	
	@Override
	public boolean parameterValueCheck(Expression firstParameter, Expression secondParameter, Values constantValues) throws PrismLangException{
		if ((double)firstParameter.evaluateDouble(constantValues) <= 0) {
			throw new PrismLangException(getTypeString() + " must have two parameters of values >0", firstParameter);
		}
		if ((double)secondParameter.evaluateDouble(constantValues) <= 0) {
			throw new PrismLangException(getTypeString() + " must have two parameters of values >0", secondParameter);
		}
		return true;
	}
	
	@Override
	public int getNumParams() {
		return 2;
	}
	
	@Override
	public double cdf(double firstParameter, double secondParameter, double x) {
		// TODO MAJO - implement
		// Specialized probability distribution library should be considered.
		throw new UnsupportedOperationException("Weibull distribution CDF computation is not yet implemented!");
	}

}