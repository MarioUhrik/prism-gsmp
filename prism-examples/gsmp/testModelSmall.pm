// This is just a nonsensical model for testing. It does not model anything.
// By default, the parsing and building should be successful!
// Lines that should cause failure are commented out.

gsmp

const int defConstant = 4;

const double timeout = 50.8;

const distribution uniDistr = uniform(4,8.8 + defConstant); // mixed arithmetics test

const distribution erlangTest = erlang(7.1,8); // erlang test (value AND type !)

const distribution expDistr = exponential((defConstant*defConstant*timeout) - defConstant); // constant arithmetics test

module Module

	event testEventDirectUsed = dirac(defConstant);

	event testEventIndirectUnused = erlangTest;

	event testEventErlang = erlangTest;

	event expEventUsedMultipleTimes = expDistr;

	// states
	s : [0..7] init 0;
	d : [0..1] init 0;

	[lbl] s=0 --expEventUsedMultipleTimes-> (s'=1); 

	[lbl2] s=1 --testEventDirectUsed-> 0.5:(s'=2) + 0.5: (d'=1);

	[deadLockTran] s=2 --testEventErlang-> (s'=7); // s=7 is a deadlock state and should be detected and fixed !

	//[lonelySlave] s=5 --slave-> (s'=1); // slave without a master - error

	//[] s=5 --slave-> (s'=1); // slave without a label - error
	
	//nonsensical CTMC commands
	[] s=0 -> 2.5 : (s'=1) + 0.5 : (s'=2) & (d'=1);
	[] s=1 -> 4: (s'=2);
	[] s=2 -> 2.8 : (s'=0) + 9.5 : (s'=1);
	[] d=1 -> 1: (d'=0);
	
endmodule
