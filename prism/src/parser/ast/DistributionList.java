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
import prism.PrismUtils;
import parser.type.*;

/**
 * Class to store list of defined distributions (based on ConstantList).
 */
public class DistributionList extends ASTElement
{
	// Name/firstParameter/secondParameter/type triples to define distributions. E.g. "distr1",1.0,2.0,TypeDistributionUniform
	private Vector<String> names = new Vector<String>();
	private Vector<Expression> firstParameters = new Vector<Expression>(); // should not be null (distributions tend to have at least one parameter)
	private Vector<Expression> secondParameters = new Vector<Expression>(); // can be null (e.g. Dirac/Exponential distribution)
	private Vector<TypeDistribution> types = new Vector<TypeDistribution>();
	// We also store an ExpressionIdent to match each name.
	// This is to just to provide positional info.
	private Vector<ExpressionIdent> nameIdents = new Vector<ExpressionIdent>();
	
	/** Constructor */
	public DistributionList()
	{
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

	/**
	 * Find cyclic dependencies.
	 */
	public void findCycles() throws PrismLangException // TODO MAJO - not sure if it really works this way. Also, could be done in a less nasty way
	{
		// Create boolean matrix of dependencies
		// (matrix[i][j] is true if constant i contains constant j)
		int n = firstParameters.size() * 2;
		boolean matrix[][] = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			Expression e;
			if (i < (n/2)) {
				e = getFirstParameter(i);
			} else {
				e = getSecondParameter(i % (n/2));
			}
			if (e != null) {
				Vector<String> v = e.getAllConstants();
				for (int j = 0; j < v.size(); j++) {
					int k = getDistributionIndex(v.elementAt(j));
					if (k != -1) {
						matrix[i][k] = true;
					}
				}
			}
		}
		// Check for and report dependencies
		int firstCycle = PrismUtils.findCycle(matrix);
		if (firstCycle != -1) {
			int index;
			Expression e;
			if (firstCycle < (n/2)) {
				index = firstCycle;
				e = getFirstParameter(index);
			} else {
				index = firstCycle % (n/2);
				e = getSecondParameter(index);
			}
			String s = "Cyclic dependency in definition of distribution \"" + getDistributionName(index) + "\"";
			
			throw new PrismLangException(s, e);
		}
	}
	
	/**
	 * Get the number of undefined constants in the list.
	 */
	/*
	public int getNumUndefined()
	{
		int i, n, res;
		Expression e;
		
		res = 0;
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e == null) {
				res++;
			}
		}
		
		return res;
	}
	*/
	
	/**
	 * Get a list of the undefined constants in the list.
	 */
	/*
	public Vector<String> getUndefinedConstants()
	{
		int i, n;
		Expression e;
		Vector<String> v;
		
		v = new Vector<String>();
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e == null) {
				v.addElement(getConstantName(i));
			}
		}
		
		return v;
	}
	*/
	
	/**
	 * Check if {@code name} is a *defined* constants in the list,
	 * i.e. a constant whose value was *not* left unspecified in the model/property.
	 */
	/*
	public boolean isDefinedConstant(String name)
	{
		int i = getConstantIndex(name);
		if (i == -1)
			return false;
		return (getConstant(i) != null);
	}
	*/
	
	/**
	 * Set values for *all* undefined constants, evaluate values for *all* constants
	 * and return a Values object with values for *all* constants.
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined
	 * Argument 'otherValues' contains any other values which may be needed, null if none
	 */
	/*
	public Values evaluateConstants(Values someValues, Values otherValues) throws PrismLangException
	{
		return evaluateSomeOrAllConstants(someValues, otherValues, true);
	}
	*/
	
	/**
	 * Set values for *some* undefined constants, evaluate values for constants where possible
	 * and return a Values object with values for all constants that could be evaluated.
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined
	 * Argument 'otherValues' contains any other values which may be needed, null if none
	 */
	/*
	public Values evaluateSomeConstants(Values someValues, Values otherValues) throws PrismLangException
	{
		return evaluateSomeOrAllConstants(someValues, otherValues, false);
	}
	*/
	
