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

	event firstTimeout = diracDistr;

	first : [0..1] init 0;

	[lol] first=0 --firstModEvent-> (first'=1); 

	[] first=1 --firstTimeout-> (first'=0);
	
endmodule

module secondModule

	event secondModEvent = exponential(5);

	event secondTimeout = diracDistr;

	second : [0..6] init 0;

	[lol] second=0 --secondModEvent-> (second'=1);

	[] second=1 --secondTimeout-> (second'=0);

endmodule

module thirdModule

	event thirdTimeout = diracDistr;

	third : [0..1] init 0;

	[lol] third=0 -> 5: (third'=1);

	[] third=1 --thirdTimeout-> (third'=0);

endmodule
