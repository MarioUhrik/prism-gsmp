gsmp

const int qCapacity = 100;

const double lambda = 1;

module main
event prod = erlang(2,2);

qSize : [0..qCapacity] init 0;

[produce] (qSize <= qCapacity)--prod -> (qSize' = min(qSize+1,qCapacity));
[consume] (qSize > 0) -> lambda: (qSize' = qSize - 1);

endmodule
