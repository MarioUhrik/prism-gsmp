// This is just a nonsensical model for testing. It does not model anything.
// By default, the parsing and building should be successful!
// Lines that should cause failure are commented out.

gsmp

const defConstant = 5;

const double timeout = 14.5;

const distribution expDistr = exponential(defConstant); 

const distribution diracDistr = dirac(timeout);

module firstModule

	event firstModEvent = expDistr;

	event timeoutEvent = diracDistr;

	first : [0..1] init 0;

	[lol] first=0 --firstModEvent-> (first'=1); 

	[timeout] first=1 --timeoutEvent-> (first'=0);
	
endmodule

module secondModule

	event secondModEvent = exponential(5);

	second : [0..6] init 0;

	[lol] second=0 --secondModEvent-> (second'=1);

	[timeout] second=1 --slave-> (second'=0);

endmodule

module thirdModule

	third : [0..1] init 0;

	[lol] third=0 -> 5: (third'=1);

	[timeout] third=1 --slave-> (third'=0);

endmodule
