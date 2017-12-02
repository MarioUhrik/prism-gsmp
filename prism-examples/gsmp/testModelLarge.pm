// This is just a nonsensical model for testing. It does not model anything.
// By default, the parsing and building should be successful!
// Lines that should cause failure are commented out.

gsmp

const distribution uniDistr = uniform(4,8.8 + defConstant); // mixed arithmetics test

const distribution erlangTest = erlang(7.1,8); // erlang test (value AND type !)

const defConstant = 4;

const undefConstant;

const double undefConstant2;

const double timeout = 178.7;

const distribution expDistr = exponential((defConstant*defConstant*timeout) - defConstant); // constant arithmetics test

const distribution diracDistr = dirac(timeout);

const int a = 4;

const int b = 1;

module firstModule

	event testEventDirectUsed = dirac(defConstant);

	event testEventDirectUnused = dirac(defConstant);

	event dirEventUnused = diracDistr;

	event expEventUsedMultipleTimes = expDistr;

	// states
	s : [0..7] init 0;
	d : [0..6] init 0;

	[lol] s=0 --expEventUsedMultipleTimes-> (s'=1); // multiple exponential masters test

	[lol] s=0 --expEventUsedMultipleTimes-> (s'=1);

	[lol] s=0 --expEventUsedMultipleTimes-> (s'=1);

	[lol3] s=1 --testEventDirectUsed-> (s'=1);

	[lol] s=2 --slave-> (s'=1); // slaves assigned to multiple exponential masters test

	[lol] s=2 --slave-> (s'=1);

	//[lonelySlave] s=5 --slave-> (s'=1); // slave without a master - error

	//[] s=5 --slave-> (s'=1); // slave without a label - error
	
	//nonsensical CTMC commands
	[] s=0 -> 0.5 : (s'=1) + 0.5 : (s'=2);
	[] s=1 -> 0.5 : (s'=3) + 0.5 : (s'=4);
	[] s=2 -> 0.5 : (s'=5) + 0.5 : (s'=6);
	[] s=3 -> 0.5 : (s'=1) + 0.5 : (s'=7) & (d'=1);
	[] s=4 -> 0.5 : (s'=7) & (d'=2) + 0.5 : (s'=7) & (d'=3);
	[] s=5 -> 0.5 : (s'=7) & (d'=4) + 0.5 : (s'=7) & (d'=5);
	[] s=6 -> 0.5 : (s'=2) + 0.5 : (s'=7) & (d'=6);
	[] s=7 -> (s'=7);
	
endmodule

module secondModule

	//event testevent6 = dirac(kon); // event not renamed in the last line - error

	//event ev5 = hahah; // event not renamed in the last line - error

	event secondModEventDirect = exponential(a); // if this one is not exponential, label lol2 has multiple non-exponential masters - error

	event secondModEvent = expDistr;

	dk : [0..6] init 0;

	[lol] dk=0 --secondModEvent-> (dk'=1);


	[lol2] dk=1 --secondModEventDirect-> (dk'=0);

endmodule

module rm = secondModule [dk = dk3, secondModEvent = thirdModEvent, secondModEventDirect = thirdModEventDirect] endmodule
