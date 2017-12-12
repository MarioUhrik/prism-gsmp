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

package explicit;

import java.util.*;

import parser.type.TypeDistribution;

/**
 * Explicit engine class representing a GSMP event.
 * 
 * GSMP events hold a time distribution type and parameters,
 * and a distribution on states for each state (hence extends DTMCSimple).
 */
public class GSMPEvent extends DTMCSimple 
{
	private TypeDistribution distributionType;
	private double firstParameter;
	private double secondParameter;
	//BitSet of states where this event is active. However, it is redundant.
	//This information is already stored in the state distribution matrix anyway.
	private BitSet active;
	/**
	 * Unique identifier String passed over when generated from ast/Event class
	 */
	private String identifier;

	// Constructors

	/**
	 * Constructor: new Event with an unspecified number of states.
	 */
	public GSMPEvent(TypeDistribution distributionType, double firstParameter, double secondParameter, String identifier) {
		super();
		this.distributionType = distributionType;
		this.firstParameter = firstParameter;
		this.secondParameter = secondParameter;
		this.identifier = identifier;
		clearActive();
	}

	/**
	 * Constructor: new Event with {@code numstates} number of pre-allocated states.
	 */
	public GSMPEvent(int numStates, TypeDistribution distributionType, double firstParameter, double secondParameter, String identifier) {
		super(numStates);
		this.distributionType = distributionType;
		this.firstParameter = firstParameter;
		this.secondParameter = secondParameter;
		this.identifier = identifier;
		clearActive();
	}

	/**
	 * Copy constructor.
	 */
	public GSMPEvent(GSMPEvent event) {
		super(event);
		this.statesList = event.getStatesList();
		this.distributionType = event.distributionType;
		this.firstParameter = event.firstParameter;
		this.secondParameter = event.secondParameter;
		this.identifier = event.getIdentifier();
		clearActive();
		this.active.or(event.active);
	}

	/**
	 * Copy constructor.
	 */
	public GSMPEvent(GSMPEvent event, int permut[]) {
		super(event, permut);
		this.statesList = event.getStatesList();
		this.distributionType = event.distributionType;
		this.firstParameter = event.firstParameter;
		this.secondParameter = event.secondParameter;
		this.identifier =  event.getIdentifier();
		clearActive();
		int min = (numStates < permut.length ? numStates : permut.length);
		for (int i = 0; i < min; i++) {
			if (event.isActive(i))
				active.set(permut[i]);
		}
	}

	/**
	 * Add to the probability for a transition.
	 */
	public void addToProbability(int i, int j, double prob) {
		super.addToProbability(i, j, prob);
		setActive(i);
	}

	private void clearActive() {
		active = new BitSet(numStates);
	}

	public void setActive(int state) {
		active.set(state);
	}

	public void setPassive(int state){
		active.clear(state);
		clearState(state);
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public void setFirstParameter(double firstParam) {
		this.firstParameter = firstParam;
	}
	
	public void setSecondParameter(double secondParam) {
		this.secondParameter = secondParam;
	}
	
	public void setDistributionType(TypeDistribution distributionType) {
		this.distributionType = distributionType;
	}

	public boolean isActive(int state) {
		return active.get(state);
	}

	public BitSet getActive() {
		return active;
	}

	public String getIdentifier() {
		return identifier;
	}

	public double getFirstParameter() {
		return firstParameter;
	}
	
	public double getSecondParameter() {
		return secondParameter;
	}
	
	public TypeDistribution getDistributionType() {
		return distributionType;
	}
	
	/**
	 * Makes the probabilities of each row in the transition matrix sum to one.
	 */
	public void normalize() { // TODO MAJO - better precision
		final double tolerance = 0.0001;// TODO MAJO - make this dependent on some global prism setting
		for (int s = 0; s < getNumStates() ; ++s) {
			Distribution distribution = trans.get(s);
			if (distribution.isEmpty()) {
				continue;
			}
			double probabilitySum = 0.0;
			for (int i = 0; i < getNumStates() ; ++i) {
				if (distribution.contains(i)) {
					probabilitySum = probabilitySum + distribution.get(i);
				}
			}
			if (probabilitySum > (1.0 - tolerance) && probabilitySum < (1.0 + tolerance)) {
				// good enough, so this row does not need normalization. Better not do it than to further screw up the precision.
				continue;
			} else {
				// not good enough, so this row needs normalization
				for (int i = 0; i < getNumStates() ; ++i) {
					if (distribution.contains(i)) {
						distribution.set(i, distribution.get(i) / probabilitySum);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		String str = "Event[name=\"" + getIdentifier() + "\", " + distributionType.getTypeString();
		switch (distributionType.getNumParams()) {
		case 1:
			str += "(" + firstParameter + ") ";
			break;
		case 2:
			str += "(" + firstParameter + "," + secondParameter + ") ";
			break;
		default:
			str += "(Unusual number of parameters) ";
		}
		str += ", active=" + active + ", probabilities_" + super.toString() + ']';
		return str;
	}
}
