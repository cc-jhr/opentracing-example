FROM openjdk:8

COPY target/reminder.jar /usr/local/bin
WORKDIR /usr/local/bin

CMD ["java", "-jar", "reminder.jar", "zipkin:9411"]

EXPOSE 8081