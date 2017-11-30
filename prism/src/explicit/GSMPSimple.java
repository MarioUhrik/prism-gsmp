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

import parser.State;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Simple explicit-state representation of a GSMP.
 */
public class GSMPSimple extends ModelExplicit implements GSMP
{
	
	protected List<GSMPEvent> events = new ArrayList<GSMPEvent>();
	
	/**
	 * Default constructor without a predefined number of states.
	 */
	public GSMPSimple() throws PrismException {
		super();
		initialise(0);
	}

	/**
	 * Default constructor with a predefined number of states.
	 */
	public GSMPSimple(int numStates) throws PrismException {
		super();
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public GSMPSimple(GSMPSimple gsmp) {
		super();
		copyFrom(gsmp);
		this.events = new ArrayList<GSMPEvent>(gsmp.getNumEvents());
		for (int i = 0; i < gsmp.getNumEvents(); ++i) {
			this.events.add(new GSMPEvent(gsmp.getEvent(i)));
		}
	}

	/**
	 * Permut copy constructor.
	 */
	public GSMPSimple(GSMPSimple gsmp, int permut[]) {
		super();
		copyFrom(gsmp, permut);
		this.events = new ArrayList<GSMPEvent>(gsmp.getNumEvents());
		for (int i = 0; i < gsmp.getNumEvents(); ++i) {
			this.events.add(new GSMPEvent(gsmp.getEvent(i), permut));
		}
	}

	public void initialise(int numStates){
		super.initialise(numStates);
		this.statesList = new ArrayList<State>();
		this.events = new ArrayList<GSMPEvent>();
	}

	@Override
	public int addState() {
		numStates += 1;
		//TODO MAJO - initialise the new state somehow!
		statesList.add(new State(0));

		for (int i = 0; i < events.size() ; ++i) {
			events.get(i).addState();
		}
		return numStates - 1;
	}

	@Override
	public void addStates(int numToAdd) {
		for (int i = 0 ; i < numToAdd ; ++i) {
			addState();
		}
	}

	/**
	 * Change the update probabilities of event under index {@code eventIndex}
	 * @return true if successfully done, false if event was not found
	 */
	public boolean addToProbability(int i, int j, double prob, int eventIndex) {
		GSMPEvent event = getEvent(eventIndex);
		if (event == null) {
			return false;
		}
		event.addToProbability(i, j, prob);
		return true;
	}

	@Override
	public ModelType getModelType() {
		return ModelType.GSMP;
	}

	@Override
	public List<GSMPEvent> getAllEvents() {
		return events;
	}
        
	public List<Integer> getAllEventIndices() {
		List<Integer> result = new ArrayList<>(events.size());
		for(int i = 0; i < events.size(); ++i) {
			result.add(i);
		}
		return result;
	}

	public int getNumEvents() {
		return events.size();
	}

	public void addEvent(GSMPEvent event) {
		events.add(event);
	}

	public GSMPEvent getEvent(int i) {
		return events.get(i);
	}

	@Override
	public boolean isEventActive(GSMPEvent event, int state) {
		//TODO MAJO
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out) {
		//TODO MAJO - is this enough?
		out.println(this);
	}

	@Override
	public String toString() {
 		return "GSMPSimple [events=" + events + ", initialStates="
				+ initialStates + ", states=" + statesList + "]";
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public void clearState(int i) {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public SuccessorsIterator getSuccessors(int s) {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public int getNumTransitions() {
		//TODO MAJO - implement, although possibly it may be enough to return events.size?
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}

}
