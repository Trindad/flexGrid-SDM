#!/usr/bin/python3

import numpy as np
import matplotlib.pyplot as plt
from matplotlib import style
import sys

style.use("ggplot")

from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_samples, silhouette_score

np.random.seed(0)

global str

def arrayOfStrings(arr):
    return list(map((lambda x: str(x)), arr))

class MyKMeans:
    def runKMeans(self, data, k):

        batch_size = 45
        centers = [[1,1], [-1,-1],[1,-1]]
        n_clusters = len(centers)
        X = np.array(data)
        # k = 2
        transformation = [[-0.6083459, -0.63667341] , [-0.40887718, 0.85253229]]
        X_aniso = np.dot(X, transformation)
        kmeans = KMeans(init='k-means++', n_clusters = k, n_init=20, algorithm='elkan')
        cluster_labels = kmeans.fit_predict(X)

        silhouette_avg = silhouette_score(X, cluster_labels)
        str_silhouette_avg = str(silhouette_avg) 
        with open('out.txt', 'a') as f:
            output = 'For n clusters: '+ str(k) + ' The average silhouette_score is: ' + str(silhouette_avg) +'\n'
            f.write(output)
            f.flush()
        centroids = kmeans.cluster_centers_
        labels = kmeans.labels_

        str_labels = '-'.join(arrayOfStrings(labels))
        str_centroids = '/'.join(list(map((lambda x: str(','.join(arrayOfStrings(x)))), centroids)))
        return (str_silhouette_avg + '@' + str_labels + '&' + str_centroids)

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

result = MyKMeans()
print(result.runKMeans(data, k))