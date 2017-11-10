all:
	java exchange.sim.Simulator -p6 g6 -t 30 -n 10 --seed 20171108

compile:
	javac exchange/sim/*.java

clean:
	rm exchange/*/*.class
