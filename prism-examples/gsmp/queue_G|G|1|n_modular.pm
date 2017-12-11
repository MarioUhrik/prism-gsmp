// This is a model of G/G/1/n queue, i.e.
// the inter-arrival time is specified by Prod_event,
// the service time is specified by Serve_event,
//

gsmp

const maxitem=12;

module Producer

	event Prod_event = dirac(10);

	[produce] true --Prod_event-> true; 
	
endmodule

module Server

	event Serve_event = weibull(12,2.0);

	[served] true --Serve_event-> true;

endmodule

module Queue

	items: [0..maxitem] init 0;

	[produce] items<maxitem --slave-> (items'=items+1);
	[served] items>0 --slave-> (items'=items-1);
endmodule