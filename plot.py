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

filename = "Genetic_Data_1576775302896.txt"
samples = 10

file = open(filename)
fig = plt.figure()
plt.subplot(1,2,1)
fig.suptitle('Fitness Evolution vs Population Generation (size = 20, gene length = 5, mutation rate = 50%)', fontsize=14, fontweight='bold')

for i in range(0,samples):
    generations = []
    averages = []
    stdevs = []
    extractGeneration(file)
    plt.plot(generations, stdevs)

plt.title('Standard Deviation vs Generation Count')
plt.ylabel('Standard Fitness Deviation')
plt.xlabel('Generations')
plt.grid()

file = open(filename)
plt.subplot(1,2,2)

for i in range(0,samples):
    generations = []
    averages = []
    stdevs = []
    extractGeneration(file)
    plt.plot(generations, averages)

plt.title('Average Fitness vs Generation Count')
plt.ylabel('Average Fitness')
plt.xlabel('Generations')
plt.grid()
plt.show()



