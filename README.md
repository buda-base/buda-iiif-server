# BUDA IIIF image server

The buda IIIF image server, based on [hymir](https://github.com/dbmdz/iiif-server-hymir).

### API

See [API.md](API.md) for a description of the API and the different ways to access an image.

## Configuration

Two profiles are available : `PROD` or `local`, they can be customized in `/src/main/resources/application.yml`.

### Configuring resolver for amazon S3 repository

Follow these instructions:

https://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html

### Configuration files and configuration path

A config path should be set to indicate where to find `iiifserv.properties` and `iiifserv-private.properties` (see templates in the root dir of this repo).

In addition, a server-specific Spring config is passed in the `application.yml` file (see template in the root dir of this repo).

### compile and run:

in the buda-iiif-server directory, run: 

```
mvn clean package
```

then using your favorite port and profile (here 8080 and "local"), run:

```
java -Dserver.port=8080 -Dspring.profiles.active=local -Diiifserv.configpath=/your/config/path/ -Dspring.config.location=file:/your/spring/config/pathFor/application.yml -jar target/buda-hymir-1.0.0-SNAPSHOT-exec.jar
```

### Note

You must implement `de.digitalcollections.core.backend.impl.file.repository.resource.resolver.S3Resolver` if you wish to use a different identifier format (see `Bdrcs3Resolver` for BDRC's identifer format).
