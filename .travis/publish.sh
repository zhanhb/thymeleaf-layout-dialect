#!/bin/bash

if git config --get remote.origin.url 2>&1 | grep -F https://github.com/zhanhb/thymeleaf-layout-dialect -q && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    mvn -Duser.name=zhanhb -DskipTests=true clean package source:jar javadoc:jar deploy -s .travis/settings.xml
    exit $?
fi

