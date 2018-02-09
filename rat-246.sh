#!/bin/bash

set -e

# cleanup
# find ~/.m2/repository/org/apache/maven/ -type d -name "*-SNAPSHOT" | xargs rm -rf

cd wagon-providers/wagon-scm
touch .someignoredfile

if git add .someignoredfile 2>/dev/null; then
  >&2 echo .someignoredfile should be ignored by git
  exit 1
fi

mvn apache-rat:check
