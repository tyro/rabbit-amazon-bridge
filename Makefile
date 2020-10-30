install:
	./mvnw install -DskipTests=true -Dmaven.javadoc.skip=true -B -V

build-docker: build
	docker build -t tyro/rabbit-amazon-bridge:latest .

test-unit:
	./mvnw test -B -P unit-tests

test-integration:
	./mvnw verify -B

build: install test-unit test-integration

debug-build:
	docker run --rm -it tyro/rabbit-amazon-bridge-build:latest bash

clean-docker:
	-docker stop rabbit-amazon-bridge
	-docker rm rabbit-amazon-bridge
	-docker rmi rabbit-amazon-bridge

release:
	./mvnw release:prepare
