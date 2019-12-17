import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator

generations = []
averages = []
stdevs = []

def extractDataPoints(line):
    generation = ""
    average = ""
    stdev = ""
    counter = 0
    while line[counter] != ',':
        generation += line[counter]
        counter += 1
    counter +=1
    while line[counter] != ',':
        average += line[counter]
        counter += 1
    counter +=1
    while line[counter] != '\n':
        stdev += line[counter]
        counter += 1
    generations.append(int(generation))
    averages.append(float(average))
    stdevs.append(float(stdev))
        

def extractLine(file):
    line = file.readline()
    if line is not "\n":
        extractDataPoints(line)
    return line

def extractGeneration(file):
    while(extractLine(file) != "\n"):
        continue

filename = "Genetic_Data_1576595063636.txt"
file = open(filename)


fig = plt.figure()
fig.suptitle('Evolution of Standard Fitness Deviation vs Population Generation', fontsize=14, fontweight='bold')

for i in range(0,20):
    generations = []
    averages = []
    stdevs = []
    extractGeneration(file)
    plt.plot(generations, stdevs)

plt.title('size = 20, gene length = 5')
plt.ylabel('Standard Fitness Deviation')
plt.xlabel('Generations')
plt.grid()


fig = plt.figure()
fig.suptitle('Evolution of Average Fitness vs Population Generation', fontsize=14, fontweight='bold')

file = open(filename)

for i in range(0,20):
    generations = []
    averages = []
    stdevs = []
    extractGeneration(file)
    plt.plot(generations, averages)

plt.title('size = 20, gene length = 5')
plt.ylabel('Average Fitness')
plt.xlabel('Generations')
plt.grid()
plt.show()




