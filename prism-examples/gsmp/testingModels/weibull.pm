gsmp

const maxItem=2;

rate r = 0.2;

rewards
	items>0 : items;
endrewards

module Queue

	event Prod_event = weibull(1/r, 1); // should be equivalent to the exponential service event

	//event Prod_event = dirac(1/r);
	//event Prod_event = weibull(1/r, 10); // should be approximately similar to the dirac one



	event Serve_event = exponential(r);

	items: [0..maxItem] init 0;

	[production] items < maxItem --Prod_event-> (items'=items+1);
	[consumption] items > 0 --Serve_event-> (items'=items-1);
endmodule
