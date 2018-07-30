//R=? [S]
gsmp

rate lambda = 1000;

rewards //combined, they should cause reward 500.5
	[left] true : 1; // alone, this should cause reward 500
	s=0 : 1; // alone, this should cause reward 0.5
endrewards

module m

	s : [0..1] init 0;

	[left] s=0 -> lambda : (s'=1);

	[right] s=1 -> lambda : (s'=0);

endmodule
