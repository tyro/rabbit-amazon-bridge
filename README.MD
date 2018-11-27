# Rabbit Amazon Bridge

The rabbit-amazon-bridge is a general purpose service that helps us route rabbit messages from on-premise applications to services running in Amazon.

## Bridge configuration

For now, add a configuration file to the project. 

```json
[
  {
    "from" : {
      "rabbit": {
        "exchange": "exchange-name-1",
        "queueName": "queue-name-1",
        "routingKey": "routing-key-1",
        "transformationSpecs": [{
          "operation": "shift",
          "spec": {
            "fieldA": "fieldA",
            "fieldB": "fieldB"
          }
        }]
      }
    },
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
        "routingKey": "routing-key-2",
        "transformationSpecs": [{
          "operation": "shift",
          "spec": {
            "fieldC": "fieldC",
            "fieldB": {
              "bid" : "bid"
            }
          }
        }]
      }
    },
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

# Development
## Regular builds

Run `maven clean install`

## Running a local Rabbit
To run a single simple docker node for testing purposes:
```
./docker-rabbit-up.sh
```
username: guest
password: guest

## Copyright and Licensing

Copyright (C) 2018 Tyro Payments Pty Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.


