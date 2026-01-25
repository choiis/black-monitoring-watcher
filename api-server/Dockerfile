FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR=build/libs/*.jar
COPY ${JAR} app.jar
EXPOSE 7080
ENTRYPOINT ["java","-jar","/app/app.jar"]
