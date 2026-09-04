#!/bin/bash
# 统一构建脚本（JDK8 + Maven 3.8.8）
export JAVA_HOME="$(cygpath -w /c/Program\ Files/Java/jdk1.8.0_212)"
export MAVEN_HOME="$(cygpath -w /c/maven-fresh/apache-maven-3.8.8)"
export PATH="$MAVEN_HOME/bin:$PATH"
cd "C:/Users/Fan/WorkBuddy/机器人之家/robot-home/robot-home-server" || exit 1
exec "$MAVEN_HOME/bin/mvn.cmd" "$@"
