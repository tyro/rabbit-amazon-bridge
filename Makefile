build-image:
	docker build --tag tyro/rabbit-amazon-bridge-build -f Dockerfile.build .

deploy-image:
	docker build -f Dockerfile.deploy -t tyro/rabbit-amazon-bridge:latest . \
	--build-arg BUILD_IMAGE=tyro/rabbit-amazon-bridge-build:latest

test-unit:
	export DOCKER_BUILD_IMAGE=tyro/rabbit-amazon-bridge-build:latest && \
	docker-compose -f docker-compose-test-unit.yml up --exit-code-from sut

test-integration:
	export DOCKER_BUILD_IMAGE=tyro/rabbit-amazon-bridge-build:latest && \
	docker-compose -f docker-compose-test-integration.yml up --exit-code-from sut

build: build-image test-unit test-integration

debug-build:
	docker run --rm -it tyro/rabbit-amazon-bridge-build:latest bash

clean-docker:
	-docker stop rabbit-amazon-bridge
	-docker rm rabbit-amazon-bridge
	-docker rmi rabbit-amazon-bridge
