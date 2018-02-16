//==============================================================================
//	
//	Copyright (c) 2017-
//	Authors:
//  * Adrian Elgyutt <396222@mail.muni.cz> (Masaryk University)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import prism.Pair;
/**
 * Class containing static methods for approximating and finding polynomial roots
 * @author Adrian E.
 *
 */
public class PolynomialRootFinding {
	/**
	 * Default constructor
	 */
	public PolynomialRootFinding(){
	}
	
	public static final BigDecimal TWO = new BigDecimal("2");
	public static final BigDecimal THREE = new BigDecimal("3");
	
	/**
	 * Calculates number of roots in interval using sturm method
	 * @param p0		polynomial to find roots
	 * @param start		start of interval (less than {@code end})
	 * @param end		end of interval
	 * @param divisionScale		division scale
	 * @return			number of roots in (start,end)
	 */
	public static int sturm(Polynomial p0, BigDecimal start, BigDecimal end, int divisionScale){
		RoundingMode rounding1 = RoundingMode.DOWN;
		RoundingMode rounding2 = RoundingMode.DOWN;
		int startSignChanges = 0;
		int   endSignChanges = 0;
		List<Polynomial> division = new ArrayList<>();
		
		division.add(p0);
		division.add(p0.derivative());
		
		int startPreviousSign = division.get(0).value(start).setScale(divisionScale - 1, rounding1).compareTo(BigDecimal.ZERO);// -1 = -, 0 = 0, +1 = +
		int   endPreviousSign = division.get(0).value(end).setScale(divisionScale - 1, rounding2).compareTo(BigDecimal.ZERO);
		int startSign = division.get(1).value(start).setScale(divisionScale - 1, rounding1).compareTo(BigDecimal.ZERO);// -1 = -, 0 = 0, +1 = +
		int   endSign = division.get(1).value(end).setScale(divisionScale - 1, rounding2).compareTo(BigDecimal.ZERO);
		if(startPreviousSign != startSign && startPreviousSign != 0 && startSign != 0) startSignChanges++;
		if(  endPreviousSign !=   endSign &&   endPreviousSign != 0 &&   endSign != 0)   endSignChanges++;
		
		Polynomial zeroP = new Polynomial(BigDecimal.ZERO.setScale(divisionScale, rounding1).toString());
		while(division.get(1).compareTo(zeroP) != 0){
			List<Polynomial> divisionRes = Polynomial.longDivision(division.get(0), division.get(1), divisionScale);
			divisionRes.get(1).multiply(new Polynomial("-1"));
			division.remove(0);
			division.add(divisionRes.get(1));
			if(startSign != 0) startPreviousSign = startSign;
			if(endSign != 0) endPreviousSign = endSign;
			startSign = division.get(1).value(start).setScale(divisionScale - 1, rounding1).compareTo(BigDecimal.ZERO);// -1 = -, 0 = 0, +1 = +
			endSign = division.get(1).value(end).setScale(divisionScale - 1, rounding2).compareTo(BigDecimal.ZERO);

			if(startPreviousSign != startSign && startPreviousSign != 0 && startSign != 0) startSignChanges++;
			if(  endPreviousSign !=   endSign &&   endPreviousSign != 0 &&   endSign != 0)   endSignChanges++;
		}
		return startSignChanges - endSignChanges;
	}

