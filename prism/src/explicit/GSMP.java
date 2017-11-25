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

import java.util.List;
import java.util.Map;

import prism.PrismException;

/**
 * Interface for classes that provide (read) access to an explicit-state GSMP.
 */
public interface GSMP extends CTMC // TODO MAJO - incomplete
{
	/**
	 * Get list of all events.
	 */
	public List<GSMPEvent> getAllEvents();


	/**
	 * Check if event {@code fdEvent} is active in state {@code state}.
	 */
	public boolean isEventActive(GSMPEvent fdEvent, int state);
	
	//TODO MAJO - the following was taken from FDPRISM and does not make sense yet
	
	// TODO MAJO - this one is weird
	/**
	 * Adds information about sychronization labels in case
	 * there is need to pair with transition rewards.
	 * @param fixedD index of fixed-delay transition,
	 * @param src source state of transition,
	 * @param dest destination state of transition,
	 * @param label synchronization label.
	 */
	public void addSynchLabel(int fixedD, int src, int dest, String label) throws PrismException;

	/**
	 * Clears information about sychronization labels.
	 * Use after reward structures are built.
	 */	
	public void clearSynchLabels();
	
	// TODO MAJO - this one is weird
	/**
	 * Returns map of synch labels of transitions from {@code state} and FD {@code fixedD}.
	 */	
	public Map<Integer, String> getSychLabelsForState(int fixedD, int state);

}
