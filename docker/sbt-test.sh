#!/bin/bash

set -e
cd agora
sbt test assembly
cd .. 
cp agora/target/scala-2.11/agora-0.1-SNAPSHOT.jar ./