	/**
	 * Method that isolates roots using sturms theorem and bisection and then approximates them using newtons method
	 * @param p					polynomial to find roots of
	 * @param beginInterval		start of interval
	 * @param endInterval		end of interval
	 * @param decimalPrecision	number of required correct decimal digits
	 * @param sc				Sturm chain of p
	 * @return					list of roots of p
	 */
	public static List<BigDecimal> rootsOfPolynomialInIntervalUsingSturmTheoremAndBisectionAndNewtonMethod(Polynomial p, BigDecimal beginInterval, BigDecimal endInterval, int decimalPrecision, SturmChain sc){
		beginInterval = beginInterval.setScale(decimalPrecision, RoundingMode.HALF_UP);
		endInterval = endInterval.setScale(decimalPrecision, RoundingMode.HALF_UP);
		List<BigDecimal> roots = new ArrayList<>();
		int rootsCount = sc.sigma(beginInterval, endInterval);
		BigDecimal originBegin = beginInterval;
		BigDecimal originEnd = endInterval;
		if(rootsCount > 0){
			if(rootsCount == 1){
					Pair<BigDecimal, Boolean> root = PolynomialRootApproximation.findRootNewtonInInterval(p, BigDecimal.ONE.divide(new BigDecimal("10").pow(decimalPrecision)), beginInterval, endInterval, true);
					if(root.getValue()){
						roots.add(root.getKey());
						return roots;
					}
			}
			beginInterval = originBegin;
			endInterval = originEnd;
			BigDecimal intervalHalf = ((endInterval.subtract(beginInterval)).divide(new BigDecimal("2"), decimalPrecision, RoundingMode.HALF_UP)).add(beginInterval); 
			roots.addAll(rootsOfPolynomialInIntervalUsingSturmTheoremAndBisectionAndNewtonMethod(p, beginInterval, intervalHalf, decimalPrecision, sc));
			roots.addAll(rootsOfPolynomialInIntervalUsingSturmTheoremAndBisectionAndNewtonMethod(p, intervalHalf, endInterval, decimalPrecision, sc));
		}
		return roots;
	}
	/**
	 * Finds roots of polynomial p using ruffini horner method
	 * @param p				polynomial
	 * @param start_inter	start of interval
	 * @param end_inter		end of interval
	 * @param epsilon		allowed error of precision
	 * @return				roots of polynomial
	 */
	public static List<BigDecimal> findRootsNewtonHorner(Polynomial p, BigDecimal start_inter, BigDecimal end_inter, /*BigDecimal tolerance,*/ BigDecimal epsilon){
		if(p.degree() == 1){
			BigDecimal roo = new BigDecimal("0");
			if(p.coeffs.get(0).compareTo(new BigDecimal("0")) == 0){
				return new ArrayList<BigDecimal>(Arrays.asList(roo));
			}
			roo = new BigDecimal("-1")
					.multiply( (p.coeffs.get(0)).divide(p.coeffs.get(1), epsilon.scale()+2, RoundingMode.HALF_UP));
			return new ArrayList<BigDecimal>(Arrays.asList(roo));
		}
		
		List<BigDecimal> roots = new ArrayList<>();
		
			Pair<BigDecimal,Boolean> newtonRes = PolynomialRootApproximation.findRootNewtonInInterval(p, epsilon.setScale(epsilon.scale()+1), start_inter, end_inter, false);
			if (newtonRes.getValue()){
				   roots.add(newtonRes.getKey());//x1
				   List<BigDecimal> revCoef = new ArrayList<>(p.coeffs);
				   Collections.reverse(revCoef);
				   BigDecimal[] coef = (revCoef).toArray(new BigDecimal[(revCoef.size())]);
				   int n = coef.length;
				   BigDecimal divis = newtonRes.getKey();//x1
				   BigDecimal[] resCoef = new BigDecimal[n];
				   resCoef[0] = coef[0];
				   int maxScale = 0;
				   for(int j = 0; j < n; j++){
					   maxScale = (coef[j].scale() > maxScale ? coef[j].scale(): maxScale);
				   }
				   for(int j = 1; j < n; j++){
					   resCoef[j] = divis.multiply(resCoef[j - 1]).add(coef[j]).setScale(maxScale, RoundingMode.HALF_UP);
				   }
				   if(n == 2){
					   if(resCoef[0].compareTo(new BigDecimal("1")) == 0 &&
							   resCoef[1].abs().compareTo(epsilon) <= 0){
						   System.out.println("success");
						   return roots;
					   }
				   }

				   if(resCoef[n - 1].abs().compareTo(epsilon.multiply(new BigDecimal("1"))) <= 0){
					   StringBuilder divPoly = new StringBuilder();
					   if(resCoef.length > 0) {
						   divPoly.append(resCoef[0]).append("x^").append(n - 2);
					   }
					   for(int j = 1; j < n - 2; j++){
							if(resCoef[j].compareTo(new BigDecimal("0")) != 0){
								if(resCoef[j].compareTo(new BigDecimal("0")) > 0){
									divPoly.append("+");
								}
								divPoly.append(resCoef[j]).append("x^").append(n - 2 - j);
							}
					   }
					   if(resCoef.length > 0) {
						   if(resCoef[n - 2].compareTo(new BigDecimal("0")) != 0){
								if(resCoef[n - 2].compareTo(new BigDecimal("0")) > 0){
									divPoly.append("+");
								}
								divPoly.append(resCoef[n - 2]);
							}
					   }

					   roots.addAll(findRootsNewtonHorner(new Polynomial(divPoly.toString()), start_inter, end_inter, epsilon));

				   }
				   else {
					   System.out.println("remainder is not 0: "+ resCoef[n - 1]);
					   return roots;
				   }
			}
		Collections.sort(roots);
		List<BigDecimal> returnList = new ArrayList<>();
		
		if(roots.size() > 0) returnList.add(roots.get(0).setScale(epsilon.scale(), RoundingMode.HALF_UP));
		for(int i = 1; i < roots.size(); i++){
			BigDecimal prevRoot = roots.get(i-1);
			BigDecimal curRoot = roots.get(i);
			if((prevRoot.subtract(curRoot)).abs().compareTo(epsilon) > 0)
				returnList.add(curRoot.setScale(epsilon.scale(), RoundingMode.HALF_UP));
		}
		return returnList;
	}
	
