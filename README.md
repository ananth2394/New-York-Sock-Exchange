# PPS 2017 - Escape
## Usage
You need a java environment installed.
First compile the simulator:
```sh
$ javac exchange/sim/*.java
```
Then run simulator for fun! For example:
```sh
java exchange.sim.Simulator -p g4 g4 g4 --gui --fps 1
```
| Parameters | Meanings |
| ------ | ------ |
| `-p/--players player0 player1 ...` | Specifying the players |
| `-n [integer number]` | Specifying the number of pairs of socks |
| `-t [integer number]/--turns [integer number]` | Specifying turns for exchanging |
| `-g/--gui` | Enable GUI |
| `--fps [float number]` | Set fps |
| `-tl/--timelimit [integer number]` | Set the total timelimit for each player (in millisecond) |
| `-v/--verbose` | Enable the detailed events log |
| `-l/--log [file]` | Save the detailed events log to file |
