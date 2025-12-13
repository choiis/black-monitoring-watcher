FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR=build/libs/*.jar
COPY ${JAR} app.jar
EXPOSE 7020
ENTRYPOINT ["java","-jar","/app/app.jar"]