	/**
	 * Computes the upper bounds on number of roots within (0,1) of p
	 * @param p	polynomial
	 * @return	0 if 0 roots, 1 if 1 root, 2 if more roots or 0 roots
	 */
	private static int budan01(Polynomial p){
		int v = 0;
		int prevSign = p.coeffs.get(0).compareTo(BigDecimal.ZERO);//-1 negative, 0 zero, 1 positive
		int sign = 0;
		
		Polynomial p1 = new Polynomial(BigDecimal.ZERO.toString());
		
		for(int i = 0; i < p.degree(); i++){
			p1.add(new Polynomial(p.coeffs.get(i)));
			p1.multiply(new Polynomial("x+1"));
		}
		p1.add(new Polynomial(p.coeffs.get(p.degree())));
		
		prevSign = p1.coeffs.get(0).compareTo(BigDecimal.ZERO);//-1 negative, 0 zero, 1 positive
		sign = 0;
		for(int i = 1; i <= p1.degree(); i++){
			sign = p1.coeffs.get(i).compareTo(BigDecimal.ZERO);
			if(prevSign != sign && sign !=0 && prevSign != 0){
				v++;
			}
			if(sign != 0) prevSign = sign;
		}
		return v;
	}
	
	/**
	 * Isolates the intervals of p using VCA algorithm, every interval contains 1 root
	 * @param p		polynomial to isolate roots of
	 * @param ab	interval to isolate roots of, where a=0, b=upper bound
	 * @param dScale	division scale, scale of interval
	 * @return		list of intervals containing 1 root
	 */
	private static List<Pair<BigDecimal, BigDecimal>> vca(Polynomial p, Pair<BigDecimal, BigDecimal> ab, int dScale){
		List<Pair<BigDecimal, BigDecimal>> intervals = new ArrayList<>();
		int var = budan01(p);
		if(var == 0) return intervals;
		if(var == 1){
			intervals.add(ab);
			return intervals;
		}
		if((ab.getKey().subtract(ab.getValue())).abs().compareTo(BigDecimal.ONE.divide(new BigDecimal("10").pow(dScale))) <= 0){//infinite loop prevention
			intervals.add(ab);//careful, could be more roots, multiple root or no root, could be false positive
			return intervals;//need to check with newton
		}

		Polynomial p012 = new Polynomial(p.coeffs);
		for(int i = 0; i <= p012.degree(); i++){
			p012.coeffs.set(i, p012.coeffs.get(i).multiply(TWO.pow(p012.degree() - i)));
		}
		BigDecimal m = (ab.getKey().add(ab.getValue())).divide(TWO, dScale + 1, RoundingMode.HALF_UP);

		Polynomial p121 = new Polynomial(BigDecimal.ZERO);

		for(int i = p.degree(); i > 0; i--){
			p121.add(new Polynomial(p.coeffs.get(i).multiply(TWO.pow(p.degree() - i))));
			p121.multiply(new Polynomial("x+1"));
		}
		p121.add(new Polynomial(p.coeffs.get(0).multiply(TWO.pow(p.degree()))));
		
		if(p.value(BigDecimal.ONE.divide(TWO, dScale + 1, RoundingMode.HALF_UP)).setScale(dScale, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) != 0){
			intervals.addAll(vca(p012, new Pair<>(ab.getKey(), m), dScale));
			intervals.addAll(vca(p121, new Pair<>(m, ab.getValue()), dScale));
			return intervals;
		} else {
			intervals.addAll(vca(p012, new Pair<>(ab.getKey(), m), dScale));
			intervals.add(new Pair<>(m, m));
			intervals.addAll(vca(p121, new Pair<>(m, ab.getValue()), dScale));
			return intervals;
		}
	}
	
