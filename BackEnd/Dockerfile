# compilmos el fat jar con maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# descargamos dependencias primero (se cachea si pom.xml no cambia!!)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests
 
# imagen final ligera solo con el jar final
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]