# img-similarity-search
POC for similarity search by abstract features

## Setup

1. Download paintings-by-numbers from kaggle
2. Create a volume with the downloaded paintings  `docker volume create --opt device=~/Downloads/art-classification/painter-by-numbers/train --opt o=bind paintings`
3. create a topic for the images:
   ```
   docker exec -it kafka /bin/bash
   kafka-topics --create --zookeeper zookeeper:2181 --replication-factor 1 --partitions 2 --topic paintings
   ```
4. Create the Elastic Connector for the paintings topic
   ```bash
   curl -X POST -H "Content-Type: application/json" -d @paintings.connector.json localhost:8083/connectors
   ```
5. Check the status with `curl http://localhost:8083/connectors/elastic-paintings-connector/tasks/0/status`
6. Create the schema mapping for the Elasticsearch index with
   ```
   curl -X PUT "localhost:9200/paintings?pretty" -H 'Content-Type: application/json' -d @paintings.mapping.json
   ```
7. Run the python script to process all images and submit image representations in kafka  
   The sink connector will ensure that processed records are store din the previously created elasticsearch index
   
8. Open the website and browser the paintings