	// a < b
	/**
	 * Isolates the intervals of p using VCA algorithm, every interval contains 1 root
	 * @param porig	polynomial to isolate roots of
	 * @param a		start of interval, a < b
	 * @param b		end of interval, a < b
	 * @param dScale	division scale
	 * @return		list intervals containing 1 root
	 */
	private static List<Pair<BigDecimal, BigDecimal>> VCAinterval(Polynomial porig, BigDecimal a, BigDecimal b, int dScale){
		Polynomial sub = new Polynomial("x+"+a.toString());
		Polynomial p = porig.substituteWithPolynomial(sub);
		BigDecimal newB = b.subtract(a);
		for(int i = 1; i <= p.degree(); i++){
			if(p.coeffs.get(i).compareTo(BigDecimal.ZERO) != 0){
				p.coeffs.set(i, p.coeffs.get(i).multiply(newB.pow(i)));
			}
		}
		List<Pair<BigDecimal, BigDecimal>> result = vca(p, new Pair<>(BigDecimal.ZERO, newB), dScale);
		for(int i = 0; i < result.size(); i++){
			result.set(i, new Pair<>(result.get(i).getKey().add(a), result.get(i).getValue().add(a)));
		}
		return result;
	}
	/**
	 * Computes the roots of p within interval (beginInterval, endInterval) using VCA and Newton
	 * @param p					polynomial to find roots of
	 * @param beginInterval		start of interval
	 * @param endInterval		end of interval
	 * @param decimalPrecision	allowed error of precision 
	 * @return					roots of p within (begin, end)
	 */
	public static List<BigDecimal> findRootsInIntervalVCANewton(Polynomial p, BigDecimal beginInterval, BigDecimal endInterval, BigDecimal decimalPrecision){
		beginInterval = beginInterval.setScale(decimalPrecision.scale(), RoundingMode.HALF_UP);
		endInterval = endInterval.setScale(decimalPrecision.scale(), RoundingMode.HALF_UP);
		
		List<BigDecimal> roots = new ArrayList<>();
		
		List<Pair<BigDecimal, BigDecimal>> rootsIntervals = VCAinterval(p, beginInterval, endInterval, decimalPrecision.scale());
		
		for(Pair<BigDecimal, BigDecimal> interval : rootsIntervals){
			BigDecimal rootBeginInterval = interval.getKey();
			BigDecimal rootEndInterval = interval.getValue();
			Pair<BigDecimal, Boolean> root = PolynomialRootApproximation.findRootNewtonInInterval(p, decimalPrecision, rootBeginInterval, rootEndInterval, true);
			if(rootBeginInterval.compareTo(rootEndInterval) == 0){
				root = PolynomialRootApproximation.findRootNewtonInInterval(p, decimalPrecision, rootBeginInterval.subtract(decimalPrecision), rootEndInterval.add(decimalPrecision.multiply(TWO)), true);
			}
			if(!root.getValue() && rootBeginInterval.compareTo(rootEndInterval) == 0) continue;//infinite loop prevention
			while(!root.getValue()){
				BigDecimal intervalHalf = ((rootEndInterval.subtract(rootBeginInterval)).divide(new BigDecimal("2"), decimalPrecision.scale(), RoundingMode.HALF_UP)).add(rootBeginInterval);
				int signBegin = p.value(rootBeginInterval).signum();
				int signHalf = p.value(intervalHalf).signum();
				if(signBegin == signHalf){
					rootBeginInterval = intervalHalf;
				}
				else{
					rootEndInterval = intervalHalf;
				}
				root = PolynomialRootApproximation.findRootNewtonInInterval(p, decimalPrecision, rootBeginInterval, rootEndInterval, true);
			}
			roots.add(root.getKey());
		}
		return roots;
	}
	
