#!/bin/bash
set -e

./mvnw install -Dmaven.javadoc.skip=true
if [[ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ]]; then
    jdk_switcher use openjdk7
    ./mvnw -Djava.version=1.7 surefire:test
fi
