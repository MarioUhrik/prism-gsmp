
fdctmc

// cost in states
const double WAITING_COST=1;

const double TIMEOUT_COST=1;

const double request = 1;
const double answer  = 1;
const double loss    = 0.1;
const double break   = 0.1;

// number of components - 1
const int max_u = 3;

// target states
label "target" = target;
formula target = num_finished = max_u + 1;
formula num_finished = ((s1=3)?1:0) + ((s2=3)?1:0) + ((s3=3)?1:0) + ((s4=3)?1:0);
formula unfinished = max_u+1-num_finished;


rewards 
	 ! target : WAITING_COST;

	[f1] true : TIMEOUT_COST;
	[f2] true : TIMEOUT_COST;
	[f3] true : TIMEOUT_COST;
	[f4] true : TIMEOUT_COST;
endrewards

module synchro

fdelay t1 = 2.0;
fdelay t2 = 2.0;
fdelay t3 = 2.0;
fdelay t4 = 2.0;

	s1 : [0..3] init 1;
	s2 : [0..3] init 1;
	s3 : [0..3] init 1;
	s4 : [0..3] init 1;
	// 0 lost
	// 1 requesting
	// 2 waiting for answer
	// 3 synchronized

	u : [0..max_u] init 0; 
	// number of finished
	
	[] (s1=1) -> request: (s1'=2);
	[] (s1=2) -> answer : (s1'=3);
	[] (s1<3) -> loss   : (s1'=0);
	[] (s1=3)&!target  -> break  : (s1'=1);

	[] (s2=1) -> request: (s2'=2);
	[] (s2=2) -> answer : (s2'=3);
	[] (s2<3) -> loss   : (s2'=0);
	[] (s2=3)&!target  -> break  : (s2'=1);

	[] (s3=1) -> request: (s3'=2);
	[] (s3=2) -> answer : (s3'=3);
	[] (s3<3) -> loss   : (s3'=0);
	[] (s3=3)&!target  -> break  : (s3'=1);

	[] (s4=1) -> request: (s4'=2);
	[] (s4=2) -> answer : (s4'=3);
	[] (s4<3) -> loss   : (s4'=0);
	[] (s4=3)&!target  -> break  : (s4'=1);

        //deadlock state
        [] target -> 1: (s1'=s1);

	// last the last but 1
	[f1] !target & (u=0) --t1-> 1: (s1'=1) & (s2'=unfinished>=2?1:3) & (s3'=unfinished>=3?1:3) & (s4'=unfinished>=4?1:3) & (u'=num_finished);
	[f2] !target & (u=1) --t2-> 1: (s1'=1) & (s2'=unfinished>=2?1:3) & (s3'=unfinished>=3?1:3) & (s4'=unfinished>=4?1:3) & (u'=num_finished);
	[f3] !target & (u=2) --t3-> 1: (s1'=1) & (s2'=unfinished>=2?1:3) & (s3'=unfinished>=3?1:3) & (s4'=unfinished>=4?1:3) & (u'=num_finished);
	[f4] !target & (u=3) --t4-> 1: (s1'=1) & (s2'=unfinished>=2?1:3) & (s3'=unfinished>=3?1:3) & (s4'=unfinished>=4?1:3) & (u'=num_finished);

endmodule


