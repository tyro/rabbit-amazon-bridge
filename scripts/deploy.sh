#!/usr/bin/env bash
REPO=tyro/rabbit-amazon-bridge
TAG=$1

echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
docker build -t $REPO:"$TAG" .
docker push $REPO
