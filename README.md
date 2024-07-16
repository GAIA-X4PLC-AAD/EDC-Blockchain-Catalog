# EDC Connector with Blockchain Support

This repository provides a samples implementation of a blockchain based catalog for the Eclipse Dataspace Components (EDC). This extension is developed and maintained at the Chair for [Information System Engineering](https://www.tu.berlin/ise) at the Technische Universität Berlin in context of the [GAIA-X 4 Product Life Cycle - Arcoss Automated Driving](https://www.gaia-x4plcaad.info/) project.

> This repository is just one part of the infrastructure. To run the complete infrastructure, please refer to the [Main Repository](https://github.com/GAIA-X4PLC-AAD/edc-blockchain-broker)

This Repository holds the source code for the following components:
- blockchain-logger (logs agreement and transfer events to the blockchain)
- blockchain-catalog-api (provide a `/blockchaincatalog` endpoint to query the blockchain and retrieve a DCAT Catalog)
- blockchain-catalog-listener (listen to edc specific events and requests and trigger the [EDC-Blockchain-Interface](https://github.com/GAIA-X4PLC-AAD/EDC-Blockchain-Interface) to store and retrieve data on the blockchain)
- transfer/http-push (is used for transfer demonstration purposes) 

## Create Docker Images
> Note: To deploy and use this Connector, please use the docker compose file in the [Main Repository](https://github.com/GAIA-X4PLC-AAD/edc-blockchain-broker)

Execute in the root directory of this repository:

```bash
docker build -f launchers/edc-tu-berlin/Dockerfile_consumer .
```
```bash
docker build -f launchers/edc-tu-berlin/Dockerfile_provider .
```
```bash
docker build -f launchers/push-http-backend/Dockerfile .
```

## Contributing

See [how to contribute](CONTRIBUTING.md).

## License

This project is licensed under the Apache License 2.0 - see [here](LICENSE) for details.


```
java -Dedc.fs.config=BlockchainCatalog/blockchain-catalog-prosumer/config.properties -Dedc.keystore=transfer/transfer-07-provider-push-http/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=BlockchainCatalog/blockchain-catalog-prosumer/provider-vault.properties -jar BlockchainCatalog/blockchain-catalog-prosumer/build/libs/consumer.jar
```