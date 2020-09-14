#!/bin/bash
set -e

. /opt/jdk_switcher/jdk_switcher.sh

./mvnw install -Dmaven.javadoc.skip=true
if [[ "$TRAVIS_JDK_VERSION" == "openjdk8" ]]; then
    jdk_switcher use openjdk7
    ./mvnw -Djava.version=1.7 surefire:test
fi
