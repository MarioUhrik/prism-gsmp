//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package parser.ast;

import java.util.Vector;

import parser.visitor.*;
import prism.PrismLangException;
import parser.type.*;

/**
 * Class to store list of defined distributions (based on ConstantList).
 */
public class DistributionList extends ASTElement
{
	// Name/firstParameter/secondParameter/type tuples to define distributions. E.g. "distr1",1.0,2.0,TypeDistributionUniform
	private Vector<String> names = new Vector<String>();
	private Vector<Expression> firstParameters = new Vector<Expression>(); // should not be null (distributions tend to have at least one parameter)
	private Vector<Expression> secondParameters = new Vector<Expression>(); // can be null (e.g. Dirac/Exponential distribution)
	private Vector<TypeDistribution> types = new Vector<TypeDistribution>();
	// We also store an ExpressionIdent to match each name.
	// This is to just to provide positional info.
	private Vector<ExpressionIdent> nameIdents = new Vector<ExpressionIdent>();
	
	private ModulesFile parent = null;
	
	/** Constructor */
	public DistributionList(ModulesFile parent)
	{
		this.parent = parent;
	}

	/** Constructor from a Values object, i.e., a list of name=value tuples */
	/*
	public DistributionList(Values constValues) throws PrismLangException
	{
		for (int i = 0; i < constValues.getNumValues(); i++) {
			Type type = constValues.getType(i);
			if (type.equals(TypeBool.getInstance()) ||
			    type.equals(TypeInt.getInstance()) ||
			    type.equals(TypeDouble.getInstance())) {
				addConstant(new ExpressionIdent(constValues.getName(i)),
				            new ExpressionLiteral(type, constValues.getValue(i)),
				            type);
			} else {
				throw new PrismLangException("Unsupported type for constant " + constValues.getName(i));
			}
		}
	}
	*/

	// Set methods
	
	public void addDistribution(ExpressionIdent n, Expression firstParam, Expression secondParam, TypeDistribution t)
	{
		names.addElement(n.getName());
		firstParameters.addElement(firstParam);
		secondParameters.addElement(secondParam);
		types.addElement(t);
		nameIdents.addElement(n);
	}
	
	public void setDistribution(int i, Expression firstParam, Expression secondParam) // TODO MAJO - might cause problems
	{
		setFirstParameter(i, firstParam);
		setSecondParameter(i, secondParam);
	}
	
	public void setFirstParameter(int i, Expression param) // TODO MAJO - might cause problems
	{
		firstParameters.setElementAt(param, i);
	}
	
	public void setSecondParameter(int i, Expression param) // TODO MAJO - might cause problems
	{
		secondParameters.setElementAt(param, i);
	}
	
	public void setParent(ModulesFile parent)
	{
		this.parent = parent;
	}
	
	// Get methods

	public int size()
	{
		return names.size();
	}

	public String getDistributionName(int i)
	{
		return names.elementAt(i);
	}
	
	public Expression getFirstParameter(int i)
	{
		return firstParameters.elementAt(i);
	}
	
	public Expression getSecondParameter(int i)
	{
		return secondParameters.elementAt(i);
	}
	
	public TypeDistribution getDistributionType(int i)
	{
		return types.elementAt(i);
	}
	
	public ExpressionIdent getDistributionNameIdent(int i)
	{
		return nameIdents.elementAt(i);
	}
	
	public ModulesFile getParent()
	{
		return parent;
	}

	/**
	 * Get the index of a distribution by its name (returns -1 if it does not exist).
	 */
	public int getDistributionIndex(String s)
	{
		return names.indexOf(s);
	}

	/**
	 * Remove the constant with the given name.
	 * @param name the name of the constant
	 * @param ignoreNonexistent if true, don't throw an exception if the constant does not exist
	 * @throws PrismLangException if the constant does not exist (if not ignoreNonexistent)
	 */
	public void removeDistribution(String name, boolean ignoreNonexistent) throws PrismLangException
	{
		int distributionIndex = getDistributionIndex(name);
		if (distributionIndex == -1) {
			if (ignoreNonexistent) {
				return;
			}
			throw new PrismLangException("Can not remove nonexistent distribution: " + name);
		}
		removeDistribution(distributionIndex);
	}

	/**
	 * Remove the constant with the given index.
	 * @param i the index
	 */
	public void removeDistribution(int i)
	{
		names.remove(i);
		firstParameters.remove(i);
		secondParameters.remove(i);
		types.remove(i);
		nameIdents.remove(i);
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		int i, n;
		Expression e;
		
		n = names.size();
		for (i = 0; i < n; i++) {
			s += "distribution ";
			s += getDistributionType(i).getTypeString() + " ";
			s += getDistributionName(i);
			e = getFirstParameter(i);
			if (e != null) {
				s += " = " + e;
			}
			e = getSecondParameter(i);
			if (e != null) {
				s += " = " + e;
			}
			s += ";\n";
		}
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		int i, n;
		DistributionList ret = new DistributionList(getParent());
		n = size();
		for (i = 0; i < n; i++) {
			Expression firstParam = (getFirstParameter(i) == null) ? null : getFirstParameter(i).deepCopy();
			Expression secondParam = (getSecondParameter(i) == null) ? null : getSecondParameter(i).deepCopy();
			ret.addDistribution((ExpressionIdent)getDistributionNameIdent(i).deepCopy(), firstParam, secondParam, getDistributionType(i));
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------