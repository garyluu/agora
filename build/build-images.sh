#!/bin/bash

set -eux

GIT_SHA=$(git rev-parse ${BRANCH})

echo "LABEL org.broadinstitute.$PROJECT.git-sha=$GIT_SHA" >> Dockerfile

# Create new docker image
docker build -t $REPO:${GIT_SHA:0:12} .

# Push the new image to DockerHub
docker push $REPO:${GIT_SHA:0:12}
