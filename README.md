# EDC - Blockchain Catalog

This repository provides a samples implementation of a blockchain based catalog for the Eclipse Dataspace Components (EDC). This extension is developed and maintained at the Chair for [Information System Engineering](https://www.tu.berlin/ise) at the Technische Universität Berlin in context of the [GAIA-X 4 Product Life Cycle - Arcoss Automated Driving](https://www.gaia-x4plcaad.info/) project.

## Run the Blockchain Catalog

> _The complete code can be found in `BlockchainCatalog`._

### Docker

Run the dockerized blockchain broker as a standalone container:

```docker
docker run -it --network host edc_blockchain_catalog
```

Create your own docker container / change the config:
1. Rebuild the BlockchainCatalog if needed: `./gradlew BlockchainCatalog:blockchain-catalog-prosumer:build`
2. Configure the Config file [config.properties](BlockchainCatalog/blockchain-catalog-prosumer/config.properties)
3. Rebuild the Docker Image: `docker build -t edc_blockchain_catalog .`

Apply custom configuration by adding the edc config paramters from the config.properties as environment parameters to the run command. Example:

`docker run -it --network host -e WEB_HTTP_IDS_PORT=8183 -e WEB_HTTP_IDS_PATH=/api/v2/ids edc_blockchain_catalog`


## Contributing

See [how to contribute](CONTRIBUTING.md).

## License

This project is licensed under the Apache License 2.0 - see [here](LICENSE) for details.
