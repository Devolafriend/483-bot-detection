import matplotlib.pyplot as plt
import os
import re
import shutil
import string
import sys
import tensorflow as tf


from tensorflow.keras import layers
from tensorflow.keras import losses
from keras.callbacks import EarlyStopping



batch_size = 32
seed = 42

max_features = 10000
sequence_length = 250

embedding_dim = 16
TOTAL_EPOCHS = 100

AUTOTUNE = tf.data.AUTOTUNE

def custom_standardization(data):
    print(data)
    lowercase = tf.strings.lower(data)
    # TODO: Remove stopwords
    print(lowercase)
    
    return tf.strings.regex_replace(lowercase, '[%s]' % re.escape(string.punctuation), '')



def format_dataset(raw_train_ds, raw_val_ds, raw_test_ds):

    train_ds = raw_train_ds.map(vectorize_text)
    val_ds = raw_val_ds.map(vectorize_text)
    test_ds = raw_test_ds.map(vectorize_text)


    train_ds = train_ds.cache().prefetch(buffer_size=AUTOTUNE)
    val_ds = val_ds.cache().prefetch(buffer_size=AUTOTUNE)
    test_ds = test_ds.cache().prefetch(buffer_size=AUTOTUNE)
    return train_ds, val_ds, test_ds

def create_model():
    model = tf.keras.Sequential([
    layers.Embedding(max_features + 1, embedding_dim),
    layers.Dropout(0.2),
    layers.GlobalAveragePooling1D(),
    layers.Dropout(0.2),
    layers.Dense(1)])
    model.summary()

    model.compile(loss=losses.BinaryCrossentropy(from_logits=True),
                optimizer='adam',
                metrics=tf.metrics.BinaryAccuracy(threshold=0.0))

    return model

def main(args):
    raw_train_ds, raw_val_ds, raw_test_ds = get_data()
    print("Label 0 corresponds to", raw_train_ds.class_names[0])
    print("Label 1 corresponds to", raw_train_ds.class_names[1])
    global vectorize_layer
    vectorize_layer = create_vectorize_layer()
    # Make a text-only dataset (without labels), then call adapt
    train_text = raw_train_ds.map(lambda x, y: x)
    vectorize_layer.adapt(train_text)
    
    train_ds, val_ds, test_ds = format_dataset(raw_train_ds, raw_val_ds, raw_test_ds)    

    model = create_model()


    early_stop = EarlyStopping(monitor='val_loss', mode="max", patience=10, restore_best_weights=True)

    print("Starting training")

    history = model.fit(
        train_ds,
        validation_data=val_ds,
        callbacks=[early_stop],
        epochs=TOTAL_EPOCHS)

    loss, accuracy = model.evaluate(test_ds)

    print("Loss: ", loss)
    print("Accuracy: ", accuracy)


    # TODO: Decide what to do with this

    history_dict = history.history
    history_dict.keys()

    acc = history_dict['binary_accuracy']
    val_acc = history_dict['val_binary_accuracy']
    loss = history_dict['loss']
    val_loss = history_dict['val_loss']

    epochs = range(1, len(acc) + 1)

    # "bo" is for "blue dot"
    plt.plot(epochs, loss, 'bo', label='Training loss')
    # b is for "solid blue line"
    plt.plot(epochs, val_loss, 'b', label='Validation loss')
    plt.title('Training and validation loss')
    plt.xlabel('Epochs')
    plt.ylabel('Loss')
    plt.legend()

    plt.show()

    plt.plot(epochs, acc, 'bo', label='Training acc')
    plt.plot(epochs, val_acc, 'b', label='Validation acc')
    plt.title('Training and validation accuracy')
    plt.xlabel('Epochs')
    plt.ylabel('Accuracy')
    plt.legend(loc='lower right')

    plt.show()

    export_model = tf.keras.Sequential([
    vectorize_layer,
    model,
    layers.Activation('sigmoid')
    ])

    export_model.compile(
        loss=losses.BinaryCrossentropy(from_logits=False), optimizer="adam", metrics=['accuracy']
    )





def get_data():
    raw_train_ds = tf.keras.preprocessing.text_dataset_from_directory(
        'data/train',
        batch_size=batch_size,
        validation_split=0.2,
        subset='training',
        seed=seed)

    raw_val_ds = tf.keras.preprocessing.text_dataset_from_directory(
        'data/train',
        batch_size=batch_size,
        validation_split=0.2,
        subset='validation',
        seed=seed)

    raw_test_ds = tf.keras.preprocessing.text_dataset_from_directory(
        'data/test',
        batch_size=batch_size)
    
    return raw_train_ds, raw_val_ds, raw_test_ds

def create_vectorize_layer():
    vectorize_layer = layers.TextVectorization(
        standardize=custom_standardization,
        max_tokens=max_features,
        output_mode='int',
        output_sequence_length=sequence_length)
    return vectorize_layer

def vectorize_text(text, label):
  text = tf.expand_dims(text, -1)
  return vectorize_layer(text), label


if __name__ == "__main__":
    main(sys.argv[1:])