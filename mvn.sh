#!/bin/bash
# 统一的 Maven 启动脚本：固定使用本机已验证的 JDK8 与 Maven 3.8.8
export JAVA_HOME="$(cygpath -w /c/Program\ Files/Java/jdk1.8.0_212)"
export MAVEN_HOME="$(cygpath -w /c/maven-fresh/apache-maven-3.8.8)"
export PATH="$MAVEN_HOME/bin:$PATH"
exec "$MAVEN_HOME/bin/mvn.cmd" "$@"
