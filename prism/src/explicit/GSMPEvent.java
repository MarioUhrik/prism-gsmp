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
import prism.PrismException;

/**
 * Explicit engine class representing a GSMP event.
 */
public class GSMPEvent extends DTMCSimple 
{
	// TODO MAJO - the following was taken from fdPRISM and does not make sense yet
	private String label;
	private double delay;
	private double weight;
	private BitSet active;
	private int module;
	private int index;

	// Constructors

	/**
	 * Constructor: empty FDEvent.
	 */
	public GSMPEvent() {
		super();
		label = "";
		delay = 0;
		module = 0;
		index = 0;
		weight = 1;
		clearActive();
	}

	/**
	 * Constructor: new Event with fixed number of states.
	 */
	public GSMPEvent(int numStates) {
		super(numStates);
		label = "";
		delay = 0;
		module = 0;
		index = 0;
		weight = 1;
		clearActive();
	}

	public GSMPEvent(String label, int numStates, double delay, int module, int index) {
		super(numStates);
		this.label = label;
		this.delay = delay;
		this.module = module;
		this.index = index;
		weight = 1;
		clearActive();
	}

	/**
	 * Copy constructor.
	 */
	public GSMPEvent(GSMPEvent fdEvent) {
		super(fdEvent);
		this.label = fdEvent.label;
		this.delay = fdEvent.delay;
		this.weight = fdEvent.weight;
		this.module = fdEvent.module;
		this.index = fdEvent.index;
		clearActive();
		this.active.or(fdEvent.active);
	}

	/**
	 * Copy constructor.
	 */
	public GSMPEvent(GSMPEvent fdEvent, int permut[]) {
		super(fdEvent, permut);
		this.label = fdEvent.label;
		this.delay = fdEvent.delay;
		this.weight = fdEvent.weight;
		this.module = fdEvent.module;
		this.index = fdEvent.index;
		clearActive();
		int min = (numStates < permut.length ? numStates : permut.length);
		for (int i = 0; i < min; i++) {
			if (fdEvent.isActive(i))
				active.set(permut[i]);
		}
		// this.active.or(fdEvent.active);
	}

	public int getNumberOfSteps(double interval) throws PrismException {
		System.out.println("Delay: " + delay + " interval: " + interval
				+ " res: "
				+ (new Double(Math.floor(delay / interval))).intValue());
        if(delay < interval)
        	throw new PrismException("Delay(" + delay + " is smaller than discretization step(" + interval + " ).");
		return (int) Math.round(delay / interval);
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
	
	public void setWeight(double weight) {
		this.weight = weight;
	}

	public void setDelay(double delay) {
		this.delay = delay;
	}

	public boolean isActive(int state) {
		return active.get(state);
	}

	public String getLabel() {
		return label;
	}
	
	public BitSet getActive() {
		return active;
	}

	public double getWeight() {
		return weight;
	}

	public int getModule() {
		return module;
	}

	public int getIndex() {
		return index;
	}

	public double getDelayTime() {
		return delay;
	}

	@Override
	public String toString() {
		return "Event{" + "label=" + label + ", delay=" + delay + ", weight=" + weight
				+ ", active=" + active + ", module=" + module + ", index="
				+ index + ", super=" + super.toString() + '}';
	}
}
