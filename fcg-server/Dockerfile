FROM amazoncorretto:17-alpine
WORKDIR /app
COPY target/fcg-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar", "--spring.profiles.active=prod"]
