// This is a model is taken (and reworked into using general distributions) from http://www.fi.muni.cz/~xuhrik/
// It models rejuvenation of a system that is initially OK, but gets degraded over time and eventually fails.
// Rejuvenation may cheaply reverse this aging process, saving a lot of resources and time wasted by a potential failure.

// The model was reworked into using many different time distributions to showcase some of the capabilities of GSMPs.
// One unit of time represents one hour.

gsmp

// costs in states
const double UNAVAILABLE_COST=20.0;
const double DEGRADED_COST=0.5;

rewards 
	avail=0 : UNAVAILABLE_COST; // cost for system unavailability
	status=1 & avail=1 : DEGRADED_COST; // cost for availability with degraded performance

endrewards

module rejuvenation

//event declarations________________________// Mean/average time until they occur, and why are these distribution and parameters chosen
event repairEvent      = uniform(1,24);     //Meant to take anything between 1 and 24 hours. The repair jobs vary in difficulty.
event maintenanceEvent = erlang(10,3600);   //Meant to happen after around 360 hours of operation  - when enough people call to complain.
event rejuvenationEvent= dirac(1);          //Meant to take exactly 1 hour. It is a simple restart and there are never any complications.
event failureEvent     = weibull(50,2350);  //Meant to usually happen between 2000 and 2500 hours of operation. As soon as the warranty expires.
event degradationEvent = exponential(1/240);//Meant to happen on average after 240 hours of peak-level operation. But it can easily be anytime.


	status: [0..2] init 0; // 0-OK, 1-degraded, 2-failed
	avail: [0..1] init 1; // 0-unavailable, 1-available
	maintenance: [0..1] init 0; // 0-waiting for rejuvenation, 1-rejuvenation in process
	

	[degrade] (status=0) & (avail=1) --degradationEvent-> (status'=1);    					

	[fail] (status=1) & (avail=1) --failureEvent-> (status'=2) & (avail'=0); 

	[repair] (status=2) --repairEvent-> (status'=0) & (avail'=1); 			

	[beginRejuvenation] (maintenance=0) & (avail=1) --maintenanceEvent-> (maintenance'=1) & (avail'=0);

	[rejuvenate] (maintenance=1) --rejuvenationEvent-> (maintenance'=0) & (avail'=1) & (status'=0);

endmodule
