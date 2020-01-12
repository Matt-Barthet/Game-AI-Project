import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator

generations = []
averages = []
stdevs = []
error = []

def extractDataPoints(line):
    average = ""
    stdev = ""
    counter = 0

    while line[counter] != '\t':
        counter += 1
    counter += 3
    while line[counter] != '\t':
        average += line[counter]
        counter += 1
    counter += 3
    while line[counter] != '\n':
        stdev += line[counter]
        counter += 1
    averages.append(round(float(average),2))
    stdevs.append(float(stdev))

def extractLine(file):
    line = file.readline()
    if line == "Generation Evolution for Next Move: \n":
        line = file.readline()
        line = file.readline()

    if line is not "\n":
        extractDataPoints(line)
    return line

def extractGeneration(file):
    while(extractLine(file) != "\n"):
        continue

filename = "Genetic Data/Genetic_Data_1578659040178.txt"
samples = 10

file = open(filename)
for i in np.arange(0,6):
    file.readline()
fig = plt.figure()
plt.subplot(1,1,1)
fig.suptitle('Fitness Evolution vs Population Generation (size = 20, gene length = 10, mutation rate = 50%)', fontsize=14, fontweight='bold')

for i in range(0,7):
    extractGeneration(file)
generations = []
averages = []
stdevs = []
error = []

for i in range(0,3):
    extractGeneration(file)

for i in range(0, len(stdevs)):
    error.append(stdevs[i] / np.sqrt(len(stdevs)))

plot = plt.errorbar(x = np.arange(0, len(averages)), y=averages, yerr=error, color = 'blue', ecolor = 'red', capsize = 5)

plt.title('Average Fitness vs Generation Count')
plt.ylabel('Average Fitness')
plt.xlabel('Generations')
plt.grid()
plt.show()

# data to plot
n_groups = 4
means_starter = [8622,
                 8625,
                 3266,
                 2053
                 ]
means_comms = [
    5271,
    3617,
    2468,
    2302

]
std_error_starter = [1170.82,
                     1764.78,
                     342.3,
                     311.33
                     ]
std_error_comms = [778.53,
                   990.88,
                   419.61,
                   475.8
                   ]

# create plot
fig, ax = plt.subplots()
index = np.arange(n_groups)
bar_width = 0.4
opacity = 0.8

rects1 = plt.bar(index, means_comms, bar_width,
                 alpha=opacity,
                 color='b',
                 yerr = std_error_comms,
                 capsize = 5,
                 label='Comm Ghosts')

rects2 = plt.bar(index + bar_width, means_starter, bar_width,
                 alpha=opacity,
                 color='g',
                 yerr = std_error_starter,
                 capsize = 5,
                 label='Starter Ghosts')

plt.ylabel('Average Score / 10 runs')
plt.xticks(index + bar_width/2, ('Q-Learning', 'Rolling Horizon', 'Starter Pacman', 'ISMCTS Agent'))
plt.legend()

plt.tight_layout()
plt.show()