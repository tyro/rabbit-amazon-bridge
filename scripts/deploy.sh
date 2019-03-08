#!/usr/bin/env bash
TAG=test-deploy-branch
REPO=tyro/rabbit-amazon-bridge

echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
docker build -f Dockerfile.deploy -t $REPO:$TRAVIS_COMMIT . --build-arg BUILD_IMAGE=tyro/rabbit-amazon-bridge-build:latest
docker tag $REPO:$TRAVIS_COMMIT $REPO:$TAG
docker tag $REPO:$TRAVIS_COMMIT $REPO:travis-$TRAVIS_BUILD_NUMBER
docker push $REPO