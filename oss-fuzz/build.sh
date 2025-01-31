#!/bin/bash

set -e

# so we can observe intermediates
cp -r netty netty-tcnative netty-incubator-transport-io_uring $WORK/
cd $WORK

pushd "netty"
#  ./mvnw install -Poss-fuzz -DskipTests -Dcheckstyle.skip -Drevapi.skip
popd

pushd "netty-tcnative"
#  ./mvnw install -pl openssl-dynamic -am -DskipTests -Dcheckstyle.skip -Drevapi.skip
popd

ls /usr/lib
env

pushd "netty-incubator-transport-io_uring"
  ./mvnw install -Poss-fuzz -DskipTests -Dcheckstyle.skip -Drevapi.skip
popd

export OSSFUZZ_NETTY_VERSION=$(cd netty && ./mvnw org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)
export OSSFUZZ_NETTY_TCNATIVE_VERSION=$(cd netty-tcnative && ./mvnw org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)
export OSSFUZZ_NETTY_IOURING_VERSION=$(cd netty-incubator-transport-io_uring && ./mvnw org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)

pushd "$OUT"
  unzip -j ~/.m2/repository/io/netty/netty-transport-native-epoll/$OSSFUZZ_NETTY_VERSION/netty-transport-native-epoll-$OSSFUZZ_NETTY_VERSION-linux-x86_64.jar META-INF/native/libnetty_transport_native_epoll_x86_64.so
  unzip -j ~/.m2/repository/io/netty/netty-tcnative/$OSSFUZZ_NETTY_TCNATIVE_VERSION/netty-tcnative-$OSSFUZZ_NETTY_TCNATIVE_VERSION-linux-x86_64.jar META-INF/native/libnetty_tcnative_linux_x86_64.so
  unzip -j ~/.m2/repository/io/netty/incubator/netty-incubator-transport-native-io_uring/$OSSFUZZ_NETTY_IOURING_VERSION/netty-incubator-transport-native-io_uring-$OSSFUZZ_NETTY_IOURING_VERSION-linux-x86_64.jar META-INF/native/libnetty_transport_native_io_uring_x86_64.so
popd

cd netty-fuzz-tests

./gradlew prepareClusterFuzz
