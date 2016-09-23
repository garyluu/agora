#!/bin/bash

cd build 

chmod +x update-dockerfile.sh
bash update-dockerfile.sh

chmod +x build-images.sh
bash build-images.sh
