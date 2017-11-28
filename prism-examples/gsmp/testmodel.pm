gsmp

const distribution hi = uniform(4,8.8 + kon); // mixed arithmetics test

const distribution erlangtest = erlang(7.1,8); // erlang test (value AND type !)

const kon = 4;

const lel;

const double heya;

const double timeout = 178.7;

const distribution exptest = exponential((kon*kon*kon) - kon); // constant arithmetics test

const distribution hahah = dirac(timeout);

const int a = 4;

const int b = 1;

module die

	event testevent = dirac(kon);

	event testevent6 = dirac(kon);

	event ev5 = hahah;

	event haihaihai = exptest;

	// local state
	s : [0..7] init 0;
	// value of the die
	d : [0..6] init 0;

	[lol] s=0 --haihaihai-> (s'=1); // multiple exponential masters test

	[lol] s=0 --haihaihai-> (s'=1);

	[lol] s=0 --haihaihai-> (s'=1);

	[lol3] s=1 --testevent-> (s'=1);

	[lol] s=2 --slave-> (s'=1); // slaves assigned to multiple exponential masters test

	[lol] s=2 --slave-> (s'=1);

	//[lonelySlave] s=5 --slave-> (s'=1); // slave without a master - error

	//[] s=5 --slave-> (s'=1); // slave without a label - error
	
	[] s=0 -> 0.5 : (s'=1) + 0.5 : (s'=2);
	[] s=1 -> 0.5 : (s'=3) + 0.5 : (s'=4);
	[] s=2 -> 0.5 : (s'=5) + 0.5 : (s'=6);
	[] s=3 -> 0.5 : (s'=1) + 0.5 : (s'=7) & (d'=1);
	[] s=4 -> 0.5 : (s'=7) & (d'=2) + 0.5 : (s'=7) & (d'=3);
	[] s=5 -> 0.5 : (s'=7) & (d'=4) + 0.5 : (s'=7) & (d'=5);
	[] s=6 -> 0.5 : (s'=2) + 0.5 : (s'=7) & (d'=6);
	[] s=7 -> (s'=7);
	
endmodule

module secondModule

	//event testevent6 = dirac(kon); // event not renamed in the last line - error

	//event ev5 = hahah; // event not renamed in the last line - error

	event secondModEvent = exponential(a); // if this one is not exponential, label lol2 has multiple non-exponential masters - error

	event localhai = exptest;

	dk : [0..6] init 0;

	[lol] dk=0 --localhai-> (dk'=1);


	[lol2] dk=1 --secondModEvent-> (dk'=0);

endmodule

module rm = secondModule [dk = dk3, secondModEvent = thirdModEvent, localhai = localhaiThird] endmodule
