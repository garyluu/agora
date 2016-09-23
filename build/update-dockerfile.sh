#!/bin/bash

set -eux

#git submodule update --init --force $PROJECT
#
## Add git sha as label
#GIT_SHA=$(git rev-parse ${CIRCLE_BRANCH}:$PROJECT)
#(cd $PROJECT \
#  && git checkout -- Dockerfile \
#  &&


echo "LABEL org.broadinstitute.$PROJECT.git-sha=$GIT_SHA" >> Dockerfile