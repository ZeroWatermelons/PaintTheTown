import tensorflow as tf
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
import pandas as pd
import os
import cv2
import numpy as np

img_width, img_height = 256, 256
train_path = "processed_dataset/train/"
test_path = "processed_dataset/val/"

train = tf.keras.preprocessing.image.ImageDataGenerator(rescale=1/255)
test = tf.keras.preprocessing.image.ImageDataGenerator(rescale=1/255)

train_dataset = train.flow_from_directory("processed_dataset/train/",
                                          target_size=(img_width, img_height),
                                          batch_size=32,
                                          class_mode='binary')

test_dataset = test.flow_from_directory("processed_dataset/val/",
                                        target_size=(img_width, img_height),
                                        batch_size=32,
                                        class_mode='binary')


print(test_dataset.class_indices)

model = tf.keras.models.Sequential()

# Convolutional layer and maxpool layer 1
model.add(tf.keras.layers.Conv2D(32,(3,3),activation='relu',input_shape=(256,256,3)))
model.add(tf.keras.layers.MaxPool2D(2,2))

# Convolutional layer and maxpool layer 2
model.add(tf.keras.layers.Conv2D(64,(3,3),activation='relu'))
model.add(tf.keras.layers.MaxPool2D(2,2))

# Convolutional layer and maxpool layer 3
model.add(tf.keras.layers.Conv2D(128,(3,3),activation='relu'))
model.add(tf.keras.layers.MaxPool2D(2,2))

# Convolutional layer and maxpool layer 4
model.add(tf.keras.layers.Conv2D(128,(3,3),activation='relu'))
model.add(tf.keras.layers.MaxPool2D(2,2))

# This layer flattens the resulting image array to 1D array
model.add(tf.keras.layers.Flatten())

# Hidden layer with 512 neurons and Rectified Linear Unit activation function
model.add(tf.keras.layers.Dense(512,activation='relu'))

# Output layer with single neuron which gives 0 for Cat or 1 for Dog
#Here we use sigmoid activation function which makes our model output to lie between 0 and 1
model.add(tf.keras.layers.Dense(1, activation="sigmoid"))


model.summary()  # print summary
# compile model:
model.compile(optimizer='adam', loss='binary_crossentropy',
              metrics=['accuracy'])

early_stop = tf.keras.callbacks.EarlyStopping(
    monitor='val_loss', mode='min', verbose=1, patience=5)

history = model.fit(train_dataset, validation_data=test_dataset, epochs=30, callbacks=[early_stop], shuffle=True)

model.save('model_cropfrombottom.h5')
