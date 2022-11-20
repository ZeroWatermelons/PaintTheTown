import numpy as np
import tensorflow as tf
from PIL import Image
from PIL import ImageOps
import PIL
from pathlib import Path
import os

#classify an image. 'footway': 0, 'primary': 1
def classify(image_path):
    img = Image.open(image_path)
    img = PIL.ImageOps.fit(img, (256, 256), method=0, bleed=0.0, centering=(0.5, 0.5))
    img_array = np.array(img).reshape(1,256,256,3)
    model = tf.keras.models.load_model('model.h5')
    return int(model.predict(img_array) + 0.5)

print(classify("hackatum_dataset/val/165180662274601.jpg"))