gsmp // instead of a ctmc

const maxItem = 12;
rate prodRate = 1.0;
rate serveRate = 0.9;

rewards
	items>0 : items;
endrewards

module Queue
	items: [0..maxItem] init 0;

	[production] items < maxItem -> prodRate: (items'=items+1);
	[consumption] items > 0 -> serveRate: (items'=items-1);
endmodule