	/**
	 * Set values for *some* or *all* undefined constants, evaluate values for constants where possible
	 * and return a Values object with values for all constants that could be evaluated.
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined.
	 * Argument 'otherValues' contains any other values which may be needed, null if none.
	 * If argument 'all' is true, an exception is thrown if any undefined constant is not defined.
	 */
	/*
	private Values evaluateSomeOrAllConstants(Values someValues, Values otherValues, boolean all) throws PrismLangException
	{
		DistributionList cl;
		Expression e;
		Values allValues;
		int i, j, n, numToEvaluate;
		Type t = null;
		ExpressionIdent s;
		Object val;
		
		// Create new copy of this ConstantList
		// (copy existing constant definitions, add new ones where undefined)
		cl = new DistributionList();
		n = constants.size();
		for (i = 0; i < n; i++) {
			s = getConstantNameIdent(i);
			e = getConstant(i);
			t = getConstantType(i);
			if (e != null) {
				cl.addConstant((ExpressionIdent)s.deepCopy(), e.deepCopy(), t);
			} else {
				// Create new literal expression using values passed in (if possible and needed)
				if (someValues != null && (j = someValues.getIndexOf(s.getName())) != -1) {
					cl.addConstant((ExpressionIdent) s.deepCopy(), new ExpressionLiteral(t, t.castValueTo(someValues.getValue(j))), t);
				} else {
					if (all)
						throw new PrismLangException("No value specified for constant", s);
				}
			}
		}
		numToEvaluate = cl.size();
		
		// Now add constants corresponding to the 'otherValues' argument to the new constant list
		if (otherValues != null) {
			n = otherValues.getNumValues();
			for (i = 0; i < n; i++) {
				Type iType = otherValues.getType(i);
				cl.addConstant(new ExpressionIdent(otherValues.getName(i)), new ExpressionLiteral(iType, iType.castValueTo(otherValues.getValue(i))), iType);
			}
		}
		
		// Go trough and expand definition of each constant
		// (i.e. replace other constant references with their definitions)
		// Note: work with new copy of constant list, don't need to expand 'otherValues' ones.
		for (i = 0; i < numToEvaluate; i++) {
			try {
				e = (Expression)cl.getConstant(i).expandConstants(cl);
				cl.setConstant(i, e);
			} catch (PrismLangException ex) {
				if (all) {
					throw ex;
				} else {
					cl.setConstant(i, null);
				}
			}
		}
		
		// Evaluate constants and store in new Values object (again, ignoring 'otherValues' ones)		
		allValues = new Values();
		for (i = 0; i < numToEvaluate; i++) {
			if (cl.getConstant(i) != null) {
				val = cl.getConstant(i).evaluate(null, otherValues);
				allValues.addValue(cl.getConstantName(i), val);
			}
		}
		
		return allValues;
	}
	*/

	/**
	 * Set values for some undefined constants, then partially evaluate values for constants where possible
	 * and return a map from constant names to the Expression representing its value. 
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined.
	 * Argument 'otherValues' contains any other values which may be needed, null if none.
	 */
	/*
	public Map<String,Expression> evaluateConstantsPartially(Values someValues, Values otherValues) throws PrismLangException
	{
		DistributionList cl;
		Expression e;
		int i, j, n, numToEvaluate;
		Type t = null;
		ExpressionIdent s;
		
		// Create new copy of this ConstantList
		// (copy existing constant definitions, add new ones where undefined)
		cl = new DistributionList();
		n = constants.size();
		for (i = 0; i < n; i++) {
			s = getConstantNameIdent(i);
			e = getConstant(i);
			t = getConstantType(i);
			if (e != null) {
				cl.addConstant((ExpressionIdent)s.deepCopy(), e.deepCopy(), t);
			} else {
				// Create new literal expression using values passed in (if possible and needed)
				if (someValues != null && (j = someValues.getIndexOf(s.getName())) != -1) {
					cl.addConstant((ExpressionIdent) s.deepCopy(), new ExpressionLiteral(t, t.castValueTo(someValues.getValue(j))), t);
				}
			}
		}
		numToEvaluate = cl.size();
		
		// Now add constants corresponding to the 'otherValues' argument to the new constant list
		if (otherValues != null) {
			n = otherValues.getNumValues();
			for (i = 0; i < n; i++) {
				Type iType = otherValues.getType(i);
				cl.addConstant(new ExpressionIdent(otherValues.getName(i)), new ExpressionLiteral(iType, iType.castValueTo(otherValues.getValue(i))), iType);
			}
		}
		
		// Go trough and expand definition of each constant
		// (i.e. replace other constant references with their definitions)
		// Note: work with new copy of constant list, don't need to expand 'otherValues' ones.
		for (i = 0; i < numToEvaluate; i++) {
			try {
				e = (Expression)cl.getConstant(i).expandConstants(cl);
				cl.setConstant(i, e);
			} catch (PrismLangException ex) {
				cl.setConstant(i, null);
			}
		}
		
		// Store final expressions for each constant in a map and return
		Map<String,Expression> constExprs = new HashMap<>();
		for (i = 0; i < numToEvaluate; i++) {
			if (cl.getConstant(i) != null) {
				constExprs.put(cl.getConstantName(i), cl.getConstant(i).deepCopy());
			}
		}
		
		return constExprs;
	}
	*/

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
		DistributionList ret = new DistributionList();
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