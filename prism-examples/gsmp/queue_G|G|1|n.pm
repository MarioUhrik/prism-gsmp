// This is a model of G/G/1/n queue, i.e.
// the inter-arrival time is specified by Prod_event,
// the service time is specified by Serve_event,
//

gsmp

const maxitem=12;


module Queue

	event Prod_event = dirac(10);
	event Serve_event = weibull(12,2.0);

	items: [0..maxitem] init 0;

	[production] items < maxitem --Prod_event-> (items'=items+1);
	[consumption] items > 0 --Serve_event-> (items'=items-1);
endmodule