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

package parser.ast;

import parser.visitor.*;
import prism.PrismLangException;

/**
 * Simple property data structure used for GSMP parameter synthesizing.
 * Holds the name of the event, index of the parameter (starting from 1), and lower and upper bounds.
 * E.g. for (ev1,1,0..5.5) we are synthesizing the first parameter of event ev1
 * to, say, maximize steady-state rewards, and the interval
 * which we consider when looking for the result is [0, 5.5].
 */
public class ParameterToSynthesize extends ASTElement
{
	/** PRISM expression representing the name of the event */
	private ExpressionIdent eventNameExpr;
	/** Index of the event parameter starting from 1.
	 *  E.g. parameter 1 of Dirac-distributed events is the timeout,
	 *  and parameter 1 and 2 of uniformly distributed events are a and b.*/
	private Expression paramIndexExpr;
	/** Lower bound of the result */
	private Expression lowerBoundExpr;
	/** Upper bound of the result */
	private Expression upperBoundExpr;
	
	// The expressions are evaluated during parsing semantics check and kept here
	private String eventName = null;
	private int paramIndex = 0;
	private double lowerBound = 0.0;
	private double upperBound = 0.0;
	
	private Property parent = null;

	// Constructor

	public ParameterToSynthesize(ExpressionIdent eventName, Expression paramIndex, Expression lowerBound, Expression upperBound)
	{
		this.eventNameExpr = eventName;
		this.paramIndexExpr = paramIndex;
		this.lowerBoundExpr = lowerBound;
		this.upperBoundExpr = upperBound;
		
		setBeginColumn(eventNameExpr.beginColumn);
		setBeginLine(eventNameExpr.beginLine);
		setEndColumn(upperBoundExpr.endColumn);
		setEndLine(upperBoundExpr.endLine);
	}

	// Accessors
	
	public Property getParent() {
		return parent;
	}

	public ExpressionIdent getEventExpr()
	{
		return eventNameExpr;
	}

	public Expression getParamIndexExpr()
	{
		return paramIndexExpr;
	}
	
	public Expression getLowerBoundExpr()
	{
		return lowerBoundExpr;
	}

	public Expression getUpperBoundExpr()
	{
		return upperBoundExpr;
	}
	
	public String getEventName() {
		return eventName;
	}
	
	public int getParamIndex() {
		return paramIndex;
	}
	
	public double getLowerBound() {
		return lowerBound;
	}
	
	public double getUpperBound() {
		return upperBound;
	}
	
	// Setters
	
	public void setParent(Property property) {
		this.parent = property;
	}

	public void setLowerBound(double lowerBound) {
		this.lowerBound = lowerBound;
	}

	public void setUpperBound(double upperBound) {
		this.upperBound = upperBound;
	}

	public void setParamIndex(int paramIndex) {
		this.paramIndex = paramIndex;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	
	/** Only used for AST traverse modify. Do not use elsewhere! */
	public void setParamIndexExpr(Expression paramIndexExpr) {
		this.paramIndexExpr = paramIndexExpr;
	}
	
	/** Only used for AST traverse modify. Do not use elsewhere! */
	public void setLowerBoundExpr(Expression lowerBoundExpr) {
		this.lowerBoundExpr = lowerBoundExpr;
	}
	
	/** Only used for AST traverse modify. Do not use elsewhere! */
	public void setUpperBoundExpr(Expression upperBoundExpr) {
		this.upperBoundExpr = upperBoundExpr;
	}

	// Methods required for ASTElement:

	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public String toString()
	{
		String s = "";
		s += "(" + eventNameExpr + "," + paramIndexExpr + "," + lowerBoundExpr + ".." + upperBoundExpr + ")";
		return s;
	}

	@Override
	public ParameterToSynthesize deepCopy()
	{
		ParameterToSynthesize newParam = new ParameterToSynthesize(
				(ExpressionIdent)eventNameExpr.deepCopy(),
				paramIndexExpr.deepCopy(),
				lowerBoundExpr.deepCopy(),
				upperBoundExpr.deepCopy());
		newParam.setType(type);
		newParam.setPosition(this);
		newParam.setParent(this.getParent());
		return newParam;
	}
	
}

//------------------------------------------------------------------------------
