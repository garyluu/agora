#!/bin/bash

#GIT_SHA=$(git rev-parse ${BRANCH})

docker pull $REPO:${GIT_SHA:0:12}
docker tag $REPO:${GIT_SHA:0:12} $REPO:$ENV
docker push $REPO:$ENV