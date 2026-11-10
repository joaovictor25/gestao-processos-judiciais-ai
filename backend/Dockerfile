# syntax=docker/dockerfile:1.7
# Back-end ProSint: Java 25 + Spring Boot 4.1.1, perfil prod, usuario nao root.
# Os testes rodam no CI e localmente (./mvnw test); a imagem apenas empacota.

FROM eclipse-temurin:25.0.4.1_1-jdk-noble AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -q -DskipTests package \
 && cp target/backend-*.jar /build/app.jar \
 && java -Djarmode=tools -jar /build/app.jar extract --layers --launcher --destination /build/extraido

FROM eclipse-temurin:25.0.4.1_1-jre-noble
# curl apenas para o healthcheck local do conteiner.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 prosint \
 && useradd --system --uid 10001 --gid prosint --no-create-home --shell /usr/sbin/nologin prosint
WORKDIR /app
# Camadas do Spring Boot: dependencias mudam menos que o codigo da aplicacao.
COPY --from=build --chown=prosint:prosint /build/extraido/dependencies/ ./
COPY --from=build --chown=prosint:prosint /build/extraido/spring-boot-loader/ ./
COPY --from=build --chown=prosint:prosint /build/extraido/snapshot-dependencies/ ./
COPY --from=build --chown=prosint:prosint /build/extraido/application/ ./
USER 10001:10001

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=25 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom -Dfile.encoding=UTF-8"
EXPOSE 8080

# Sem actuator: 401 em /api/auth/me prova que Tomcat, Spring Security e o contexto (incluindo
# Flyway e a validacao Hibernate, executados na inicializacao) estao de pe.
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=5 \
  CMD test "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/auth/me)" = "401" || exit 1

# Forma exec: a JVM recebe SIGTERM diretamente e aplica o shutdown gracioso (server.shutdown=graceful).
ENTRYPOINT ["java", "-cp", "/app", "org.springframework.boot.loader.launch.JarLauncher"]
