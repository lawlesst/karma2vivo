#!/usr/bin/env bash
echo $1

export VIVO_EMAIL="vivo_root@school.edu"
export VIVO_PASSWORD=""
export VIVO_API="http://localhost:8080/vivo/api/"
export DATA_NAMESPACE="http://vivo.school.edu/individual/"

# Karma home directory
export KARMA_USER_HOME="/home/user/karma"

# Increase memory if desired.
#export MAVEN_OPTS="-Xmx4096m -XX:MaxPermSize=128m"

mvn exec:java -X -Dexec.mainClass="com.github.lawlesst.karma2vivo.Batch" -Dexec.args="-config $1"

