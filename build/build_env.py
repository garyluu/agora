import sys, os

PROJECT = 'agora'
BRANCH = os.environ['CIRCLE_BRANCH']
REPO = 'broadinstitute/' + PROJECT
ENV = 'dev'  # will need additional functionality with this