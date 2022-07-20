#!/bin/bash -x

set -euo pipefail

VERSION=$1
echo "You want me to build and deploy ${VERSION} ??"

export MAVEN_HOME="$HOME/.sdkman/candidates/maven/current"
export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
export PATH="$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH"

export JENKINS_HOME=/tmp/jenkins-home
export RELEASE_TOOLS_CACHE=${JENKINS_HOME}/.m2/spring-data-release-tools
export LOGS_DIR=${JENKINS_HOME}/spring-data-shell/logs
export SETTINGS_XML=${JENKINS_HOME}/settings.xml

mkdir -p ${RELEASE_TOOLS_CACHE}
mkdir -p ${LOGS_DIR}

export GNUPGHOME=~/.gnupg/
mkdir -p ${GNUPGHOME}
chmod 700 ${GNUPGHOME}

cp ci/settings.xml ${JENKINS_HOME}

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
            -jar target/spring-data-release-cli.jar \
            --cmdfile target/prepare-and-build.shell
    }
else
    echo "You are running inside Jenkins! Using parameters fed from the agent."

    cp $KEYRING $GNUPGHOME

    ls -ld ~/.gnupg
    ls -lR ~/.gnupg
    id

    function spring-data-release-shell {
        java \
            -Dspring.profiles.active=jenkins \
            -Dmaven.home=${MAVEN_HOME} \
            -jar target/spring-data-release-cli.jar \
            --cmdfile target/prepare-and-build.shell
    }
fi

#SDKMAN_PLATFORM=linuxx64
#SDKMAN_CANDIDATES_API=https://api.sdkman.io/2
#SDKMAN_VERSION=5.15.0
#SDKMAN_DIR="$HOME/.sdkman"
#SDKMAN_CANDIDATES_DIR=$SDKMAN_DIR/candidates
#source "${SDKMAN_DIR}/bin/sdkman-init.sh"

echo "About to prepare and build ${VERSION}."

#gpg --list-secret-keys
#gpg --list-secret-keys
#gpg --list-keys
#ls -lR ${GNUPGHOME}

sed "s|\${VERSION}|${VERSION}|g" < ci/prepare-and-build.template > target/prepare-and-build.shell

spring-data-release-shell

ls -ld ~/.gnupg
ls -lR ~/.gnupg
id

