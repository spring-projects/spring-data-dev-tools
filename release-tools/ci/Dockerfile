FROM ubuntu:22.10

ARG USER_UID="1001"
ARG USER_GID="1001"
ARG USER_NAME="jenkins"

RUN groupadd -g $USER_GID $USER_NAME && \
	useradd -m -g $USER_GID -u $USER_UID $USER_NAME

RUN apt-get update &&  \
    apt-get -y install curl zip gnupg
RUN rm -rf /var/lib/apt/lists/* &&  \
    rm -rf /tmp/*

USER $USER_UID:$USER_GID

RUN curl -s "https://get.sdkman.io" | bash

RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && \
    yes | sdk install java 17.0.2-tem && \
    yes | sdk install java 17.0.3-tem && \
    yes | sdk install java 8.0.322-tem && \
    yes | sdk install java 8.0.332-tem && \
    yes | sdk install maven && \
    rm -rf $HOME/.sdkman/archives/* && \
    rm -rf $HOME/.sdkman/tmp/*"

ENV MAVEN_HOME="$HOME/.sdkman/candidates/maven/current"
ENV JAVA_HOME="$HOME/.sdkman/candidates/java/current"
ENV PATH="$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH"
