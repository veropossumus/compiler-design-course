#!/usr/bin/env sh
BIN_DIR="$(dirname "$0")/build/install/compiler/bin"
$BIN_DIR/compiler test.md output.s
gcc output.s -o output
./output
echo $?

