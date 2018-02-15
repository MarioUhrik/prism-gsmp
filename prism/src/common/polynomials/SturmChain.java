//==============================================================================
//	
//	Copyright (c) 2017-
//	Authors:
//  Adrian Elgyutt <396222@mail.muni.cz> (Masaryk University)
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

package common.polynomials;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing Sturm chain
 * @author Adrian E.
 *
 */
public class SturmChain {
	private List<Polynomial> chain;
	private int divisionScale = 0;
	/**
	 * Constructor, creates the chain and stores it into member chain
	 * @param p0	polynomial to create chain from
	 * @param divisionScale	scale of division
	 */
	public SturmChain(Polynomial p0, int divisionScale){
		RoundingMode rounding1 = RoundingMode.DOWN;
		List<Polynomial> division = new ArrayList<>();
		chain =  new ArrayList<>();
		this.divisionScale = divisionScale;
		division.add(p0);
		division.add(p0.derivative());
		chain.add(p0);
		chain.add(p0.derivative());
					
		Polynomial zeroP = new Polynomial(BigDecimal.ZERO.setScale(divisionScale, rounding1).toString());
		while(division.get(1).compareTo(zeroP) != 0){
			List<Polynomial> divisionRes = Polynomial.longDivision(division.get(0), division.get(1), divisionScale);
			divisionRes.get(1).multiply(new Polynomial("-1"));
			division.remove(0);
			division.add(divisionRes.get(1));
			chain.add(divisionRes.get(1));
		}
	}
	/**
	 * GCD of p0 and p1 = p'
	 * @return gcd of p0 and p1
	 */
	public Polynomial gcd(){
		return chain.get(chain.size() - 2);
	}
	/**
	 * Get chain
	 * @return chain
	 */
	public List<Polynomial> getChain(){
		return chain;
	}
	/**
	 * Calculates the number of sign changes between start and end substituted in the chain
	 * @param start	start value of interval
	 * @param end	end value of interval
	 * @return		number of sign changes between sturmchain(start) and sturmchain(end)
	 */
	public int sigma(BigDecimal start, BigDecimal end){
		RoundingMode rounding1 = RoundingMode.DOWN;
		RoundingMode rounding2 = RoundingMode.DOWN;
		
		int startSignChanges = 0;
		int   endSignChanges = 0;
		
		int startPreviousSign = chain.get(0).value(start).setScale(divisionScale - 1, rounding1).compareTo(BigDecimal.ZERO);// -1 = -, 0 = 0, +1 = +
		int   endPreviousSign = chain.get(0).value(end).setScale(divisionScale - 1, rounding2).compareTo(BigDecimal.ZERO);

		int startSign = chain.get(1).value(start).setScale(divisionScale - 1, rounding1).compareTo(BigDecimal.ZERO);// -1 = -, 0 = 0, +1 = +
		int   endSign = chain.get(1).value(end).setScale(divisionScale - 1, rounding2).compareTo(BigDecimal.ZERO);

		if(startPreviousSign != startSign && startPreviousSign != 0 && startSign != 0) startSignChanges++;
		if(  endPreviousSign !=   endSign &&   endPreviousSign != 0 &&   endSign != 0)   endSignChanges++;
		
		
		for(int i = 2; i < chain.size(); i++){
			if(startSign != 0) startPreviousSign = startSign;
			if(endSign != 0) endPreviousSign = endSign;
			
			startSign = chain.get(i).value(start).setScale(divisionScale - 1, rounding1).compareTo(BigDecimal.ZERO);// -1 = -, 0 = 0, +1 = +
			endSign = chain.get(i).value(end).setScale(divisionScale - 1, rounding2).compareTo(BigDecimal.ZERO);
			
			if(startPreviousSign != startSign && startPreviousSign != 0 && startSign != 0) startSignChanges++;
			if(  endPreviousSign !=   endSign &&   endPreviousSign != 0 &&   endSign != 0)   endSignChanges++;
		
		}
		
		return startSignChanges - endSignChanges;
	}
}