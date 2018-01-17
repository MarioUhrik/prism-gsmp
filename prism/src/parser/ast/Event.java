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


import parser.visitor.*;
import prism.PrismLangException;

/**
 * Class to store a parsed event for GSMP models.
 * Each event has its own identifier,
 * and also an identifier of the assigned distribution (see class DistributionList)
 */
public class Event extends ASTElement
{
	// Name of the event and the name of the distribution assigned to it
	private String eventName;
	private ExpressionIdent eventNameIdent;
	
	private String distributionName;
	private ExpressionIdent distributionNameIdent;
	
	//parent module
	private Module parent;
	
	/** Constructor */

	public Event(ExpressionIdent eventEI, ExpressionIdent distributionEI)
	{
		this.eventNameIdent = eventEI;
		this.eventName = eventEI.getName();
		
		this.distributionNameIdent = distributionEI;
		this.distributionName = distributionEI.getName();
		
		setBeginColumn(0);
		setBeginLine(eventEI.getBeginLine());
		if (distributionEI.hasPosition()) {
			setEndColumn(distributionEI.getEndColumn()+1);
			setEndLine(distributionEI.getEndLine());
		} else {
			setEndColumn(eventEI.getEndColumn() +1);
			setEndLine(eventEI.getEndLine());
		}
	}

	// Get methods

	public String getEventName()
	{
		return eventName;
	}
	
	public String getDistributionName()
	{
		return distributionName;
	}
	
	public ExpressionIdent getEventNameIdent()
	{
		return eventNameIdent;
	}
	
	public ExpressionIdent getDistributionNameIdent()
	{
		return distributionNameIdent;
	}
	
	public Module getParent() {
		return parent;
	}
	
	// Set methods
	
	public void setEventNameIdent(ExpressionIdent eventNameIdent)
	{
		this.eventNameIdent = eventNameIdent;
		this.eventName = eventNameIdent.getName();
		setBeginLine(eventNameIdent.getBeginLine());
	}
	
	public void setDistributionNameIdent(ExpressionIdent distributionNameIdent)
	{
		this.distributionNameIdent = distributionNameIdent;
		this.distributionName = distributionNameIdent.getName();
		if (distributionNameIdent.hasPosition()) {
			setEndColumn(distributionNameIdent.getEndColumn()+1);
			setEndLine(distributionNameIdent.getEndLine());
		}
	}
	
	public void setEventName(String eventName) //added for module renaming
	{
		this.eventName = eventName;
	}
	
	public void setParent(Module parent) {
		this.parent = parent;
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
		
		s += "event ";
		s += getEventName() + " = ";
		s += getDistributionName();
		s += ";";
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		Event ret = new Event((ExpressionIdent)getEventNameIdent().deepCopy(), (ExpressionIdent)getDistributionNameIdent().deepCopy());
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------