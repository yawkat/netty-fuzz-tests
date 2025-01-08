#!/bin/bash

set -e

pushd "netty"
  mvn install -DskipTests -Dcheckstyle.skip
popd

export OSSFUZZ_NETTY_VERSION=$(cd micronaut-core && mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)

cd netty-fuzz-tests

./gradlew prepareClusterFuzz
