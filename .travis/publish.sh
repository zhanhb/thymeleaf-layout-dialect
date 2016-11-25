#!/bin/bash

if git config --get remote.origin.url 2>&1 | grep -F https://github.com/zhanhb/thymeleaf-layout-dialect -q && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    mvn -Duser.name=zhanhb -DskipTests=true clean deploy -s .travis/settings.xml $*
    if [ $? -ne 0 ];then exit $?;fi
fi

