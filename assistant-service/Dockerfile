FROM openjdk:8

COPY target/assistant.jar /usr/local/bin
WORKDIR /usr/local/bin

CMD ["java", "-jar", "assistant.jar", "http://reminder:8081", "http://notes:8082"]

EXPOSE 8080