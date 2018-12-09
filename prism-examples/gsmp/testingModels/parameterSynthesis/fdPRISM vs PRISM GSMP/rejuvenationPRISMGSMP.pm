gsmp //Rmin=? [F (s=3)] {(t1,1, 0.00000001..5)}

// cost in states
const double WAITING_COST=1;

const double TIMEOUT_COST=0.4;

// target states
label "target" = s=3;

rewards 
	s<=2 : WAITING_COST;

	[f1] true : TIMEOUT_COST;
endrewards

module synchro

event t1= dirac(1.0);

	s : [0..3] init 0;
	// 0 synchronized
	// 1 non-synchronized
	// 2 fixing
	// 3 done
	

	// non-synchronization
	[] (s=0) -> 0.4: (s'=1);

	// production of item
	[] (s=0) -> 1.6: (s'=3);
	[] (s=1) -> 0.4: (s'=3);

	// start fixing
	[f1] (s<=1)  --t1-> 1: (s'=2);

	//finished fixing
	[] (s=2) -> 2: (s'=0);

endmodule
