import subprocess, os, sys

def save_float(x):
    try:
        return float(x)
    except ValueError:
        return None

def rotate_list(l, x):
    return l[-x:] + l[:-x]

num_pairs = 10
turnlimit = 100
timelimit = 1000  # in milliseconds
players = ['g1', 'g2', 'g3', 'g4', 'g5', 'g6']
repetition = 1

# Generating random seed for each run
primal_seed = 20171030
import random
random.seed(primal_seed)
seeds = []
for i in range(repetition):
    seeds.append(random.randrange(2147483647))
print(seeds)


results = {}
for p in players:
    results[p] = []

for run in range(repetition):
    # rotate the player list
    for k in range(len(players)):
        p = open("tmp.log", "w")
        err = open("err.log", "w")
        players_to_run = rotate_list(players, k)
        subprocess.run(["java", "exchange.sim.Simulator", "--silent", "-s", str(seeds[run]), "-n", str(num_pairs), "-t", str(turnlimit), "-tl", str(timelimit), "-p"] + players_to_run, stdout = p, stderr = err)
        p.close()
        err.close()
        with open("tmp.log", "r") as log:
            t = log.readlines()[-len(players):]
            for i in range(len(players)):
                score = [save_float(s) for s in t[i].split()][-1]
                if (t[i].find("illegal") != -1) or (t[i].find("timed") != -1):
                    score = -1
                results[players_to_run[i]].append(score)
            # if (len(parsed) == 0):
            #     results.append(-1)
            # results.extend(parsed)
            log.close()

# TODO: Add your code here
for player, scores in results.items():
    print(player + "'s score: " + str(scores))
