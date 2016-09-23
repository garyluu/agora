#!/bin/bash

set -eux

GIT_SHA=$(git rev-parse ${CIRCLE_BRANCH}:$PROJECT)

# Create new docker image
docker build -t $REPO:${GIT_SHA:0:12} $PROJECT

# Push the new image to DockerHub
docker push -f $REPO:${GIT_SHA:0:12}