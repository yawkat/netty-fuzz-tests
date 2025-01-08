#!/bin/bash

set -e

apt-get install -y autoconf automake libtool make tar gcc

pushd "netty"
  ./mvnw install -DskipTests -Dcheckstyle.skip
popd

export OSSFUZZ_NETTY_VERSION=$(cd netty && ./mvnw org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)

cd netty-fuzz-tests

./gradlew prepareClusterFuzz
