#!/bin/bash

chmod +x build/update-dockerfile.sh
bash build/update-dockerfile.sh

chmod +x build/build-images.sh
bash build/build-images.sh
