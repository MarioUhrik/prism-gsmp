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

public class TypeDistributionErlang extends TypeDistribution {

	private static TypeDistributionErlang singleton;
	
	static
	{
		singleton = new TypeDistributionErlang();
	}
	
	private TypeDistributionErlang()
	{		
	}	
	
	@Override
	public String getTypeString()
	{
		return "Erlang distribution";
	}
	
	@Override
	public Object defaultValue()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static TypeDistributionErlang getInstance()
	{
		return singleton;
	}
	
	@Override
	public int canAssign(Type firstType, Type secondType)
	{
		if (!(firstType instanceof TypeDouble || firstType instanceof TypeInt)) { // if first type NOK then report bad first type
			return 1;
		}
		if (!(secondType instanceof TypeInt)) { // if second type NOK then report bad second type
			return 2;
		}
		return 0; // else all OK
	}
	
	/**
	 * Checks whether the distribution has sensible parameter values. For example, dirac distribution parameter should be >0.
	 * @return true if the parameters have sensible values
	 * @throws PrismLangException if the parameters dont have sensible values
	 */
	@Override
	public boolean parameterValueCheck(Expression firstParameter, Expression secondParameter, Values constantValues) throws PrismLangException{
		if ((double)firstParameter.evaluateDouble(constantValues) <= 0) {
			throw new PrismLangException("First parameter of " + getTypeString() + " must be of value >0", firstParameter);
		}
		if ((int)secondParameter.evaluateInt(constantValues) <= 0) {
			throw new PrismLangException("Second parameter of " + getTypeString() + " must be an integer of value >0", secondParameter);
		}
		return true;
	}
	
	@Override
	public int getNumParams() {
		return 2;
	}
	

}