# Rabbit Amazon Bridge
[![Download](https://maven-badges.herokuapp.com/maven-central/com.tyro.oss/rabbit-amazon-bridge/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.tyro.oss/rabbit-amazon-bridge)
[![Build Status](https://travis-ci.org/tyro/rabbit-amazon-bridge.svg?branch=master)](https://travis-ci.org/tyro/rabbit-amazon-bridge)

The rabbit-amazon-bridge is a service that helps us route rabbit messages from on-premise applications to and from services running in Amazon.

## Bridge Setup

To run rabbit-amazon-bridge:

```bash
java -Dspring.profiles.active=<your-profile(s)> \
     -classpath <path-to-your-properties-and-bridge-configs>:rabbit-amazon-bridge-0.10.0-deployable.jar org.springframework.boot.loader.JarLauncher
```


### Mandatory properties

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=<your rabbit mq user>
spring.rabbitmq.password=<your rabbit mq password>
spring.rabbitmq.virtual-host=/
cloud.aws.credentials.accessKey=<your aws secret key>
cloud.aws.credentials.secretKey=<your aws access key>
cloud.aws.region.static=us-east-1
bridge.config.location=classpath:bridge-config.json,file:/opt/rabbit-bridge/some-other-config.json

```
### The bridge configuration
Multiple bridge configuration files can be provided to the application using the `bridge.config.location` property.

See the example below for details of how they look. 
The transformationSpecs are based on the [Jolt](https://github.com/bazaarvoice/jolt) library.
We mostly for shifting (which we use to strip out sensitive fields) and/or field renaming operations but you can do more advanced data transformation. 
 

```json
[
  {
    "from" : {
      "rabbit": {
        "exchange": "exchange-name-1",
        "queueName": "queue-name-1",
        "routingKey": "routing-key-1"
      }
    },
    "transformationSpecs": [{
      "operation": "shift",
      "spec": {
        "fieldA": "fieldA",
        "fieldB": "fieldB"
      }
    }],
    "to" : {
      "sns": {
        "name":"rabbit-bridge-test-topic"
      }
    }
  },
  {
    "from" : {
      "rabbit": {
        "exchange": "exchange-name-2",
        "queueName": "queue-name-2",
        "routingKey": "routing-key-2"
      }
    },
    "transformationSpecs": [{
      "operation": "shift",
      "spec": {
        "fieldA": "fieldA",
        "fieldB": "fieldB"
      }
    }],
    "to" : {
      "sqs": {
        "name":"rabbit-bridge-test-queue"
      }
    }
  },
  {
    "from" : {
      "sqs": {
        "name":"rabbit-bridge-incoming-test-queue"
      }
    },
    "to" : {
      "rabbit": {
        "exchange": "exchange-name-3",
        "routingKey": "routing-key-3"
      }
    }
  }
]
```

#### Bridge Properties

field | description
-------|------------
from | The endpoint messages will be received from, currently SQS, or Rabbit
to | The endpoint to route messages to, current rabbit -> SQS or SNS, or SQS -> Rabbit
shouldForwardMessages | Tells the application not to configure the bridge, disabling it.

### Customised properties
We've added a few custom properties

#### Proxied comms to AWS
This may be useful for organisations who want all traffic between their on-premise artefacts and amazon to go through a proxy.

```properties
aws.proxy.host=<proxy-host>
aws.proxy.port=<proxy-port>
```

This is disabled by default

#### Default message id key for incoming messages
When messages come in from AWS on SQS or SNS they contain a 'messageId' that may be useful to some as a correlation id. 
This messageId can be added to the messages prior to publishing the to payload to rabbit. 
The format of this field is <queue-name>/messageId

```properties
default.incoming.message.id.key=globalMessageId
```

**Default**: null

#### Change your SQS endpoint url
This is used for working with elasticmq for local testing.  

```properties  
aws.sqs.endpoint.url=http://localhost:9324
aws.sqs.aws.region=localhost
```

### Additional useful Spring Properties
Rabbit amazon bridge is based on spring boot so you can use almost any property it supports using it's relaxed binding.
See [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) in the spring framework docs for more details.

#### Want to use SSL to talk to rabbit?
```properties
spring.rabbitmq.ssl.enabled=true
spring.rabbitmq.ssl.algorithm=SSLv3
spring.rabbitmq.ssl.key-store=file:/certs/keystore.jks
spring.rabbitmq.ssl.key-store-type=JKS
spring.rabbitmq.ssl.key-store-password=<keystore-pass>
spring.rabbitmq.ssl.trust-store=file:/certs/truststore.jks
spring.rabbitmq.ssl.trust-store-password=<truststore-pass>
```

#### Want to set your server ports?
```properties
server.port=8080
management.server.port=8083
```

#### Make your server run SSL
```properties
server.ssl.client-auth=need
server.ssl.key-store=file:/opt/tyro/ssl/rabbit-amazon-bridge.jks
server.ssl.trust-store=file:/opt/tyro/ssl/truststore.jks
```

#### More Settings
Refer to [Common application properties](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)
We will cheekily suggest applying common sense. We don't use hibernate in this project for example. 

# Development
## Regular local builds

Run 
```bash
./mvnw clean install
```

### Dependencies

* Docker
* Java 8 or higher 

## Docker builds

Run 
```bash
make build
```

### Explanation of key make targets

Target | Explanation
-------|------------
build-image | Creates a container for running builds in
test-unit | runs the unit tests against the latest build image
test-integration | runs the integration tests against the current build image
build | Runs build-image test-unit and test-integration in one go

### Dependencies

* Docker
* Linux or Mac (for the make commands) 

## Running a local services for manual testing
To start a local rabbit node and local elasticmq service for manual testing


### Rabbit
Run:
```bash
./docker-services-up.sh
```

Go to: [http://localhost:15672/](http://localhost:15672/)

username: guest
password: guest

### elastimq

Assuming you have the aws command lines tools installed:

To list queues 
```bash
aws --endpoint http://localhost:9324 sqs list-queues
```

To send a message
```bash
aws --endpoint http://localhost:9324 sqs send-message --queue-url http://localhost:9324/queue/rabbit-bridge-incoming-test-queue --message-body hello
```

To get a message
```bash
aws --endpoint http://localhost:9324 sqs receive-message --queue-url http://localhost:9324/queue/rabbit-bridge-incoming-test-queue
```

## Copyright and Licensing

Copyright (C) 2018 Tyro Payments Pty Ltd

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an AS IS BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.