	/**
	 * Computes the roots of p within interval (beginInterval, endInterval) using VCA and Halley
	 * @param p					polynomial to find roots of
	 * @param beginInterval		start of interval
	 * @param endInterval		end of interval
	 * @param decimalPrecision	allowed error of precision 
	 * @return					roots of p within (begin, end)
	 */
	public static List<BigDecimal> findRootsInIntervalVCAHalley(Polynomial p, BigDecimal beginInterval, BigDecimal endInterval, BigDecimal decimalPrecision){
		beginInterval = beginInterval.setScale(decimalPrecision.scale(), RoundingMode.HALF_UP);
		endInterval = endInterval.setScale(decimalPrecision.scale(), RoundingMode.HALF_UP);
		List<BigDecimal> roots = new ArrayList<>();
		int maxScale = decimalPrecision.scale() + 1;

		List<Pair<BigDecimal, BigDecimal>> rootsIntervals = VCAinterval(p, beginInterval, endInterval, maxScale);
		for(Pair<BigDecimal, BigDecimal> interval : rootsIntervals){
			BigDecimal rootBeginInterval = interval.getKey();
			BigDecimal rootEndInterval = interval.getValue();
			if(rootBeginInterval.compareTo(rootEndInterval)==0){
				roots.add(rootBeginInterval);
				continue;
			}
			Pair<BigDecimal, Boolean> root = PolynomialRootApproximation.findRootHalleyInInterval(p, decimalPrecision, rootBeginInterval, rootEndInterval, true);
			if(!root.getValue() && rootBeginInterval.subtract(rootEndInterval).abs().compareTo(decimalPrecision) == 0) continue;//ochrana proti zacykleniu pri nasobnom koreni
			while(!root.getValue()){
				BigDecimal intervalHalf = ((rootEndInterval.subtract(rootBeginInterval)).divide(new BigDecimal("2"), decimalPrecision.scale(), RoundingMode.HALF_UP)).add(rootBeginInterval);
				int signBegin = p.value(rootBeginInterval).signum();
				int signHalf = p.value(intervalHalf).signum();
				if(signBegin == signHalf){
					rootBeginInterval = intervalHalf;
				}
				else{
					rootEndInterval = intervalHalf;
				}
				root = PolynomialRootApproximation.findRootHalleyInInterval(p, decimalPrecision, rootBeginInterval, rootEndInterval, true);
			}
			roots.add(root.getKey());
		}
		return roots;
	}
	/**
	 * Isolates the positive roots of p
	 * @param p			square free polynomial
	 * @param mob		mobius transformation
	 * @param scale		num
	 * @return			list of intervals of positive roots
	 */
	private static List<Pair<BigDecimal, BigDecimal>>vas(Polynomial p, Mobius mob, int scale){
		List<Pair<BigDecimal, BigDecimal>> intervals = new ArrayList<>();
		int var = 0;
		int prevSign = p.coeffs.get(0).compareTo(BigDecimal.ZERO);//-1 negative, 0 zero, 1 positive
		int sign = 0;
		for(int i = 1; i <= p.degree(); i++){
			sign = p.coeffs.get(i).compareTo(BigDecimal.ZERO);
			if(prevSign != sign && sign != 0 && prevSign != 0){
				var++;
			}
			if(sign != 0) prevSign = sign;
		}
		if (var == 0) return intervals;

		BigDecimal upperBound = Bounds.kojimaBound(p, scale);
		if (var == 1){
			BigDecimal a = mob.value(BigDecimal.ZERO, scale).min(mob.valueInfinityOrBound(mob.value(BigDecimal.ZERO, scale), scale));
			BigDecimal b = mob.value(BigDecimal.ZERO, scale).max(mob.valueInfinityOrBound(upperBound, scale));
			intervals.add(new Pair<>(a, b));
			return intervals;
		}
		
		List<BigDecimal> revr = new ArrayList<>();
		for(int i = p.degree(); i >= 0; i--){
			revr.add(p.coeffs.get(i));			
		}
		Polynomial revp = new Polynomial(revr);
		BigDecimal lowerBound = BigDecimal.ONE.divide(Bounds.kojimaBound(revp, scale), scale, RoundingMode.HALF_UP);

		if(lowerBound.compareTo(BigDecimal.ONE) >= 0){
			p = p.substituteWithPolynomial(new Polynomial("x+"+lowerBound.toString()));
			mob = new Mobius(mob.getA(), mob.getB().add(mob.getA().multiply(lowerBound)), mob.getC(), mob.getD().add(mob.getC().multiply(lowerBound)));
		}
		
		Polynomial p01 = new Polynomial(p.coeffs);
		
		p01.substituteThisWithOne_div_X_plus_1_multiplied_x_n();

		Mobius m01 = new Mobius(mob.getB(), mob.getA().add(mob.getB()), mob.getD(), mob.getC().add(mob.getD()));
		
		BigDecimal m = mob.value(BigDecimal.ONE, scale);
		
		Polynomial p1infinity = p.substituteWithPolynomial(new Polynomial("x+1"));
		Mobius m1infinity = new Mobius(mob.getA(), mob.getA().add(mob.getB()),  mob.getC(), mob.getC().add(mob.getD()));
		
		if(p.value(BigDecimal.ONE).abs().compareTo(BigDecimal.ONE.divide(BigDecimal.TEN.pow(scale+1), scale+1, RoundingMode.HALF_UP)) <= 0){
			intervals.addAll(vas(p01, m01, scale));
			intervals.add(new Pair<>(m, m));
			intervals.addAll(vas(p1infinity, m1infinity, scale));
			return intervals;
		} else {
			intervals.addAll(vas(p01, m01, scale));
			intervals.addAll(vas(p1infinity, m1infinity, scale));
		}
		
		return intervals;
	}
	/**
	 * Computes positive roots of p using VAS to isolate roots and Newtons method to approximate them
	 * @param p					square free polynomial to find roots of
	 * @param decimalPrecision	allowed error of precision
	 * @return					positive roots of p
	 */
	private static List<BigDecimal> rootsVAS(Polynomial p, BigDecimal decimalPrecision){
		List<BigDecimal> roots = new ArrayList<>();
		List<Pair<BigDecimal, BigDecimal>> rootsIntervals = vas(p, new Mobius(), decimalPrecision.scale());

		int i = 1;
		for(Pair<BigDecimal, BigDecimal> interval : rootsIntervals){
			BigDecimal rootBeginInterval = interval.getKey();
			BigDecimal rootEndInterval = interval.getValue();
			if(i == rootsIntervals.size()) {				
				BigDecimal sizeOfInter = rootEndInterval.subtract(rootBeginInterval);
				int sizeOfLast = sizeOfInter.signum() == 0 ? 1 : sizeOfInter.precision() - sizeOfInter.scale();
				BigDecimal intervalTenth = (rootEndInterval.subtract(rootBeginInterval)).divide(new BigDecimal("1").scaleByPowerOfTen(sizeOfLast/2), decimalPrecision.scale(), RoundingMode.HALF_UP).add(rootBeginInterval);//10th
				int signBegin = p.value(rootBeginInterval).signum();
				int signHalf = p.value(intervalTenth).signum();
				if(signBegin == signHalf){
					rootBeginInterval = intervalTenth;
				}
				else{
					rootEndInterval = intervalTenth;
				}
			}
			Pair<BigDecimal, Boolean> root = PolynomialRootApproximation.findRootNewtonInInterval(p, decimalPrecision, rootBeginInterval, rootEndInterval, true);
			if(!root.getValue() && rootBeginInterval.compareTo(rootEndInterval) == 0) continue;//infinite loop prevention
			while(!root.getValue()){
				BigDecimal intervalHalf = ((rootEndInterval.subtract(rootBeginInterval)).divide(new BigDecimal("2"), decimalPrecision.scale(), RoundingMode.HALF_UP)).add(rootBeginInterval);
				int signBegin = p.value(rootBeginInterval).signum();
				int signHalf = p.value(intervalHalf).signum();
				if(signBegin == signHalf){
					rootBeginInterval = intervalHalf;
				}
				else{
					rootEndInterval = intervalHalf;
				}
				root = PolynomialRootApproximation.findRootNewtonInInterval(p, decimalPrecision, rootBeginInterval, rootEndInterval, true);
			}
			roots.add(root.getKey());
			i++;
		}
		return roots;
	}
	
