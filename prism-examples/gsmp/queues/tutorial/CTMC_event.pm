gsmp

const maxItem = 12;
rate prodRate = 1.0;
rate serveRate = 0.9;

rewards
	items>0 : items;
endrewards

module Queue
	event Prod_event = exponential(prodRate);
	event Serve_event = exponential(serveRate);

	items: [0..maxItem] init 0;

	[production] items < maxItem --Prod_event-> (items'=items+1);
	[consumption] items > 0 --Serve_event-> (items'=items-1);
endmodule
