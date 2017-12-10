import numpy as np
import matplotlib.pyplot as plt
from matplotlib import style

style.use("ggplot")

from sklearn.cluster import KMeans

class KMeans:
    def runKMeans(self, data, k):
        X = np.array([data])

        kmeans = KMeans(n_clusters=k)
        kmeans.fit(X)

        centroids = kmeans.cluster_centers_
        labels = kmeans.labels_

        print(centroids)
        print(labels)
    #Plot graffic 
    def plotGraphic(self, labels, X, centroids):
        colors = ["g.", "r."]

        for i in range(len(X)):
            print("coordinate:",X[i],labels[i])
            plt.plot(X[i][0],X[i][1], colors[labels[i]],markersize = 10)

        plt.scatter(centroids[:,0], centroids[:,1], marker = "x", s = 150, linewidths = 5, zorder = 10)
        plt.show()
