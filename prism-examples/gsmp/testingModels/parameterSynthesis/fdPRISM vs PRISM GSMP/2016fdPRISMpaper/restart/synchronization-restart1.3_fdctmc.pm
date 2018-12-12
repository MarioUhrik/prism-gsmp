fdctmc

// cost in states
const double WAITING_COST=1;

const double TIMEOUT_COST=1;

const double request = 1;
const double answer  = 1;
const double loss    = 0.1;
const double break   = 0.1;

// number of components - 1
const int max_u = 0;

// target states
label "target" = target;
formula target = num_finished = max_u + 1;
formula num_finished = ((s1=3)?1:0);
formula unfinished = max_u+1-num_finished;


rewards 
	 ! target : WAITING_COST;

	[f1] true : TIMEOUT_COST;
endrewards

module synchro

fdelay t1 = 2.0;

	s1 : [0..3] init 1;
	// 0 lost
	// 1 requesting
	// 2 waiting for answer
	// 3 synchronized

//	u : [0..max_u] init 0; 
	// number of finished
	
	[] (s1=1) -> request: (s1'=2);
	[] (s1=2) -> answer : (s1'=3);
	[] (s1<3) -> loss   : (s1'=0);
	[] (s1=3)&!target  -> break  : (s1'=1);

        //deadlock state
        [] target -> 1: (s1'=s1);

	// last the last but 1
	[f1] !target --t1-> 1: (s1'=1);

endmodule

