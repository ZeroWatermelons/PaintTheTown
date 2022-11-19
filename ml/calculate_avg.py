# TensorFlow and tf.keras
import os.path

import tensorflow as tf
import numpy as np

if __name__ == "__main__":
    PATH = os.path.expanduser("~/hackatum_dataset/val")
    filecount = 0
    ratioSum = 0
    for fn in os.listdir(PATH):
        if fn.endswith(".jpg"):
            img = tf.keras.utils.load_img(f"{PATH}/{fn}")
            ratio = img.width / img.height
            ratioSum += ratio
            filecount += 1

    avg = ratioSum / filecount
    print("avg ratio:", avg, "... this means for a width of 1920px youd get", 1920/avg, "px height")

