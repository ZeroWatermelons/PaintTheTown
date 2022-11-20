from PIL import Image
from PIL import ImageOps
import PIL
from pathlib import Path
import os
import pandas as pd

def preprocess_images(width, height, input_folder, output_folder):
    df = pd.read_csv(input_folder + "labels.csv")
    dataframe = df.astype({'image_id': 'string'})
    for image_name in os.listdir(input_folder):
        name, ext = os.path.splitext(image_name)
        if ext == '.jpg':
            img = Image.open(input_folder + image_name)
            resized_img = PIL.ImageOps.fit(img, (width, height), method=0, bleed=0.0, centering=(0.5, 1.0))
            if dataframe[dataframe.values == image_name[:-4]]["highway"].item() == "primary":
                resized_img.save(output_folder + "primary/" + str(image_name), "JPEG")
            else:
                resized_img.save(output_folder + "footway/" + str(image_name), "JPEG")


width = 256
height = 256

preprocess_images(width, height, "hackatum_dataset/train/", "processed_dataset/train/")
preprocess_images(width, height, "hackatum_dataset/val/", "processed_dataset/val/")


