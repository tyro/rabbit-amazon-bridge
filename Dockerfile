FROM openjdk:8-alpine3.9
MAINTAINER Tyro "open-source@tyro.com"

WORKDIR /code

RUN mkdir -p /app
COPY /target/rabbit-amazon-bridge-*-deployable.jar /app/rabbit-amazon-bridge.jar

EXPOSE 8080

ENTRYPOINT ["/usr/lib/jvm/java-1.8-openjdk/jre/bin/java", \
     "-Xms128m", \
     "-Xmx512m", \
     "-classpath", \
     "/app/resources:/app/extra-config:/app/rabbit-amazon-bridge.jar", \
     "org.springframework.boot.loader.JarLauncher"]
