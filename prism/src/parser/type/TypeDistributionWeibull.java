//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

public class TypeDistributionWeibull extends TypeDistribution {

	private static TypeDistributionWeibull singleton;
	
	static
	{
		singleton = new TypeDistributionWeibull();
	}
	
	private TypeDistributionWeibull()
	{		
	}	
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeDouble);
	}
	
	@Override
	public String getTypeString()
	{
		return "weibull distribution";
	}
	
	@Override
	public Object defaultValue()
	{
		throw new UnsupportedOperationException("not yet implemented");
		//return new ExponentialDistr(1.0); // TODO MAJO
	}
	
	public static TypeDistributionWeibull getInstance()
	{
		return singleton;
	}
	
	@Override
	public int canAssign(Type firstType, Type secondType)
	{
		if (!(firstType instanceof TypeDouble || firstType instanceof TypeInt)) { // if first type NOK then report bad first type
			return 1;
		}
		if (!(secondType instanceof TypeDouble || secondType instanceof TypeInt)) { // if second type NOK then report bad second type
			return 2;
		}
		return 0; // else all OK
	}
	
	/**
	 * Checks whether the distribution has sensible parameter values. For example, dirac distribution parameter should be >0.
	 * @return true if the parameters have sensible values
	 * @throws PrismLangException if the parameters dont have sensible values
	 */
	// TODO MAJO - not sure if calling evaluateDouble() is safe.
	@Override
	public boolean parameterValueCheck(Expression firstParameter, Expression secondParameter, Values constantValues) throws PrismLangException{
		if ((double)firstParameter.evaluateDouble(constantValues) <= 0) {
			throw new PrismLangException("Weibull distribution must have two parameters of values >0", firstParameter);
		}
		if ((double)secondParameter.evaluateDouble(constantValues) <= 0) {
			throw new PrismLangException("Weibull distribution must have two parameters of values >0", secondParameter);
		}
		return true;
	}
	
	@Override
	public Double castValueTo(Object value) throws PrismLangException
	{
		throw new UnsupportedOperationException("not yet implemented");
		
		/* TODO MAJO
		if (value instanceof Distribution)
			return (Distribution) value;
		else
			throw new PrismLangException("Can't convert " + value.getClass() + " to type " + getTypeString());
			*/
	}

}