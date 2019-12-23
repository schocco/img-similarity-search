import pandas as pd
import sys
from confluent_kafka import avro
from confluent_kafka.avro import AvroProducer
import numpy as np

from model import PaintingModel, ImgLoader


def batched(batch_size, iterable):
    batch = []
    for item in iterable:
        batch.append(item)
        if len(batch) == batch_size:
            yield batch
            batch = []
    if len(batch) > 0:
        yield batch


def read_categories(path):
    with open(path, 'r') as f:
        categories = f.readlines()
    return [c.strip() for c in categories if c.strip() != '']


def main(img_path, img_csv):
    model = PaintingModel(genres=read_categories('genres.txt'), styles=read_categories('styles.txt'), model_weights='./weights.h5')
    kafka_client = KafkaClient()
    img_loader = ImgLoader(dim=model.img_size, directory=img_path)
    df = pd.read_csv(img_csv)

    for indexes in batched(16, df.index):
        df_batch = df.loc[indexes]
        imgs = [img_loader.get_resized_img(file) for file in df_batch['filename'].values]
        vectors = model.vectorize(imgs)
        for k, v in vectors.items():
            df_batch[k] = pd.Series(tuple(vector) for vector in v)
        for index, row in df_batch.iterrows():
            vector_features = {
                "usedModel": 'v1',
                "genre512": row['genre_512'],
                "genre1024": row['genre_1024'],
                "style512": row['style_512'],
                "style1024": row['style_1024'],
                "common1024": row['block4_dense']
            }
            image = VectorizedImage(filename=row['filename'],
                                    genre=row['genre'],
                                    title=row['title'],
                                    date=row['date'],
                                    style=row['style'],
                                    artist=row['artist'],
                                    sha1sum='',
                                    vector_features=vector_features)
            kafka_client.submit(image)
        kafka_client.flush()


class KafkaClient:
    """
    client for publishing vectorization results to kafka
    """

    def __init__(self,
                 schema_registry='http://127.0.0.1:8081',
                 bootstrap_servers='localhost:9092',
                 topic='paintings'):
        self.painting_schema = avro.load('../avro/painting.avsc')
        self.painting_key_schema = avro.load('../avro/painting.key.avsc')
        self.topic = topic
        self.avro_producer = AvroProducer({
            'bootstrap.servers': bootstrap_servers,
            'schema.registry.url': schema_registry,
            'default.topic.config': {'acks': 'all'}},
            default_value_schema=self.painting_schema,
            default_key_schema=self.painting_key_schema)

    def submit(self, vectorized_img):
        if isinstance(vectorized_img, VectorizedImage):
            value = vectorized_img.to_dict()
            self.avro_producer.produce(topic=self.topic,
                                       key={'filename': vectorized_img.filename},
                                       value=value)
        else:
            raise Exception("vectorized image must be an instances of " + VectorizedImage.__name__)

    def flush(self):
        self.avro_producer.flush()


class VectorizedImage:

    def __init__(self, filename, genre, title, date, style, artist, sha1sum, vector_features):
        self.filename = filename
        self.genre = genre
        self.title = title
        self.date = self.year_or_none(date)
        self.style = style
        self.artist = artist
        self.sha1sum = sha1sum
        self.vector_features = vector_features

    def year_or_none(self, val):
        try:
            if np.isnan(float(val)):
                return None
            else:
                return str(round(float(val)))
        except ValueError:
            return None

    def to_dict(self):
        return {
            "filename": self.filename,
            "genre": self.genre,
            "title": self.title,
            "date": self.date,
            "style": self.style,
            "artist": self.artist,
            "sha1sum": self.sha1sum,
            "vectorFeatures": self.vector_features,
        }


if __name__ == "__main__":
    img_path, csv_path = sys.argv[1:]
    main(img_path, csv_path)
