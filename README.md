# The New York Sock Exchange

## Programming and Problem Solving
## Prompt:  
You have inherited a collection of 2n socks from an eccentric great-uncle. Each sock has a color determined by an (R,G,B) triple, where R, G, and B are integers between 0 and 255. Unfortunately, your great-uncle didn't care much for matching his socks, and so the collection of (R,G,B) values is random. You'll likely be embarrassed to wear two socks of different colors, so you aim to pair up socks of similar colors. At the end of the simulation you will have n pairs of socks (you choose which socks to pair) and your total embarrassment is the sum of the L2-norm differences between the (R,G,B) colors of the socks in each pair.

Fortunately for you, there are many other people in a similar situation to you, and as a result a sock exchange has been developed. In the exchange, each participant publicly posts two socks they would like to trade, ranked as first and second. After all participants in the exchange see everybody's sock offers, they list two socks (also ranked) from other players that they would like to receive. The auction house then pairs up compatible trades, where two parties want each others' socks. Trades are ordered by the combined ranks, and trades with the lowest combined rank are executed first, with ties broken randomly. If an earlier trade makes a later trade impossible, then the later trade does not happen.

With many traders and only two socks offered at a time, it is likely that most offers will not lead to actual trades. However, all information at the exchange is public, so that each participant can see both which socks were offered and which were sought by each participant. As a result, participants can refine their behavior in light of how the other participants behaved on previous rounds.

Each of the p players starts with the same number 2n of socks. The 2np (R,G,B) values are chosen uniformly at random from [0,255]. The exchange period lasts t turns, and then each player calculates their total embarrassment. Note that this is not a zero-sum game: both players benefit from a trade. Since we'll be running many simulations with different combinations of players, it may still pay to be cooperative rather than competitive if other players reciprocate.

# Usage
You need a java environment installed.
First compile the simulator:
```sh
$ javac exchange/sim/*.java
```
Then run simulator for fun! For example:
```sh
java exchange.sim.Simulator -p g0 g0 g0 --gui --fps 1
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
