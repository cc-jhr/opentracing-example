FROM openjdk:8

COPY target/reminder.jar /usr/local/bin
WORKDIR /usr/local/bin

CMD ["java", "-jar", "reminder.jar", "jaeger"]

EXPOSE 8081