gsmp

const maxItem = 12;

rewards
	items>0 : items;
endrewards

module Queue
	event Prod_event = dirac(5);
	//event Prod_event = uniform(5,5.1);
	//event Prod_event = erlang(20/5,20);
	//event Prod_event = exponential(0.25);
	//event Prod_event = weibull(4, 1);
	event Serve_event = exponential(0.25);

	items: [0..maxItem] init 0;

	[production] items < maxItem --Prod_event-> (items'=items+1);
	[consumption] items > 0 --Serve_event-> (items'=items-1);
endmodule
