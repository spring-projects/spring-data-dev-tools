#!/bin/bash -x

set -euo pipefail

VERSION=$1
echo "You want me to conclude ${VERSION} ??"

export MAVEN_HOME="$HOME/.sdkman/candidates/maven/current"
export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
export PATH="$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH"

export JENKINS_HOME=/tmp/jenkins-home
export RELEASE_TOOLS_CACHE=${JENKINS_HOME}/.m2/spring-data-release-tools
export LOGS_DIR=${JENKINS_HOME}/spring-data-shell/logs
export SETTINGS_XML=${JENKINS_HOME}/settings.xml
export GNUPGHOME=~/.gnupg/

if test -f application-local.properties; then
    echo "You are running from dev environment! Using application-local.properties."

    GIT_BRANCH=""

    function spring-data-release-shell {
        java \
            "-Ddeployment.settings-xml=${SETTINGS_XML}" \
            "-Ddeployment.local=true" \
            "-Dmaven.mavenHome=${MAVEN_HOME}" \
            "-Dgpg.executable=/usr/bin/gpg" \
            "-Dio.workDir=dist" \
            -jar target/spring-data-release-cli.jar
    }
else
    echo "You are running inside Jenkins! Using parameters fed from the agent."

    \rm -rf ${GNUPGHOME}
    mkdir -p ${GNUPGHOME}
    chmod 700 ${GNUPGHOME}
    cp $KEYRING $GNUPGHOME

    function spring-data-release-shell {
        java \
            -Dspring.profiles.active=jenkins \
            -Dmaven.home=${MAVEN_HOME} \
            -jar target/spring-data-release-cli.jar
    }
fi

echo "About to conclude ${VERSION}."

echo "release conclude ${VERSION}" | spring-data-release-shell
