# -*- coding: utf-8 -*-

import matplotlib.pyplot as plt
import csv
import argparse

def read_csv_file(filename):
  with open(filename, 'r') as file:
    csvreader = csv.reader(file)
    rows = []
    skipped = False
    for row in csvreader:
      if skipped == False:
        skipped = True
        continue
      rows.append({
        "nr_tasks": int(row[0]),
        "avg_execution_time": float(row[1])
      })
  print(rows)
  return sorted(rows, key=lambda row: row['nr_tasks'])

def Union(lst1, lst2):
  final_list = list(set(lst1) | set(lst2))
  return final_list

# functie pentru trasare grafic ce uneste mai multe puncte definite si salvare figura
def plot(task, x1, y1, x2, y2):
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
  plt.title('Execution performance', fontdict = {'fontsize': 20, 'fontweight': 'medium'})
  plt.xlabel('Nr. clients', fontsize = 14)
  plt.ylabel('Avg Execution time (s)', fontsize = 14)
  plt.legend()
  # salvare imagine, incrementare contor si afisare figura
  plt.savefig("execution_perfomance_" + task, bbox_inches = 'tight')
  # plt.show()
  plt.close()

def compute_graph_data(filename):
  # read csv file
  content = read_csv_file(filename)
  x=[]
  y=[]

  # get data
  for row in content:
    x.append(row['nr_tasks'])
    y.append(row['avg_execution_time'])

  # return data
  return [x, y]

def main():
  # init cmd args parser
  parser = argparse.ArgumentParser()
  parser.add_argument('-t', '--task', help='Task type')
  parser.add_argument('-f1', '--filename1', help='First input file to read data from')
  parser.add_argument('-f2', '--filename2', help='Second input file to read data from')
  args = parser.parse_args()

  [x1, y1] = compute_graph_data(args.filename1)
  print(x1)
  print(y1)

  [x2, y2] = compute_graph_data(args.filename2)
  print(x2)
  print(y2)
  # plot the graph
  plot(args.task, x1, y1, x2, y2)


if __name__ == "__main__":
  main()
