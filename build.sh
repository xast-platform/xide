#!/usr/bin/env bash
set -e
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
JVM_DIR="$BUILD_DIR/jvm"
mkdir -p "$JVM_DIR"
cd "$ROOT_DIR"
mvn clean package -DskipTests
cp xide-app/target/xide-app-*-jar-with-dependencies.jar "$JVM_DIR/xide-app.jar"
echo "JVM jar: $JVM_DIR/xide-app.jar"
