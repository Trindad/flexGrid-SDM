#!/usr/bin/python3

import numpy as np
import matplotlib.pyplot as plt
from matplotlib import style
import sys

style.use("ggplot")

from sklearn.cluster import KMeans

global str

class MyKMeans:
    def runKMeans(self, data, k):
        X = np.array(data)

        kmeans = KMeans(n_clusters=k)
        kmeans.fit(X)

        centroids = kmeans.cluster_centers_
        labels = kmeans.labels_

        str_labels = '-'.join(list(map((lambda x: str(x)), labels)))
        return str_labels

    #Plot graffic 
    def plotGraphic(self, labels, X, centroids):
        colors = ["g.", "r."]

        for i in range(len(X)):
            print("coordinate:",X[i],labels[i])
            plt.plot(X[i][0],X[i][1], colors[labels[i]],markersize = 10)

        plt.scatter(centroids[:,0], centroids[:,1], marker = "x", s = 150, linewidths = 5, zorder = 10)
        plt.show()

k = int(sys.argv[1])

not_json = sys.argv[2]
arr1 = not_json[2:-2].split("][")

data = [];

for s in arr1:
    arr2 = s.split(",");
    data.append(list(map((lambda x: float(x)), arr2)))

kk = MyKMeans()
print(kk.runKMeans(data, k))