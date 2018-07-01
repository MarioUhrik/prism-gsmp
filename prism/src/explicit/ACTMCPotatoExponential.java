//==============================================================================
//	
//	Copyright (c) 2018-
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

package explicit;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import explicit.rewards.ACTMCRewardsSimple;
import prism.PrismException;

/**
 * Class for storage and computation of single exponentially distributed potato-related data for ACTMCs,
 * I.e. this implementation treats the event as exponentially distributed.
 * Exponential transitions are normally embedded into the underlying CTMC,
 * so this class is useful only when, for example, parameter synthesis for the exponential rate is done.
 * Even then, this class is merely a wrapper for ACTMCPotatoErlang with k=1.
 * <br>
 * Potato is a subset of states of an ACTMC in which a given event is active.
 * <br><br>
 * This data is fundamental for ACTMC model checking methods based on reduction
 * of ACTMC to DTMC. The reduction works by pre-computing the expected behavior
 * (rewards, spent time, resulting distribution...) occurring between
 * entering and leaving a potato. Then, these expected values are used in
 * regular CTMC/DTMC model checking methods.
 */
public class ACTMCPotatoExponential extends ACTMCPotato
{
	
	/**
	 * Internal Erlang distributed potato (with rate lambda and k=1).
	 * It is actually used to do all the computations.
	 * I.e. ACTMCPotatoExponential is just disguised ACTMCPotatoErlang with k=1.
	 */
	private ACTMCPotatoErlang erlang;
	
	/** {@link ACTMCPotato#ACTMCPotato(ACTMCSimple, GSMPEvent, ACTMCRewardsSimple, BitSet)} */
	public ACTMCPotatoExponential(ACTMCSimple actmc, GSMPEvent event, ACTMCRewardsSimple rewards, BitSet target) throws PrismException {
		super(actmc, event, rewards, target);
		this.erlang = new ACTMCPotatoErlang(this);
		erlang.event.setSecondParameter(1);
	}
	
	public ACTMCPotatoExponential(ACTMCPotato other) {
		super(other);
		this.erlang = new ACTMCPotatoErlang(this);
		erlang.event.setSecondParameter(1);
	}
	
	@Override
	public void setKappa(BigDecimal kappa) {
		erlang.setKappa(kappa);
	}
	
	@Override
	public Set<Integer> getPotato() {
		return erlang.getPotato();
	}
	
	@Override
	public Set<Integer> getEntrances() {
		return erlang.getEntrances();
	}
	
	@Override
	public Set<Integer> getSuccessors() {
		return erlang.getSuccessors();
	}
	
	@Override
	public DTMCSimple getPotatoDTMC() {
		return erlang.getPotatoDTMC();
	}
	
	@Override
	public BigDecimal getKappa() {
		return erlang.getKappa();
	}
	
	@Override
	public Map<Integer, Integer> getMapACTMCtoDTMC() {
		return erlang.getMapACTMCtoDTMC();
	}
	
	@Override
	public Vector<Integer> getMapDTMCtoACTMC() {
		return erlang.getMapDTMCtoACTMC();
	}
	
	@Override
	public Map<Integer, Distribution> getMeanTimes() throws PrismException {
		return erlang.getMeanTimes();
	}
	
	@Override
	public Map<Integer, Distribution> getMeanDistributions() throws PrismException {
		return erlang.getMeanDistributions();
	}
	
	@Override
	public Map<Integer, Double> getMeanRewards() throws PrismException {
		return erlang.getMeanRewards();
	}
	
	@Override
	protected void computeFoxGlynn() throws PrismException {
		erlang.computeFoxGlynn();
	}

	@Override
	protected void computeMeanTimes() throws PrismException {
		erlang.computeMeanTimes();
	}
	
	@Override
	protected void computeMeanDistributions() throws PrismException {
		erlang.computeMeanDistributions();
	}
	
	@Override
	protected void computeMeanRewards() throws PrismException {
		erlang.computeMeanRewards();
	}
	
	@Override
	public double[] applyEventRewards(double[] rewardsArray, boolean originalIndexing) throws PrismException {
		return erlang.applyEventRewards(rewardsArray, originalIndexing);
	}
}