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

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import explicit.rewards.ACTMCRewardsSimple;
import explicit.rewards.MCRewards;
import explicit.rewards.StateRewardsConstant;
import explicit.rewards.StateRewardsSimple;
import prism.PrismComponent;
import prism.PrismException;

/**
 * Class for reduction of ACTMC to equivalent DTMC. (and also their reward structures)
 * <br>
 * This class fulfills similar purpose to class {@code ACTMCPotatoData},
 * but on the scope of the entire ACTMC, whereas the scope of {@code ACTMCPotatoData}
 * only encompasses a single event.
 */
public class ACTMCReduction extends PrismComponent // TODO MAJO - optimize!
{
	/** ACTMC model this class is associated with */
	private ACTMCSimple actmc;
	/** Optional reward structure associated with {@code actmc}.
	 *  The CTMC transition rewards are already expected to have been converted to state rewards.
	 *  May be null if rewards are not of interest for given model checking method.*/
	private ACTMCRewardsSimple actmcRew = null;
	/** Optional bitset of target states (for reachability) */
	private BitSet target = null;
	/** Map where the keys are string identifiers of the GSMPEvents,
	 *  and the values are corresponding ACTMCPotatoData structures.
	 *  This is useful for fast access and efficient reusage of the ACTMCPotatoData structures.*/
	private Map<String, ACTMCPotatoData> pdMap;
	/** DTMC equivalent to {@code actmc} eventually generated by this class.
	 *  Initially null.*/
	private DTMCSimple dtmc = null;
	private MCRewards dtmcRew = null;
	private boolean divideByUniformizationRate;
	
	/** Default first stage accuracy for computing kappa */
	private static final double epsilon = 1;
	
	/**
	 * The only constructor
	 * @param actmc Associated ACTMC model. Must not be null!
	 * @param actmcRew Optional reward structure associated with {@code actmc}. May be null.
	 * @param target Optional bitset of target states (if doing reachability). May be null.
	 * @param parent PrismComponent, presumably a model checker.
	 * Used only to pass to DTMCModelChecker when computing reachability rewards for kappa.
	 * @throws Exception if the arguments break the above rules
	 */
	public ACTMCReduction(ACTMCSimple actmc, ACTMCRewardsSimple actmcRew, BitSet target, PrismComponent parent) throws PrismException {
		super(parent);
		if (actmc == null) {
			throw new NullPointerException("ACTMCReduction constructor has received a null actmc!");
		}
		this.actmc = actmc;
		this.actmcRew = actmcRew;
		this.target = target;
		if (this.target == null) {
			this.target = new BitSet(actmc.getNumStates());
		}
		this.pdMap = createPotatoDataMap(this.actmc, this.actmcRew, this.target);
	}
	
	/**
	 * Get a DTMC fully equivalent to {@code actmc}.
	 * Computed DTMC is accurate up to error {@literal kappa} computed by this class.
	 */
	public DTMCSimple getDTMC() throws PrismException {
		if (dtmc == null) {
			computeEquivalentDTMC();
		}
		return dtmc;
	}
	
	/**
	 * Get a DTMC reward structure for {@code dtmc} fully equivalent to {@code actmc}.
	 * Computed values are accurate up to error {@literal kappa} computed by this class.
	 * @param divideByUniformizationRate is true if the computed rewards should be divided
	 * by the {@code dtmc} uniformization rate. Commonly, this should true when computing
	 * steady-state rewards, and false when computing reachability rewards.
	 */
	public MCRewards getDTMCRew(boolean divideByUniformizationRate) throws PrismException {
		if (dtmc == null) {
			computeEquivalentDTMC();
		}
		if (this.divideByUniformizationRate == divideByUniformizationRate) {
			if (dtmcRew != null) {
				return dtmcRew;
			}
		}
		this.divideByUniformizationRate = divideByUniformizationRate;
		computeEquivalentDTMCRew();
		return dtmcRew;
	}
	
