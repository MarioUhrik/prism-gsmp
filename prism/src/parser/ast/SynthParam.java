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

package parser.ast;

import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.visitor.*;
import prism.PrismLangException;

/**
 * Simple property data structure used for GSMP parameter synthesis.
 * Holds the name of the event, index of the parameter (starting from 1), and lower and upper bounds.
 * <br>
 * E.g. for (ev1,1,0..5.5) we are synthesizing the first parameter of event ev1
 * to, say, maximize steady-state rewards, and the interval
 * from which the result is picked is within bounds 0>=, <=5.5.
 */
public class SynthParam extends ASTElement
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
	
	// The expressions are evaluated during parse-time semantics check,
	// and kept here so that they do not need to be evaluated again.
	private String eventName = null;
	private int paramIndex = 0;
	private double lowerBound = 0.0;
	private double upperBound = 0.0;
	
	private Property parent = null;

	// Constructor

	public SynthParam(ExpressionIdent eventName, Expression paramIndex, Expression lowerBound, Expression upperBound)
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

	/** Use with caution - make sure the newly set value would not cause trouble later */
	public void setLowerBound(double lowerBound) {
		Expression tmp = new ExpressionLiteral(TypeDouble.getInstance(), lowerBound);
		tmp.setPosition(lowerBoundExpr);
		this.lowerBoundExpr = tmp;
		this.lowerBound = lowerBound;
	}

	/** Use with caution - make sure the newly set value would not cause trouble later */
	public void setUpperBound(double upperBound) {
		Expression tmp = new ExpressionLiteral(TypeDouble.getInstance(), upperBound);
		tmp.setPosition(upperBoundExpr);
		this.upperBoundExpr = tmp;
		this.upperBound = upperBound;
	}

	/** Use with caution - make sure the newly set value would not cause trouble later */
	public void setParamIndex(int paramIndex) {
		Expression tmp = new ExpressionLiteral(TypeInt.getInstance(), paramIndex);
		tmp.setPosition(paramIndexExpr);
		this.paramIndexExpr = tmp;
		this.paramIndex = paramIndex;
	}

	/** Use with caution - make sure the newly set value would not cause trouble later */
	public void setEventName(String eventName) {
		ExpressionIdent tmp = new ExpressionIdent(eventName);
		tmp.setPosition(eventNameExpr);
		this.eventNameExpr = tmp;
		this.eventName = tmp.getName();
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
		s += "(" + eventNameExpr + ", " + paramIndexExpr + ", " + lowerBoundExpr + ".." + upperBoundExpr + ")";
		return s;
	}

	@Override
	public SynthParam deepCopy()
	{
		SynthParam newParam = new SynthParam(
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
