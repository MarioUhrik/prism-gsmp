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
import parser.type.TypeDistributionExponential;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Simple explicit-state representation of a GSMP.
 * 
 * This implementation only consists of a backbone provided by ModelExplicit.java
 * and a collection of all events. The events are organized into a map for faster access.
 * See GSMPEvent.java for implementation details of the events.
 */
public class GSMPSimple extends ModelExplicit implements GSMP
{
	/**
	 * Mapping of events onto their unique identifiers.
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
		initialise(gsmp.getNumStates());
		copyFrom(gsmp);
		this.statesList = gsmp.getStatesList();
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
		initialise(gsmp.getNumStates());
		copyFrom(gsmp, permut);
		this.statesList = gsmp.getStatesList();
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
	 * Change the probabilities of event with name {@code eventIdent}
	 * @return true if successfully done, false if the event was not found
	 */
	public boolean addToProbability(int i, int j, double prob, String eventIdent, String actionLabel) {
		GSMPEvent event = getEvent(eventIdent);
		if (event == null) {
			// this should never happen
			return false;
		}
		event.addToProbability(i, j, prob);
		event.setActionLabel(i,j, actionLabel);
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

	public int getNumEvents() {
		return events.size();
	}

	public boolean addEvent(GSMPEvent event) {
		if (getEvent(event.getIdentifier()) != null) {
			return false;
		}
		GSMPEvent tmp = new GSMPEvent(
				getNumStates(),
				event.getDistributionType(),
				event.getFirstParameter(),
				event.getSecondParameter(),
				event.getIdentifier());
		events.put(event.getIdentifier(), tmp);
		return true;
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
	
	public void setEventMap(Map<String, GSMPEvent> eventsMap) {
		this.events = eventsMap;
	}
	
	@Override
	public List<GSMPEvent> getActiveEvents(int state){
		List<GSMPEvent> actEvents = new ArrayList<GSMPEvent>();
		List<GSMPEvent> allEvents = getEventList();
		for (int e = 0; e < allEvents.size() ; ++e) {
			if (allEvents.get(e).isActive(state)) {
				actEvents.add(allEvents.get(e));
			}
		}
		return actEvents;
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out) {
		//TODO MAJO - is this enough?
		out.println(this);
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	@Override
	public void clearState(int i) {
		// Do nothing if state i does not exist
		if (i >= numStates || i < 0)
			return;
		List<GSMPEvent> events = getEventList();
		for (int j = 0; j < events.size() ; ++j) {
			events.get(j).clearState(i);
		}
	}
	
	/**
	 *  Get an iterator over the successors of state s
	 */
	@Override
	public Iterator<Integer> getSuccessorsIterator(final int s)
	{
		List<GSMPEvent> events = getEventList();
		Set<Integer> successors = new HashSet<Integer>();
		for (int j = 0; j < events.size() ; ++j) {
			successors.addAll(events.get(j).trans.get(s).getSupport());
		}
		return successors.iterator();
	}

	@Override
	public SuccessorsIterator getSuccessors(int s) {
		return SuccessorsIterator.from(getSuccessorsIterator(s), true);
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException {
		List<GSMPEvent> events = getEventList();
		for (int s = 0; s < getNumStates(); s++) {
			boolean isDeadlock = true;
			for ( int e = 0; e < events.size() ; ++e) {
				if (!events.get(e).trans.get(s).isEmpty()) {
					isDeadlock = false;
					break;
				}
			}
			if (isDeadlock) {
				addDeadlockState(s);
			}
		}
		if (fix && getDeadlockStates().iterator().hasNext() ) {
			//fix all the deadlocks by creating a new exponential event looping over them
			double uniformizationRate = getUniformisationRate();
			String selfLoopEventIdent = 
					"<Deadlock_fixing_exp_event>\"=" +
					TypeDistributionExponential.getInstance().getTypeString() + 
					"(" + uniformizationRate + ")";
			GSMPEvent selfLoop = new GSMPEvent(getNumStates()
					,TypeDistributionExponential.getInstance(),
					uniformizationRate,
					0.0,
					selfLoopEventIdent);
			this.addEvent(selfLoop);
			for (Integer deadlockState : getDeadlockStates()) {
				selfLoop.addToProbability(deadlockState, deadlockState, 1.0);
			}
		}
	}
	
	/**
	 * @return The highest rate (first parameter) of all exponentially distributed events in this GSMP
	 *         If there are no exponentially distributed events, return 1.
	 */
	public double getUniformisationRate(){
		List<GSMPEvent> events = getEventList();
		double maxExitRate = Double.MIN_VALUE;
		boolean expEventVisited = false;
		for (int i = 0 ; i < events.size() ; ++i) {
			if (events.get(i).getDistributionType() instanceof TypeDistributionExponential) {
				expEventVisited = true;
				if (events.get(i).getFirstParameter() > maxExitRate) {
					maxExitRate = events.get(i).getFirstParameter();
				}
			}
		}
		if (expEventVisited) {
			return maxExitRate;
		} else {
			return 1.0;
		}
	}

	@Override
	public int getNumTransitions() {
		List<GSMPEvent> events = getEventList();
		int numTransitions = 0;
		for ( int i = 0 ; i < events.size() ; ++i) {
			numTransitions += events.get(i).getNumTransitions();
		}
		return numTransitions;
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException {
		List<GSMPEvent> events = getEventList();
		for (int s = 0; s < getNumStates(); s++) {
			for ( int e = 0; e < events.size() ; ++e) {
				if (events.get(e).trans.get(s).isEmpty() && (except == null || !except.get(s))) {
					throw new PrismException("GSMP has a deadlock in state " + s);
				}
			}
		}
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException {
		//TODO MAJO - implement
		throw new UnsupportedOperationException("Not yet implemented!");
	}
	
	/**
	 * Removes all events that have no probability in any state.
	 */
	public void removeEmptyEvents() {
		List<GSMPEvent> events = getEventList();
        Predicate<GSMPEvent> eventIsEmpty = e -> e.getActive().isEmpty();
        events.removeIf(eventIsEmpty);
        setEvents(events);
	}
	
	/**
	 * Makes the probabilities of each used row in the transition matrix of each event sum to one.
	 */
	public void normalize() {
		List<GSMPEvent> events = getEventList();
		for (int e = 0; e < events.size() ; ++e) {
			events.get(e).normalize();
		}
	} 
	
	@Override
	public void setStatesList(List<State> statesList){
		List<GSMPEvent> events = getEventList();
		for (int i = 0; i < events.size(); i++) {
			events.get(i).setStatesList(statesList);
		}
		this.statesList = statesList;
	}
	
	@Override
	public String toString() {
 		String str =  "GSMP with " + getNumEvents() + " events:";
 		List<GSMPEvent> events = getEventList();
		for (int i = 0; i < events.size(); i++) {
			str += "\n" + events.get(i);
		}
 		return str;
	}

}
