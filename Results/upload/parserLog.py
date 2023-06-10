# -*- coding: utf-8 -*-
import argparse
import matplotlib.pyplot as plt
import math

def parseLine(rawLine, resource):
  line = rawLine.strip().split()
  
  if resource == "CPU":
    return [line[0], line[1]]
  
  if resource == "Real":
    return [line[0], line[2]]

  return [line[0], line[3]]

def parseFile(filename, resource):
  f = open(filename)
  content = f.readlines()
  f.close()
  X = []
  Y = []

  for index, line in enumerate(content):
    if index == 0:
      continue
    [x, y] = parseLine(line, resource)
    X.append(math.floor(float(x)))
    Y.append(float(y))

  return [X, Y]

def Union(lst1, lst2):
  final_list = list(set(lst1) | set(lst2))
  return final_list

def plot(task, x1, y1, x2, y2, resource):
  assert(x1 == x2)

  # setare dimensiune figura si auto ajustare
  plt.rcParams["figure.figsize"] = [10, 12]
  plt.rcParams["figure.autolayout"] = True

  # creare grafic (punctele sunt formate din perechi (x, y), valori preluate din cei doi vectori definiti)
  plt.plot(x1, y1, color="r", marker='o', label="Threads")
  plt.plot(x1, y2, color="g", marker='o', label="CompletableFuture")
  # afisare pe axe doar a valorilor punctelor definite
  plt.xticks(x1, fontsize = 5)
  plt.yticks(Union(y1, y2), fontsize = 5)
  # setare titlu, denumire coordonate si font
  title = ""
  yLabel = ""

  if resource == "CPU":
    title = "CPU utilization"
    yLabel = "CPU (%)"
  else:
    title = "Real Memory uilization"
    yLabel = "RRS Memory (MB)"
  
  plt.title(title, fontdict = {'fontsize': 20, 'fontweight': 'medium'})
  plt.xlabel('time(s)', fontsize = 14)
  plt.ylabel(yLabel, fontsize = 14)
  plt.legend()
  # salvare imagine, incrementare contor si afisare figura
  plt.savefig(resource + "_" + task, bbox_inches = 'tight')
  # plt.show()
  plt.close()

def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('-t', '--task', help='Task type')
  parser.add_argument('-f1', '--filename1', help='First log input file to read data from')
  parser.add_argument('-f2', '--filename2', help='Second log input file to read data from')
  parser.add_argument('-r', '--resource', help='Resource to plot')
  args = parser.parse_args()
  
  [X1, Y1] = parseFile(args.filename1, args.resource)
  print(X1)
  print(Y1)
  
  [X2, Y2] = parseFile(args.filename2, args.resource)
  print(X2)
  print(Y2)

  plot(args.task, X1, Y1, X2, Y2, args.resource)

if __name__ == "__main__":
  main()
