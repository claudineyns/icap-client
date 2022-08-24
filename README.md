# icap-client

Simple Java ICAP Client for ICAP Services.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them.

```
Java JDK 1.7 or higher.
```

### Installing

Just clone this project on your favorite Java-Compatible IDE and have fun. 

### Examples

In this examples we are assuming that you are running an ICAP server on `127.0.0.1:1344`. To run an ICAP server please see [icap-server](https://github.com/claudineyns/icap-server).

#### Send OPTIONS

````
ICAPClient icapClient = new ICAPClient("127.0.0.1", 1344);
icapClient.options("virus_scan");
````
This request will send the following bytes:
````
OPTIONS icap://127.0.0.1/virus_scan ICAP/1.0
Host: 127.0.0.1
User-Agent: Java-ICAP-Client/1.1
Encapsulated: null-body=0
````

#### Send REQMOD

````
ICAPClient icapClient = new ICAPClient("127.0.0.1", 1344);
ICAPRequest icapRequest = new ICAPRequest("virus_scan", ICAPRequest.Mode.REQMOD);
icapClient.execute(icapRequest);
````
This request will send the following bytes:
````
REQMOD icap://127.0.0.1/virus_scan ICAP/1.0
Host: 127.0.0.1
User-Agent: Java-ICAP-Client/1.1
Allow: 204
Encapsulated: null-body=0
````

#### Send RESPMOD

````
ICAPClient icapClient = new ICAPClient("127.0.0.1", 1344);
ICAPRequest icapRequest = new ICAPRequest("virus_scan", ICAPRequest.Mode.RESPMOD);
icapClient.execute(icapRequest);
````
This request will send the following bytes:
````
RESPMOD icap://127.0.0.1/virus_scan ICAP/1.0
Host: 127.0.0.1
User-Agent: Java-ICAP-Client/1.1
Allow: 204
Encapsulated: null-body=0
````

## Deployment

.

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

.

## Versioning

This project uses [SemVer](http://semver.org/) for versioning.

## Authors

* **Claudiney Nascimento** - *Initial work* - [claudineyns](https://github.com/claudineyns)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* [RFC3507](https://www.ietf.org/rfc/rfc3507.txt)