	/**
	 * Computes all roots of p using VAS
	 * @param p					square free polynomial
	 * @param decimalPrecision	allowed error of precision
	 * @return					roots of p
	 */
	public static List<BigDecimal> findRootsVAS(Polynomial p, BigDecimal decimalPrecision){
		List<BigDecimal> roots = rootsVAS(p, decimalPrecision);
		List<BigDecimal> negativeRoots = rootsVAS(p.substituteWithMinusX(), decimalPrecision);
		for(BigDecimal bd : negativeRoots){
			roots.add(bd.negate());
		}
		Collections.sort(roots);
		return roots;
	}
	/**
	 * Computes the nth root of number b
	 * @param b		number
	 * @param n		degree of root
	 * @param scale	number of decimal digits required
	 * @return		nth root of b
	 */
	public static BigDecimal nthRoot(BigDecimal b, int n, int scale){
		if(b.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
		Polynomial p = new Polynomial("x^" + n + "-" + b.toString());
		BigDecimal startpoint = BigDecimal.ONE;
		while(p.value(startpoint).compareTo(BigDecimal.ZERO) < 0) startpoint = startpoint.multiply(THREE);//much faster than 1
		return PolynomialRootApproximation.findRootHalleyInInterval(p, BigDecimal.ONE.divide(BigDecimal.TEN.pow(scale)), startpoint, startpoint, false).getKey();
	}
}