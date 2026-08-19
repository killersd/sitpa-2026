# --- Estagio de build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /projeto

# As dependencias mudam muito menos que o codigo; resolve-las em uma camada propria
# faz os rebuilds seguintes reaproveitarem o cache.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Imagem final ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl serve ao HEALTHCHECK; a aplicacao nao precisa de root, porque o motor de regras
# compila o DRL em memoria e nao escreve nada em disco.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system sitpa \
    && useradd --system --gid sitpa --create-home sitpa

COPY --from=build /projeto/target/sitpa-backend-*.jar app.jar
RUN chown sitpa:sitpa app.jar

USER sitpa
EXPOSE 8090

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENV SPRING_PROFILES_ACTIVE=postgres

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD ["sh", "-c", "curl -fsS http://localhost:8090/actuator/health/readiness || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
