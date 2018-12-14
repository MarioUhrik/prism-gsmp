gsmp

const int qCapacity = 10000;

const double timeout = 1;
const double lambda = 1;

module main
event prod = dirac(timeout);

qSize : [0..qCapacity] init 0;

[produce] (qSize <= qCapacity)--prod -> (qSize' = min(qSize+1,qCapacity));
[consume] (qSize > 0) -> lambda: (qSize' = qSize - 1);

endmodule
