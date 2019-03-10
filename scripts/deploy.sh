#!/usr/bin/env bash
TAG=test-deploy-branch
REPO=tyro/rabbit-amazon-bridge
TAG=$1

echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
docker build -f Dockerfile.deploy -t $REPO:$TAG . --build-arg BUILD_IMAGE=tyro/rabbit-amazon-bridge-build:latest
docker push $REPO