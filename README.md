# yaml-spicedb-converter
JAVA CLI for converting a yaml file to spicedb schema format and vice versa

# Setup
Firstly it is necessary to build the project so after cloning run

```
mvn package
```
in the root directory of the project. 
The jar built is a standalone jar file, so it can be moved and renamed as wished.

# Usage
## Encoding (yaml to spiceDB)

```
java -jar converter.jar --encode -c <context> <filename.yaml>
```
Where
* **context** -> the context that will be used for definitions
* **filename.yaml** the path to the file to be encoded into spicedb format

The result will be stored in the directory the jar was called from as filename_encoded.yaml

## Decoding (spiceDB to yaml)

```
java -jar converter.jar --decode <filename.yaml>
```
Where
* **filename.yaml** -> The name of the file to be decoded

The result will be stored in the directory the jar was called from as filename_decoded.yaml
