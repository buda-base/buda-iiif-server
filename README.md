# buda-iiif-server
the buda Image server based on hymir iiif-server

## Set up (using maven)
clone the buda-iiif-server repository

## Configuration

Two profiles are available : PROD or local and editables in buda-iiif-server/src/main/resources/application.yml

### Configuring resolver for amazon S3 repository

Follow these instructions:

https://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html

### compile and run:

in the buda-iiif-server directory, run: 

```
mvn clean package
```
then using your favorite port and profile (here 8080 and "local"), run:
```
java -Dserver.port=8080 -Dspring.profiles.active=local -jar target/buda-hymir-1.0.0-SNAPSHOT-exec.jar
```

### BDRC iiif compliant ("{identifier}/{region}/{size}/{rotation}/{quality}.{format}") url example :

http://localhost:8080/bdr:V22084_I0886::08860035.tif/full/full/0/default.jpg

### Note

You must implement de.digitalcollections.core.backend.impl.file.repository.resource.resolver.S3Resolver if you wish to use a different identifier format (see Bdrcs3Resolver as an example)




