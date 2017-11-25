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

import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Simple explicit-state representation of a CTMC.
 */
public class GSMPSimple extends CTMCSimple implements GSMP // TODO MAJO - copied from FDPRISM, unfinished
{
	protected List<GSMPEvent> events;
	protected Map<Integer,Map<Integer,Map<Integer,String>>> transToSynchLabel;

	// Constructors

	/**
	 * Constructor: empty GSMP.
	 */
	public GSMPSimple() throws PrismException {
		super();
		initialise();
	}

	/**
	 * Constructor: new GSMP with fixed number of states.
	 */
	public GSMPSimple(int numStates) throws PrismException {
		super(numStates);
		initialise();
	}

	/**
	 * Copy constructor.
	 */
	public GSMPSimple(GSMPSimple gsmp) {
		super(gsmp);
		this.events = new ArrayList<GSMPEvent>(gsmp.getNumEvents());
		for (int i = 0; i < gsmp.getNumEvents(); ++i) {
			this.events.add(new GSMPEvent(gsmp.getEvent(i)));
		}
		this.transToSynchLabel = gsmp.transToSynchLabel; //TODO perform deep copy if needed
	}

	/**
	 * TODO: implement properly Construct a GSMP from an existing one and a
	 * state in permutation, i.e. in which state index i becomes index
	 * permut[i]. Note: have to build new Distributions from scratch anyway to
	 * do this, so may as well provide this functionality as a constructor.
	 */
	public GSMPSimple(GSMPSimple gsmp, int permut[]) 
	{
		super(gsmp, permut);
		this.events = new ArrayList<GSMPEvent>(gsmp.getNumEvents());
		for (int i = 0; i < gsmp.getNumEvents(); ++i) {
			this.events.add(new GSMPEvent(gsmp.getEvent(i), permut));
		}
		
		transToSynchLabel = new HashMap<Integer,Map<Integer,Map<Integer,String>>> (gsmp.getNumEvents()+1);
		if(gsmp.transToSynchLabel == null) return; 
		
		for(int i : gsmp.transToSynchLabel.keySet()) {
			Map<Integer,Map<Integer,String>> sources = new HashMap<Integer,Map<Integer,String>>();
			for(int src: gsmp.transToSynchLabel.get(i).keySet()) {
				Map<Integer,String> destinations = new HashMap<Integer,String>(gsmp.transToSynchLabel.get(i).get(src).size());
				for(Entry<Integer,String> dest: gsmp.transToSynchLabel.get(i).get(src).entrySet())
					destinations.put(permut[dest.getKey()],dest.getValue());
				sources.put(permut[src], destinations);
			}
			transToSynchLabel.put(i, sources);
		}
	}

	private void initialise() throws PrismException {
		this.events = new ArrayList<GSMPEvent>();
		transToSynchLabel = new HashMap<Integer,Map<Integer,Map<Integer,String>>> (getNumEvents()+1);
		transToSynchLabel.put(-1, new HashMap<Integer, Map<Integer,String>>());
		for (int i= 0; i <getNumEvents();++i) {
			transToSynchLabel.put(i, new HashMap<Integer, Map<Integer,String>>());
		}
	}

	@Override
	public int addState() {
		return super.addState();
	}

	@Override
	public void addStates(int numToAdd) {
		super.addStates(numToAdd);

		for (GSMPEvent event : events)
			event.addStates(numToAdd);
	}

	/**
	 * Add to the probability for a transition.
	 */
	public void addToProbability(int i, int j, double prob, double delay,
			int module, int fdIndex, String label) {
		if (fdIndex < 0)
			addToProbability(i, j, prob);
		else {
			GSMPEvent event = getEvent(module, fdIndex);
			if (event == null) {
				event = new GSMPEvent(label, numStates, delay, module, fdIndex);
				addEvent(event);
			}
			event.addToProbability(i, j, prob);
		}
	}

	// Accessors (for ModelSimple, overrides CTMCSimple)

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
		if (events == null) // TODO MAJO - wtf? why not make it an empty list instead?
			return 0;
		return events.size();
	}

	public void addEvent(GSMPEvent event) {
		events.add(event);
	}

	public GSMPEvent getEvent(int i) {
		return events.get(i);
	}

	public GSMPEvent getEvent(int module, int index) {
		for (GSMPEvent event : events) {
			if (event.getModule() == module && event.getIndex() == index)
				return event;
		}
		return null;
	}

	@Override
	public boolean isEventActive(GSMPEvent event, int state) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out) {
		super.exportToPrismExplicitTra(out);


		out.println(this);
		out.println("Events: " + getNumEvents());
	}



	@Override
	public String toString() {
		return "GSMPSimple [fdEvents=" + events + ", transToSynchLabel="
				+ transToSynchLabel + ", trans=" + trans + ", initialStates="
				+ initialStates + "]";
	}

	// TODO MAJO - this one is weird
	@Override
	public void addSynchLabel(int fixedD, int src, int dest, String label) throws PrismException {
		if(transToSynchLabel.get(fixedD) == null)
			transToSynchLabel.put(fixedD, new HashMap<Integer, Map<Integer,String>>());
		
		Map<Integer, String> map;
		if(transToSynchLabel.get(fixedD).get(src) == null) {
			map = new HashMap<Integer, String>(1);
			map.put(dest, label);
			transToSynchLabel.get(fixedD).put(src, map);
			return;
		}
		
		map = transToSynchLabel.get(fixedD).get(src);
		
		if(map.get(dest) == null){
			map.put(dest,label);
			return;
		}
		
		if(map.get(dest) != label)
			throw new PrismException("Multiple synchronization labels from state " + src + " to state " + dest + "!");

	}

	@Override
	public void clearSynchLabels() {
		transToSynchLabel.clear();
	}

	// TODO MAJO - this one is weird
	@Override
	public Map<Integer, String> getSychLabelsForState(int fixedD, int state) {
		if(transToSynchLabel.get(fixedD) == null)
			return null;
		return transToSynchLabel.get(fixedD).get(state);
	}
}
