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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PolyRoots {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		List<Polynomial> polynomials34_67 = new ArrayList<>();
		List<Polynomial> polynomials68_101 = new ArrayList<>();
		int i = 0;
		try(BufferedReader br = new BufferedReader(new FileReader("polynomy.txt"))) {
		    String line = br.readLine();
	
		    while (line != null) {
		    	if(i % 4 == 0){
		    		String[] vhc = line.split("\\+");
		    		StringBuilder vhc2 = new StringBuilder();
		    		for(int j = vhc.length - 1; j > 0; j--){
		    			vhc2.append(vhc[j] + "+");
		    		}
		    		vhc2.append(vhc[0]);
		    		Polynomial readP = new Polynomial (vhc2.toString());
		    		if(readP.degree() < 68){
		    			polynomials34_67.add(readP);
		    		} else {
		    			polynomials68_101.add(readP);
		    		}
		    	}
		    	i++;
		        line = br.readLine();
		    }
		}
		
		int precisionAll = 20;
		
		List<Long> vcaN34_67 = new ArrayList<>();
		for(int j = 0; j < polynomials34_67.size(); j++){
			int precision = precisionAll;
			BigDecimal preci = BigDecimal.ONE.divide(new BigDecimal("10").pow(precision));
			BigDecimal start = new BigDecimal("-20");
			BigDecimal end = new BigDecimal("40");
			long t0 = System.nanoTime();
			List<BigDecimal> roots = PolynomialRootFinding.findRootsInIntervalVCANewton(polynomials34_67.get(j), start, end, preci);
			long t1 = System.nanoTime();
			vcaN34_67.add(TimeUnit.MILLISECONDS.convert((t1 - t0), TimeUnit.NANOSECONDS));
			//System.out.println("roots: " + roots.toString());
		}
		
		System.out.println("vca n done");
		
		List<Long> vcaH34_67 = new ArrayList<>();
		for(int j = 0; j < polynomials34_67.size(); j++){
			int precision = precisionAll;
			BigDecimal preci = BigDecimal.ONE.divide(new BigDecimal("10").pow(precision));
			BigDecimal start = new BigDecimal("-20");
			BigDecimal end = new BigDecimal("40");
			long t0 = System.nanoTime();
			List<BigDecimal> roots = PolynomialRootFinding.findRootsInIntervalVCAHalley(polynomials34_67.get(j), start, end, preci);
			long t1 = System.nanoTime();
			vcaH34_67.add(TimeUnit.MILLISECONDS.convert((t1 - t0), TimeUnit.NANOSECONDS));
			//System.out.println("roots: " + roots.toString());
		}
		
		System.out.println("vca h done");
		
		List<Long> sturm34_67 = new ArrayList<>();
		for(int j = 0; j < polynomials34_67.size(); j++){
			int precision = precisionAll;
			BigDecimal start = new BigDecimal("-20");
			BigDecimal end = new BigDecimal("40");
			SturmChain sc = new SturmChain(polynomials34_67.get(j), 120);
			long t0 = System.nanoTime();
			List<BigDecimal> roots = PolynomialRootFinding.rootsOfPolynomialInIntervalUsingSturmTheoremAndBisectionAndNewtonMethod(polynomials34_67.get(j), start, end, precision, sc);
			long t1 = System.nanoTime();
			sturm34_67.add(TimeUnit.MILLISECONDS.convert((t1 - t0), TimeUnit.NANOSECONDS));
			//System.out.println("roots: " + roots.toString());
		}
		
		System.out.println("sturm n done");
		
		List<Long> vasN34_67 = new ArrayList<>();
		for(int j = 0; j < polynomials34_67.size(); j++){
			int precision = precisionAll;
			BigDecimal preci = BigDecimal.ONE.divide(new BigDecimal("10").pow(precision));
			long t0 = System.nanoTime();
			List<BigDecimal> roots = PolynomialRootFinding.findRootsVAS(polynomials34_67.get(j), preci);
			long t1 = System.nanoTime();
			vasN34_67.add(TimeUnit.MILLISECONDS.convert((t1 - t0), TimeUnit.NANOSECONDS));
			//System.out.println("roots: " + roots.toString());
		}
		System.out.println("vas n done");
		
		Collections.sort(vcaN34_67);
		Collections.sort(vcaH34_67);
		Collections.sort(sturm34_67);
		Collections.sort(vasN34_67);
	
		Long vcaNavg = 0L;
		Long vcaHavg = 0L;
		Long vasNavg = 0L;
		Long sturmavg = 0L;
		
		System.out.println( "VCA+N & VCA+H & VAS+N & Sturm+N \\\\" );
		for(int j = 0; j < polynomials34_67.size(); j++){
			vcaNavg += vcaN34_67.get(j);
			vcaHavg += vcaH34_67.get(j);
			vasNavg += vasN34_67.get(j);
			sturmavg += sturm34_67.get(j);
			System.out.println(vcaN34_67.get(j) + " & "  + vcaH34_67.get(j) + " & " + vasN34_67.get(j) + " & " + sturm34_67.get(j) + " \\\\");
		}
		
		vcaNavg = vcaNavg/polynomials34_67.size();
		vcaHavg = vcaHavg/polynomials34_67.size();
		vasNavg = vasNavg/polynomials34_67.size();
		sturmavg = sturmavg/polynomials34_67.size();
		
		System.out.println( "Average & " + vcaNavg + " & "  + vcaHavg + " & " + vasNavg + " & " + sturmavg + " \\\\");
		System.out.println( "Minimum & " + vcaN34_67.get(0) + " & "  + vcaH34_67.get(0) + " & " + vasN34_67.get(0) + " & " + sturm34_67.get(0) + " \\\\");
		System.out.println( "Maximum & " + vcaN34_67.get(polynomials34_67.size()-1) + " & "  + vcaH34_67.get(polynomials34_67.size()-1) + " & " + vasN34_67.get(polynomials34_67.size()-1) + " & " + sturm34_67.get(polynomials34_67.size()-1) + " \\\\");
		System.out.println( "Median & " + vcaN34_67.get(polynomials34_67.size()/2) + " & "  + vcaH34_67.get(polynomials34_67.size()/2) + " & " + vasN34_67.get(polynomials34_67.size()/2) + " & " + sturm34_67.get(polynomials34_67.size()/2) + " \\\\");
		
	}
}
