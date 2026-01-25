FROM eclipse-temurin:21-jdk
WORKDIR /app
ARG JAR=build/libs/*.jar
COPY ${JAR} app.jar
EXPOSE 7030
ENTRYPOINT ["java","-jar","/app/app.jar"]
