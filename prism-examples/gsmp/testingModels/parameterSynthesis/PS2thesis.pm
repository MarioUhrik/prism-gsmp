// This is a model taken (and modified) from http://www.fi.muni.cz/~xuhrik/
// R=? [F (target=1)]   ==  37717
// Rmin=? [F (target=1)] {(maintenanceEvent, 1, 0.0000001..50)} == 23.1783667

gsmp

// costs in states
const double UNAVAILABLE_COST=5.0;
const double DEGRADED_COST=0.01;
const double REPAIR_COST=2.0;

rewards 
	avail=0 : UNAVAILABLE_COST; // cost for system unavailability
	status=1 & avail=1 : DEGRADED_COST; // cost for availability with degraded performance
	[repair] true: REPAIR_COST; // additional repair costs
endrewards

module rejuvenation

event repairEvent      = uniform(2, 6);     
event maintenanceEvent = dirac(25); 
event rejuvenationEvent= uniform(0.06, 0.1);        
event failureEvent     = exponential(1/225);
event degradationEvent = exponential(1/50);

	status: [0..2] init 0; // 0-OK, 1-degraded, 2-failed
	avail: [0..1] init 1; // 0-unavailable, 1-available
	maintenance: [0..1] init 0; // 0-waiting for rejuvenation, 1-rejuvenation in process
	target: [0..1] init 0; //reachability target

	//[reachTarget] (target=0) -> 0.000001: (target'=1) & (status'=0) &  (avail'=1) & (maintenance'=0);
	[reachTarget] (target=0) -> 0.000001: (target'=1);

	[degrade] (status=0) & (avail=1) & (target=0) --degradationEvent-> (status'=1);    					

	[fail] (status=1) & (avail=1) & (target=0) --failureEvent-> (status'=2) & (avail'=0); 

	[repair] (status=2) & (target=0) --repairEvent-> (status'=0) & (avail'=1); 			

	[beginRejuvenation] (maintenance=0) & (avail=1) & (target=0) --maintenanceEvent-> (maintenance'=1) & (avail'=0);

	[rejuvenate] (maintenance=1) & (target=0) --rejuvenationEvent-> (maintenance'=0) & (avail'=1) & (status'=0);

endmodule
