#!/bin/bash

set -e
cd agora
sbt package 
sbt clean compile test