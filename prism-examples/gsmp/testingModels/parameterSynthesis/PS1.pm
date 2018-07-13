//Rmax=? [F (items=3)] {(Prod_event1, 1, 1..2)}
//Rmin=? [F (items=3)] {(Prod_event1, 1, 1..2)}

gsmp

const maxItem=3;

rewards
	items>=0 : 1;
endrewards

module Queue

	event Prod_event1 = dirac(1.5);
	event Prod_event2 = dirac(2);
	event Prod_event3 = dirac(3);
	event Serve_event = exponential(2.0);

	items: [0..maxItem] init 0;

	[] items=0 --Prod_event1-> (items'=items+1);
	[] items=1 --Prod_event2-> (items'=items+1);
	[] items=2 --Prod_event3-> (items'=items+1);

	[consumption] items > 0 --Serve_event-> (items'=items-1);
endmodule
