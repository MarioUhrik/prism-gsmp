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

	event testEventDirectUnused = erlangTest;

	event expEventUsedMultipleTimes = expDistr;

	// states
	s : [0..7] init 0;

	[lol] s=0 --expEventUsedMultipleTimes-> (s'=1); 

	[lol3] s=1 --testEventDirectUsed-> (s'=2);

	//[lonelySlave] s=5 --slave-> (s'=1); // slave without a master - error

	//[] s=5 --slave-> (s'=1); // slave without a label - error
	
	//nonsensical CTMC commands
	[] s=0 -> 0.5 : (s'=1) + 0.5 : (s'=2);
	[] s=1 -> (s'=2);
	[] s=2 -> 0.5 : (s'=0) + 0.5 : (s'=1);
	
endmodule
