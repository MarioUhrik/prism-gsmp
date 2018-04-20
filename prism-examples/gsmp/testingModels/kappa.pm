// The purpose of this model is to create such a small probability in the reduced DTMC,
// that the probability gets rounded to zero (double precision), thus affecting the steady-state distribution.
// The backbone of this model is essentially a single-direction exponentially distributed queue of capacity maxItem.
// The queue is getting filled up at rate 1/lambda.
// At the same time, from the very beginning, there is an active dirac event timeout with delay 1/lambda.
// Experiment succeeds if the queue manages to fill up completely before timeout.
// Experiment fails if the timeout occurs before the queue is full.

// experimental steady-state distribution results for time = 5000, maxItem=10, constant kappa = 10^-20:
//1:=0.9999999600000008
//3:=3.9999998400000033E-8
//5:=7.999999680000006E-16
// where 21 is the successful state, i.e. the successful outcome is not reachable.

// experimental steady-state distribution results for time = 5000, maxItem=10, constant kappa = 10^-40:
//1:=0.9999999600000008
//3:=3.9999998400000033E-8
//5:=7.999999680000006E-16
//7:=1.0666666240000009E-23
//9:=1.0666666240000009E-31
//11:=8.533332992000007E-40
// where 21 is the successful state, i.e. the successful outcome is not reachable.

// experimental steady-state distribution results for time = 5000, maxItem=10, computed kappa = 10^-683:
//1:=0.9999999600000008
//3:=3.9999998400000033E-8
//5:=7.999999680000006E-16
//7:=1.0666666240000009E-23
//9:=1.0666666240000009E-31
//11:=8.533332992000007E-40
//13:=5.688888661333339E-48
//15:=3.2507935207619077E-56
//17:=1.6253967603809538E-64
//19:=7.223985601693129E-73
//21:=2.8895942511848666E-81
// where 21 is the successful state, i.e. successful outcome is reachable.

// experimental steady-state distribution results for time = 90000000, maxItem=20, computed kappa = 10^-1368:
//1:=0.9999999999999999
//3:=1.2345679012345678E-16
//5:=7.62078951379363E-33
//7:=3.1361273719315352E-49
//9:=9.679405468924493E-66
//11:=2.389976658993702E-82
//13:=4.9176474464891E-99
//15:=8.67309955289083E-116
//17:=1.3384412890263629E-132
//19:=1.8359962812432963E-149
//21:=2.266662075609008E-166
//23:=2.543952946811457E-183
//25:=2.617235541987096E-200
//27:=2.485503838544251E-217
//29:=2.1918023267585992E-234
//31:=1.8039525323116043E-251
//33:=1.391938682339201E-268
//35:=1.0108487162957162E-285
//37:=6.933118767460332E-303
//39:=4.505E-320
// where 41 is the successful state, i.e. successful outcome is not reachable.

// Conclusion: 
//  Given arbitrary precision data type for storage of the probabilities, kappa must be computed.
//  However, when the probability is lowered to such a small value that
//  it cannot be stored in type Double (10^-350 or smaller), the probability is still rounded to 0.
//  Hence constant kappa smaller than the smallest value the data type used to store the probability can hold 
//  (e.g. 10^-350 for type Double) is sufficient.

gsmp

const maxItem=10; // Adjust at will. Increase this to decrease the probability of success.
rate time = 5000; // Adjust at will. Increase this to decrease the probability of success.

rewards
	items>0 : items;
endrewards

module Queue

	event enqueue = exponential(1/time);
	event timeout = dirac(1/time);

	items: [0..maxItem] init 0;
	outcome: [0..2] init 0; // 0- timeout hasnt occurred yet 1- failure, 2- success

	[production]   (items < maxItem) & (outcome = 0) --enqueue-> (items'=items+1);
	[timeout_bad]  (items < maxItem) & (outcome = 0) --timeout-> (outcome'=1);
	[timeout_good] (items = maxItem) & (outcome = 0) --timeout-> (outcome'=2);
endmodule
