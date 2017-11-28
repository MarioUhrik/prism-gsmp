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

/**
 * General type distribution class.
 */
public class TypeDistribution extends Type {

	private static TypeDistribution singleton;
	
	static
	{
		singleton = new TypeDistribution();
	}
	
	@Override
	public String getTypeString()
	{
		return "distribution";
	}
	
	@Override
	public Object defaultValue()
	{
		throw new UnsupportedOperationException("not yet implemented");
		//return new ExponentialDistr(1.0); // TODO MAJO
	}
	
	public static TypeDistribution getInstance()
	{
		return singleton;
	}
	
	/**
	 * 
	 * @param firstType
	 * @param secondType
	 * @return 0 if both can be assigned, 1 if firstType cannot be assigned, 2 else (OK = 0, NOK = index of first wrong type)
	 */
	public int canAssign(Type firstType, Type secondType)
	{
		// TODO MAJO - this should be overriden by children classes for now
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Checks whether the distribution has sensible parameter values. For example, dirac distribution parameter should be >0.
	 * @return true if the parameters have sensible values
	 * @throws PrismLangException if the parameters dont have sensible values
	 */
	public boolean parameterValueCheck(Expression firstParameter, Expression secondParameter, Values constantValues)  throws PrismLangException{
		 // TODO MAJO - this should be overriden by children classes for now
		throw new UnsupportedOperationException("not yet implemented");
	}
	

}