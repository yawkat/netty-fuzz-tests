#!/bin/bash

set -e

# TODO
git clone --depth=1 -b oss-fuzz https://github.com/yawkat/netty.git
git clone --depth=1 https://github.com/netty/netty-tcnative.git
git clone --depth=1 -b oss-fuzz https://github.com/yawkat/netty-incubator-transport-io_uring.git