# prism-gsmp
PRISM model checker fork that adds modelling and analysis using Generalized Semi-Markov Processes (GSMP).
GSMPs may be seen as CTMCs with any number of concurrently active state-changing "events" at any given time.
These "events" may have any general distribution on time of occurrence (instead of just exponential distribution as is the case of CTMC).
Thus, the modeling capability of GSMP is far greater.

## Features:
- Design and working implementation of an extension of PRISM modelling language that adds support for GSMP.
For now, only the following types of distributions are supported:
  * [Exponential distribution](https://en.wikipedia.org/wiki/Exponential_distribution) with unconstrained real rate parameter
  * [Dirac distribution](https://en.wikipedia.org/wiki/Degenerate_distribution) with real timeout parameter t>0
  * [Uniform distribution](https://en.wikipedia.org/wiki/Uniform_distribution_(continuous)) with real parameters b>a>0
  * [Erlang distribution](https://en.wikipedia.org/wiki/Erlang_distribution) with unconstrained real rate parameter and unconstrained integer shape parameter
  * [Weibull distribution](https://en.wikipedia.org/wiki/Weibull_distribution) with unconstrained real shape and scale parameters

Formal definition of GSMP, syntax and semantics of the language extension can be found [here](https://github.com/VojtechRehak/prism-gsmp/blob/master/doc/prism-gsmp-property.pdf).

Example GSMP models written in this language extension can be found [here](https://github.com/VojtechRehak/prism-gsmp/tree/master/prism-examples/gsmp).


- Working implementation of ACTMC analysis.
ACTMC is a GSMP with only up to one non-exponentially distributed event active in any given state.
With this restraint, using the method of subordinated Markov chains, we have implemented the following kinds of analysis:
  * Steady-state probabilities
  * Steady-state rewards
  * Reachability rewards
  
- Working implementation of optimal parameter synthesis for minimizing/maximizing the reachability reward.
  E.g. given an ACTMC, find the optimal distribution parameter of a particular event such that the reachability reward is maximized.
  This is implemented using efficient algorithms recently published in [the PhD thesis of Lubos Korenciak](https://is.muni.cz/th/zaes9/main.pdf?lang=en).
