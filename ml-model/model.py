from os import path

import keras
import numpy as np
import pyvips
from keras.applications.xception import Xception
from keras.layers import *
from keras.models import Model
from keras.utils import Sequence
from keras.utils import plot_model
from sklearn.preprocessing import OneHotEncoder


class PaintingModel:
    """
    Xception based model for classifying genre and style of paintings.
    """

    def __init__(self, genres, styles, model_weights=None):
        self.img_size = (300, 300)
        self.num_styles = len(styles)
        self.num_genres = len(genres)
        self.genres_encoder = OneHotEncoder().fit(np.array([genres]).T)
        self.styles_encoder = OneHotEncoder().fit(np.array([styles]).T)
        self._build(model_weights)
        self.img_loader = ImgLoader(self.img_size, ".")

    def _build(self, weights):
        base_model = Xception(include_top=False, weights='imagenet')
        x = base_model.layers[105].output
        block4_out = base_model.get_layer('block4_sepconv2_bn').output
        block4_avg = GlobalAveragePooling2D()(block4_out)
        self.block4_dense = Dense(512, name="block4_dense")(block4_avg)
        genre_predictions = self._create_block(x, self.num_genres, 'genre')
        style_predictions = self._create_block(x, self.num_styles, 'style')
        self.model = Model(inputs=base_model.input, outputs=[genre_predictions, style_predictions])
        if weights:
            self.model.load_weights(weights)

    def plot(self):
        plot_model(self.model, to_file='model.png')

    def _create_block(self, input_tensor, classes, out_name):
        x = SeparableConv2D(filters=728, kernel_size=(3, 3), strides=(1, 1), padding='same')(input_tensor)
        x = BatchNormalization()(x)
        x = Activation('relu')(x)
        x = Dropout(0.2)(x)
        x = SeparableConv2D(filters=728, kernel_size=(3, 3), strides=(1, 1), padding='same')(x)
        x = BatchNormalization()(x)
        x = Activation('relu')(x)
        x = Dropout(0.2)(x)
        x = SeparableConv2D(filters=728, kernel_size=(3, 3), strides=(1, 1), padding='same')(x)
        x = BatchNormalization()(x)
        x = keras.layers.add([x, input_tensor], name="add_{}".format(out_name))
        x = GlobalMaxPooling2D()(x)
        x = Dense(2048, activation='relu', name="{}_2048".format(out_name))(x)
        x = Dropout(0.2)(x)
        x = Dense(1024, activation='relu', name="{}_1024".format(out_name))(x)
        x = Dense(512, activation='relu', name="{}_512".format(out_name))(x)
        added = keras.layers.add([x, self.block4_dense], name="add_dense_{}".format(out_name))
        predictions = Dense(classes, activation='softmax', name=out_name)(added)
        return predictions

    def predict_on_batch(self, imgs):
        x = np.array([self.img_loader.get_resized_img(img) for img in imgs])
        genre_predictions, style_predictions = self.model.predict_on_batch(x)
        return {
            "style": self.styles_encoder.inverse_transform(style_predictions),
            "genre": self.genres_encoder.inverse_transform(genre_predictions)
        }

    def vectorize(self, imgs):
        """
        Gets a vector embedding from a batch of images

        :param imgs: images as numpy arrays fitting the input dimensions of the model
        :returns: numpy ndarray
        """
        vector_layers = ['genre_512', 'genre_1024', 'style_512', 'style_1024', 'block4_dense']
        intermediate_layer_model = Model(
            inputs=self.model.input,
            outputs=[self.model.get_layer(layer).output for layer in vector_layers]
        )
        batch = np.array(imgs)
        intermediate_outputs = intermediate_layer_model.predict(batch)
        return dict(zip(vector_layers, intermediate_outputs))

    def __str__(self):
        return "\n".join([layer.name for layer in self.model.layers])


class ImgLoader:
    """
    loads and resizes images using pyvips.
    """

    def __init__(self, dim, directory):
        self.dim = dim
        self.directory = directory
        self.format_to_dtype = {
            'uchar': np.uint8,
            'char': np.int8,
            'ushort': np.uint16,
            'short': np.int16,
            'uint': np.uint32,
            'int': np.int32,
            'float': np.float32,
            'double': np.float64,
            'complex': np.complex64,
            'dpcomplex': np.complex128,
        }

    def get_resized_img(self, img_name):
        return self._read_and_resize(img_name)

    def _read_and_resize(self, img_name):
        try:
            im = pyvips.Image.new_from_file(path.join(self.directory, img_name))
            vscale = self.dim[1] / im.height
            hscale = self.dim[0] / im.width
            resized = im.resize(hscale, kernel="nearest", vscale=vscale)
            img_arr = np.ndarray(
                buffer=resized.write_to_memory(),
                dtype=self.format_to_dtype[im.format],
                shape=[resized.height, resized.width, resized.bands]
            ) / 255.0
            if img_arr.shape[2] != 3:
                img_arr = np.repeat(img_arr, 3, axis=2)[:, :, :3]
            return img_arr
        except Exception as e:
            print("broken img ", img_name, e)
            return np.zeros(shape=[self.dim[0], self.dim[1], 3])


class PaintingSequence(Sequence):
    """
    Keras sequence used for training/validation/testing
    """

    def __init__(self, directory, x_set, y_set, batch_size, multi_out=False, dim=(400, 400)):
        self.directory = directory
        self.x, self.y = x_set, y_set
        self.batch_size = batch_size
        self.multi_out = multi_out
        self.dim = dim
        self.img_loader = ImgLoader(dim=dim, directory=directory)
        print("Y shape", np.array(self.y).shape)

    def _read_and_resize(self, img_name):
        return self.img_loader.get_resized_img(img_name)

    def __len__(self):
        return int(np.ceil(len(self.x) / float(self.batch_size)))

    def __getitem__(self, idx):
        idx0 = idx * self.batch_size
        idx1 = (idx + 1) * self.batch_size
        batch_x = self.x[idx0:idx1]
        if self.multi_out:
            batch_y = [[v for v in y[idx0:idx1]] for y in self.y]
        else:
            batch_y = [v for v in self.y[idx0:idx1]]
        resized = [self._read_and_resize(file_name) for file_name in batch_x]
        return np.array(resized), batch_y
