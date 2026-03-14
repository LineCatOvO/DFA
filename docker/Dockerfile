# DFA Android Build Environment
# Based on Ubuntu with Android SDK and Gradle

FROM ubuntu:22.04

LABEL maintainer="DFA Team"
LABEL description="Android build environment for DFA project"
LABEL version="1.0.0"

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_HOME=/opt/gradle
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/emulator:${GRADLE_HOME}/bin"

WORKDIR /workspace

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk \
    wget \
    curl \
    git \
    unzip \
    build-essential \
    libssl-dev \
    libffi-dev \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && cd ${ANDROID_SDK_ROOT}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip \
    && unzip -q cmdline-tools.zip \
    && mv cmdline-tools latest \
    && rm cmdline-tools.zip

RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true

RUN sdkmanager --update

RUN sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "platforms;android-33" \
    "build-tools;34.0.0" \
    "build-tools;33.0.2" \
    "emulator" \
    "system-images;android-34;google_apis;x86_64" \
    "system-images;android-33;google_apis;x86_64"

RUN wget -q https://services.gradle.org/distributions/gradle-8.4-bin.zip -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && mv /opt/gradle-8.4 ${GRADLE_HOME} \
    && rm /tmp/gradle.zip

RUN gradle --version

RUN mkdir -p ~/.gradle \
    && echo "org.gradle.daemon=false" >> ~/.gradle/gradle.properties \
    && echo "org.gradle.parallel=true" >> ~/.gradle/gradle.properties \
    && echo "org.gradle.caching=true" >> ~/.gradle/gradle.properties \
    && echo "org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError" >> ~/.gradle/gradle.properties

RUN apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /tmp/* \
    && rm -rf /var/tmp/*

RUN mkdir -p /workspace

WORKDIR /workspace

ENTRYPOINT ["/bin/bash"]
