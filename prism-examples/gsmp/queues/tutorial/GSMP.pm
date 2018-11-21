gsmp

const maxItem = 12;

rewards
	items>0 : items;
endrewards

module Queue
	event Prod_event = dirac(5);
	event Serve_event = weibull(12,2.0);

	items: [0..maxItem] init 0;

	[production] items < maxItem --Prod_event-> (items'=items+1);
	[consumption] items > 0 --Serve_event-> (items'=items-1);
endmodule