	/**
	 * Get {@code ACTMCPotatoData} used to create equivalent DTMC.
	 * Computed values are accurate up to error {@literal kappa} computed by this class.
	 */
	public Map<String, ACTMCPotatoData> getPotatoData() throws PrismException {
		if (dtmc == null) {
			computeEquivalentDTMC();
		}
		return pdMap;
	}
	
	private void computeEquivalentDTMC() throws PrismException {
		computeKappa();
		dtmc = constructDTMC();
	}
	
	private void computeEquivalentDTMCRew() throws PrismException {
		if (dtmc == null) {
			computeEquivalentDTMC();
		}
		dtmcRew = constructDTMCRew(dtmc, divideByUniformizationRate);
	}
	
	/**
	 * Computes the kappa error bounds and assigns it to the {@code pdMap}.
	 */
	private Map<Integer, Double> computeKappa() throws PrismException {
		
		Map<Integer, Double> kappaOneMap = new HashMap<Integer, Double>();
		Map<Integer, Double> kappaTwoMap = new HashMap<Integer, Double>();
		Map<Integer, Double> maxStepsMap = new HashMap<Integer, Double>();
		Map<Integer, Double> maxTRMap = new HashMap<Integer, Double>();
		Map<Integer, Integer> nMap = new HashMap<Integer, Integer>();
		Set<Integer> allEntrances = new HashSet<Integer>();
		
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			ACTMCPotatoData pd = pdEntry.getValue();
			Set<Integer> entrances = pd.getEntrances();
			allEntrances.addAll(entrances);
			DTMCSimple potatoDTMC = pd.getPotatoDTMC();
			Map<Integer, Integer> ACTMCtoDTMC = pd.getMapACTMCtoDTMC();
			Vector<Integer> DTMCtoACTMC = pd.getMapDTMCtoACTMC();
			
			BitSet targetPermut = new BitSet(actmc.getNumStates());
			for (int i = target.nextSetBit(0); i >= 0; i = target.nextSetBit(i+1)) {
				targetPermut.set(ACTMCtoDTMC.get(i));
			}
			
			for (int entrance : entrances) {
				// TODO MAJO - shitty variable names - do something about it!
				BitSet reachableStatesPermut = potatoDTMC.getReachableStates(ACTMCtoDTMC.get(entrance), targetPermut);
				// TODO MAJO - Optimize by doing them per entire bitset!
				BitSet reachableStates = new BitSet(actmc.getNumStates());
				for (int i = reachableStatesPermut.nextSetBit(0); i >= 0; i = reachableStatesPermut.nextSetBit(i+1)) {
					reachableStates.set(DTMCtoACTMC.get(i));
				}
				
				double minProb = potatoDTMC.getMinimumProbability(reachableStatesPermut);
				double maxRew;
				if (actmcRew != null) {
					maxRew = actmcRew.getMax(reachableStates);
				} else {
					maxRew = 0;
				}
				double baseKappaOne = minProb / 2;
				double baseKappaTwo = Math.min(baseKappaOne, maxRew);
				int n = reachableStates.cardinality(); // amount of non-target states
				nMap.put(entrance, n);
				double maxExpectedSteps = n / Math.pow(baseKappaOne, n);
				double maxExpectedTR = maxExpectedSteps * maxRew;
				double b = 1 / (2 * maxExpectedSteps * n);
				double kappaOne; {
					double c = epsilon / ((2 * maxExpectedSteps) * (1 + maxExpectedSteps * n));
					kappaOne = Math.min(baseKappaOne, Math.min(b, c));
				}
				double kappaTwo; {
					double c = epsilon / ((2 * maxExpectedSteps) * (1 + maxExpectedTR * n));
					kappaTwo = Math.min(baseKappaTwo, Math.min(b, c));
				}
				kappaOneMap.put(entrance, kappaOne);
				kappaTwoMap.put(entrance, kappaTwo);
			}
			pd.setKappaMap(kappaOneMap);
		}

