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
 * See parent class documentation for more basic info. {@link ACTMCPotato}
 * <br>
 * This extension implements high-precision precomputation
 * of exponentially-distributed potatoes using class BigDecimal.
 * <br>
 * HOW IT'S DONE:
 * This class is implemented as Erlang distribution with k=1.
 * See {@link ACTMCPotatoErlang} for details.
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
	public Distribution getMeanRewards() throws PrismException {
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