//R=? [F (halt=1)]
gsmp

rate lambda = 1000;
rate finishRate = 1;

rewards // combined, they should cause reward a little above 500.5
	[left] true : 1; // alone, this should cause reward a little above 500
	s=0 : 1; // alone, this should cause reward a little above 0.5
endrewards

module m

	s : [0..1] init 0;
	halt : [0..1] init 0;

	[left] s=0 & (halt=0) -> lambda : (s'=1);

	[right] s=1 & (halt=0) -> lambda : (s'=0);

	[finish] (halt=0) -> finishRate : (halt'=1);

endmodule
