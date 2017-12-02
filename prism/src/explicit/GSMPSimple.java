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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Simple explicit-state representation of a GSMP.
 */
public class GSMPSimple extends ModelExplicit implements GSMP
{
	/**
	 * Events are mapped onto their unique identifiers to provide faster access.
	 */
	protected Map<String, GSMPEvent> events = new HashMap<String, GSMPEvent>();
	
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
		this.events = new HashMap<String, GSMPEvent>(gsmp.getNumEvents());
		List<GSMPEvent> tmp = gsmp.getEventList();
		for (int i = 0; i < tmp.size(); ++i) {
			this.events.put(tmp.get(i).getIdentifier(), new GSMPEvent(tmp.get(i)));
		}
	}

	/**
	 * Permut copy constructor.
	 */
	public GSMPSimple(GSMPSimple gsmp, int permut[]) {
		super();
		copyFrom(gsmp, permut);
		this.events = new HashMap<String, GSMPEvent>(gsmp.getNumEvents());
		List<GSMPEvent> tmp = gsmp.getEventList();
		for (int i = 0; i < tmp.size(); ++i) {
			this.events.put(tmp.get(i).getIdentifier(), new GSMPEvent(tmp.get(i), permut));
		}
	}

	public void initialise(int numStates){
		super.initialise(numStates);
		this.statesList = new ArrayList<State>();
		this.events = new HashMap<String, GSMPEvent>();
	}

	@Override
	public int addState() {
		numStates += 1;
		//TODO MAJO - initialise the new state somehow!
		statesList.add(new State(0));

		List<GSMPEvent> tmp = getEventList();
		for (int i = 0; i < tmp.size() ; ++i) {
			tmp.get(i).addState();
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
	 * Change the probabilities of event under index {@code eventIndex}
	 * @return true if successfully done, false if event was not found
	 */
	public boolean addToProbability(int i, int j, double prob, String eventIdent) {
		GSMPEvent event = getEvent(eventIdent);
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

	public Map<String, GSMPEvent> getEventMap() {
		return events;
	}
	
	@Override
	public List<GSMPEvent> getEventList() {
		return (new ArrayList<GSMPEvent>(events.values()));
	}
        
	public List<Integer> getAllEventIndices() {
		List<Integer> result = new ArrayList<Integer>(getEventList().size());
		for(int i = 0; i < result.size(); ++i) {
			result.add(i);
		}
		return result;
	}

	public int getNumEvents() {
		return events.size();
	}

	public void addEvent(GSMPEvent event) {
		events.put(event.getIdentifier(), event);
	}

	public GSMPEvent getEvent(String identifier) {
		return events.get(identifier);
	}
	
	public void setEvents(List<GSMPEvent> events) {
		this.events = new HashMap<String, GSMPEvent>();
		for ( int i = 0; i < events.size() ; ++i) {
			this.events.put(events.get(i).getIdentifier(), events.get(i));
		}
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
