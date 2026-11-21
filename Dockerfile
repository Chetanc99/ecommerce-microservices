# Build any backend module with:
#   docker build --build-arg MODULE=ecommerce-order-service -t ecommerce/order-service .
#
# All services share this Dockerfile so the build logic stays in one place.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .

ARG MODULE
RUN mvn -B -pl "${MODULE}" -am -DskipTests package


FROM eclipse-temurin:21-jre-alpine

ARG MODULE
ENV MODULE=${MODULE}

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

# Only the executable Spring Boot jar matches this pattern; *.jar.original does not.
COPY --from=build /workspace/${MODULE}/target/*.jar /app/

RUN chown -R app:app /app

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/*.jar"]