		DTMCSimple kappaOneDTMC = constructDTMC();
		MCRewards kappaOneDTMCRew = new StateRewardsConstant(1);
		DTMCModelChecker mc1 = new DTMCModelChecker(this);
		for (int entrance : allEntrances ) {
			boolean isEntranceTarget = target.get(entrance);
			target.set(entrance);
			ModelCheckerResult kappaOneTR = mc1.computeReachRewards(kappaOneDTMC, kappaOneDTMCRew, target);
			target.set(entrance, isEntranceTarget);
			
			double max = findMaxTR(kappaOneTR.soln);
			maxStepsMap.put(entrance, max + epsilon);
		}
		
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			pdEntry.getValue().setKappaMap(kappaTwoMap);
		}
		DTMCSimple kappaTwoDTMC = constructDTMC();
		MCRewards kappaTwoDTMCRew = constructDTMCRew(kappaTwoDTMC, false);
		DTMCModelChecker mc2 = new DTMCModelChecker(this);
		for (int entrance : allEntrances ) {
			boolean isEntranceTarget = target.get(entrance);
			target.set(entrance);
			ModelCheckerResult kappaTwoTR = mc2.computeReachRewards(kappaTwoDTMC, kappaTwoDTMCRew, target);
			target.set(entrance, isEntranceTarget);
			
			double max = findMaxTR(kappaTwoTR.soln);
			maxTRMap.put(entrance, max + epsilon);
		}
		
		Map<Integer, Double> kappaMap = new HashMap<Integer, Double>();
		for (Map.Entry<Integer, Double> entry : kappaOneMap.entrySet()) {
			int entrance = entry.getKey();
			double kappaOne = entry.getValue();
			double kappaTwo = kappaTwoMap.get(entrance);
			double maxSteps = maxStepsMap.get(entrance);
			double maxTR = maxTRMap.get(entrance);
			int n = nMap.get(entrance);
			double a = 1 / (2 * n * maxSteps);
			double b = epsilon / (2 * maxSteps * (1 + maxTR * n));
			
			double kappa = Math.min(kappaOne, Math.min(kappaTwo, Math.min(a, b)));
			kappaMap.put(entrance, kappa);
		}
		
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			pdEntry.getValue().setKappaMap(kappaMap);
		}
		
		return kappaMap;
	}
	
	/**
	 * Finds the maximum element of the array, but only considers indices
	 * that are either potato entrances or outside the potato.
	 */
	private double findMaxTR(double[] soln) {
		// find relevant states
		Set<Integer> potatoes = new HashSet<Integer>();
		Set<Integer> entrances = new HashSet<Integer>();
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			entrances.addAll(pdEntry.getValue().getEntrances());
			potatoes.addAll(pdEntry.getValue().getPotato());
		}
		Set<Integer> relevantStates = new HashSet<Integer>();
		for (int i = 0; i < actmc.getNumStates() ; ++i) {
			relevantStates.add(i);
		}
		relevantStates.removeAll(potatoes);
		relevantStates.addAll(entrances);
		
		// find maximum of the relevant states
		double max = Double.MIN_VALUE;
		for (int relevantState : relevantStates) {
			if (soln[relevantState] > max) {
				max = soln[relevantState];
			}
		}
		if (max == Double.MIN_VALUE) {
			max = 0.0; // TODO MAJO - can this happen? shouldnt it be an error?
		}
		
		return max;
	}
	
	/**
	 * Uses {@code actmc} and current {@code pdMap} to construct
	 * an equivalent {@code dtmc}.
	 * @return {@code dtmc} equivalent to {@code actmc} according to the current {@code pdMap}
	 */
	private DTMCSimple constructDTMC() throws PrismException {
		CTMCSimple ctmc = new CTMCSimple(actmc);
		double uniformizationRate = ctmc.getMaxExitRate();
		//ctmc.uniformise(uniformizationRate); // TODO MAJO - make 100% sure this can be deleted
		DTMCSimple dtmc = ctmc.buildUniformisedDTMC(uniformizationRate);
		
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			ACTMCPotatoData potatoData = pdEntry.getValue();
			Map<Integer, Distribution> meanDistrs = potatoData.getMeanDistributions();
			
			Set<Integer> potatoEntrances = potatoData.getEntrances();
			for (int entrance : potatoEntrances) {
				// assign the computed distribution the CTMC
				Distribution meanDistr = new Distribution(meanDistrs.get(entrance));
				Set<Integer> distrSupport = meanDistrs.get(entrance).getSupport();
				for ( int s : distrSupport) {
					meanDistr.set(s, meanDistr.get(s));
				}
				dtmc.trans.set(entrance, meanDistr);
			}
		}
		
		return dtmc;
	}
	
	/**
	 * Uses {@code actmc} and current {@code pdMap} to construct
	 * equivalent uniformized {@code dtmc}. The DTMC is uniformized according to how much
	 * time is spent within each potato having entered from a particular entrance.
	 * This tends to introduce a lot of unnecessary loops!
	 * @return {@code dtmc} equivalent to {@code actmc} according to the current {@code pdMap}
	 */
	@Deprecated
	private DTMCSimple constructUniformizedDTMC() throws PrismException {
		CTMCSimple ctmc = new CTMCSimple(actmc);
		double uniformizationRate = ctmc.getMaxExitRate();
		
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			ACTMCPotatoData potatoData = pdEntry.getValue();
			Map<Integer, Distribution> meanTimesWithinPotato = potatoData.getMeanTimes();
			Map<Integer, Distribution> meanDistrs = potatoData.getMeanDistributions();
			
			Set<Integer> potatoEntrances = potatoData.getEntrances();
			for (int entrance : potatoEntrances) {
				// compute the rate
				Distribution potatoTimeDistr = meanTimesWithinPotato.get(entrance);
				double theta = potatoTimeDistr.sum();
				double meanRateWithinPotato = 1 / theta;
				if ((meanRateWithinPotato) > uniformizationRate) {
					uniformizationRate = meanRateWithinPotato;
				}
				
				// weigh the distribution by the rate and assign it to the CTMC
				Distribution meanDistr = new Distribution(meanDistrs.get(entrance));
				Set<Integer> distrSupport = meanDistrs.get(entrance).getSupport();
				for ( int s : distrSupport) {
					meanDistr.set(s, meanDistr.get(s) * meanRateWithinPotato);
				}
				ctmc.trans.set(entrance, meanDistr);
			}
		}
		
		// Then, reduce the CTMC to a DTMC.
		ctmc.uniformise(uniformizationRate); // TODO MAJO - this doesnt need to be here
		DTMCSimple dtmc = ctmc.buildUniformisedDTMC(uniformizationRate);
		return dtmc;
	}
	
	/**
	 * Uses {@code actmc}, {@code actmcRew} and current {@code pdMap} to construct
	 * equivalent {@code mcRewards} for {@code dtmc}.
	 * <br>
	 * Iff {@code divideByUniformizationRate} is true,
	 * rewards are also adjusted to {@code dtmc.uniformizationRate}.
	 * @return {@code MCRewards} equivalent to actmcRew
	 */
	private MCRewards constructDTMCRew(DTMCSimple dtmc, boolean divideByUniformizationRate) throws PrismException {
		StateRewardsSimple newRew = new StateRewardsSimple();
		if (actmcRew == null) {
			return newRew;
		}
		
		double uniformizationRate = dtmc.uniformizationRate;
		if (!divideByUniformizationRate) {
			dtmc.uniformizationRate = 1;
		}
		
		int numStates = dtmc.getNumStates();
		for (int s = 0; s < numStates ; ++s) {
			double rew = actmcRew.getStateReward(s);
			if (rew > 0) {
				newRew.setStateReward(s, rew / dtmc.uniformizationRate);
			}
		}
		
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			ACTMCPotatoData potatoData = pdEntry.getValue();
			Set<Integer> entrances = potatoData.getEntrances();
			for (int entrance : entrances) {
				double rew = potatoData.getMeanRewards().get(entrance);
				newRew.setStateReward(entrance, rew / dtmc.uniformizationRate);
			}
		}
		
		dtmc.uniformizationRate = uniformizationRate;
		return newRew;
	}
	
	/**
	 * Uses {@code actmc}, {@code actmcRew} and current {@code pdMap} to construct
	 * equivalent {@code mcRewards} for uniformized {@code dtmc} (created by {@code constructUniformizedDTMC()}.
	 * <br>
	 * Iff {@code divideByUniformizationRate} is true,
	 * rewards are also adjusted to {@code dtmc.uniformizationRate}.
	 * @return {@code MCRewards} equivalent to actmcRew
	 */
	@Deprecated
	private MCRewards constructUniformizedDTMCRew(DTMCSimple dtmc, boolean divideByUniformizationRate) throws PrismException {
		StateRewardsSimple newRew = new StateRewardsSimple();
		if (actmcRew == null) {
			return newRew;
		}
		
		double uniformizationRate = dtmc.uniformizationRate;
		if (!divideByUniformizationRate) {
			dtmc.uniformizationRate = 1;
		}
		
		Map<Integer, Double> meanRewWithinPotatoesOverTime = new HashMap<Integer, Double>();
		for (Map.Entry<String, ACTMCPotatoData> pdEntry : pdMap.entrySet()) {
			ACTMCPotatoData potatoData = pdEntry.getValue();
			Set<Integer> entrances = potatoData.getEntrances();
			for (int entrance : entrances) {
				double rew = potatoData.getMeanRewards().get(entrance);
				double theta = potatoData.getMeanTimes().get(entrance).sum();
				double meanRew = rew / theta;//average reward over average time spent within
				meanRewWithinPotatoesOverTime.put(entrance, meanRew);
			}
		}
		
		int numStates = dtmc.getNumStates();
		for (int s = 0; s < numStates ; ++s) {
			double rew = actmcRew.getStateReward(s);
			if (rew > 0) {
				newRew.setStateReward(s, rew / dtmc.uniformizationRate);
			}
		}
		
		Set<Integer> entrances = meanRewWithinPotatoesOverTime.keySet();
		for (int entrance : entrances) {
			newRew.setStateReward(entrance, meanRewWithinPotatoesOverTime.get(entrance) / dtmc.uniformizationRate);
		}
		
		dtmc.uniformizationRate = uniformizationRate;
		return newRew;
	}

	/**
	 * Creates a map where the keys are string identifiers of the GSMPEvents,
	 * and the values are corresponding ACTMCPotatoData structures.
	 * This is useful as to enable reusage of the ACTMCPotatoData structures efficiently.
	 * @param actmc ACTMC model for which to create the ACTMCPotatoData structures
	 * @param rew Optional rewards associated with {@code actmc}. May be null, but calls
	 *            to {@code ACTMCPotatoData.getMeanReward()} will throw an exception!
	 */
	private Map<String, ACTMCPotatoData> createPotatoDataMap(ACTMCSimple actmc,
			ACTMCRewardsSimple rew, BitSet target) throws PrismException {
		Map<String, ACTMCPotatoData> pdMap = new HashMap<String, ACTMCPotatoData>();
		List<GSMPEvent> events = actmc.getEventList();
		
		for (GSMPEvent event: events) {
			ACTMCPotatoData potatoData = new ACTMCPotatoData(actmc,
					event,
					rew,
					target);
			pdMap.put(event.getIdentifier(), potatoData);
		}
		return pdMap;
	}
	
}