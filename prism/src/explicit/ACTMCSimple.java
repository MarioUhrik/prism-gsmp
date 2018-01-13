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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prism.ModelType;

/**
 * Read-only explicit-state representation of an ACTMC suitable for CTMC-based model checking methods.
 * ACTMCs are constructed from viable GSMPSimple models during GSMP model-checking.
 * This is done to enable usage of more effective model-checking algorithms,
 * and to reduce the amount of events.
 * <br>
 * ACTMCs are GSMPs where at most one non-exponential event is active in any given state.
 * In addition, all exponential events of the previous GSMP are merged
 * into the CTMC transition matrix and then removed.
 */
public class ACTMCSimple extends CTMCSimple
{
	/** Mapping of non-exponential events onto states in which they are active. */
	protected Map<Integer, GSMPEvent> events = new HashMap<Integer, GSMPEvent>();

	/**
	 * Constructor from an already created GSMP that has been verified
	 * to have at most one non-exponential event active in any given state.
	 * Otherwise, the behavior of this method is undefined.
	 */
	public ACTMCSimple(GSMPSimple gsmp) {
		super(gsmp.generateCTMC());
		copyFrom(gsmp);
		this.events = new HashMap<Integer, GSMPEvent>(gsmp.getNumEvents());
		List<GSMPEvent> allEvents = gsmp.getEventList();
		for (int e = 0; e < allEvents.size(); ++e) {
			if (!allEvents.get(e).isExponential()) {
				GSMPEvent nonExpEvent = allEvents.get(e);
				BitSet activeStates = nonExpEvent.getActive();
				for (int s = activeStates.nextSetBit(0); s >= 0; s = activeStates.nextSetBit(s+1)) {
					this.events.put(s, nonExpEvent); // shallow copies
				}
			}
		}
	}
	
	@Override
	public ModelType getModelType() {
		return ModelType.GSMP;
	}

	/**
	 * Returns a mapping of non-exponential events within this ACTMC onto states in which they are active.
	 */
	public Map<Integer, GSMPEvent> getEventMap() {
		return events;
	}
	
	/**
	 * Returns a list of non-exponential events within this ACTMC.
	 */
	public List<GSMPEvent> getEventList() {
		return (new ArrayList<GSMPEvent>(events.values()));
	}

	/**
	 * @return The total number of non-exponential events within the ACTMC
	 */
	public int getNumEvents() {
		return events.size();
	}
	
	/**
	 * @param state index of a state
	 * @return The event active in the provided state, or null if none.
	 */
	public GSMPEvent getActiveEvent(int state) {
		return events.get(state);
	}
	
	@Override
	public String toString() {
 		String str =  "ACTMC with " + getNumEvents() + " events:";
 		List<GSMPEvent> events = getEventList();
		for (int i = 0; i < events.size(); i++) {
			str += "\n" + events.get(i);
		}
		str += "Underlying CTMC :\n" + super.toString();
 		return str;
	}
}
