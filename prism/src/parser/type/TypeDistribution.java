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
	
	/**
	 * e.g. "Erlang distribution"
	 */
	@Override
	public String getTypeString()
	{
		return "distribution";
	}
	
	public static TypeDistribution getInstance()
	{
		return singleton;
	}
	
	/**
	 * @param firstType
	 * @param secondType
	 * @return If both types can be assigned, returns 0.
	 *         Else returns the index of first wrong type (firstType starts with 1).
	 */
	public int canAssign(Type firstType, Type secondType)
	{
		// Play safe: assume not possible
		return 1;
	}
	
	/**
	 * Checks whether the given parameter values fit constraints of this distribution.
	 * For example, Dirac distribution parameter should be >0.
	 * @return true if the parameters have sensible values, else throws exception or returns false
	 * @throws PrismLangException if the parameters don't have sensible values
	 */
	public boolean parameterValueCheck(Expression firstParameter, Expression secondParameter, Values constantValues)  throws PrismLangException{
		return false;
	}
	
	/**
	 * @return The number of parameters used by this distribution.
	 */
	public int getNumParams() {
		return 0;
	}
	

}