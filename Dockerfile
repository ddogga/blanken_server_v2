

#
# jar 파일은 이미지 안에서 빌드하지 않음 : 밖에서 빌드한 bootJar를 복사만 한다.
#   - CI(GitHub Actions) 는 Testcontainers 테스트를 러너에서 돌리므로 그 과정에서
#      이미 컴파일을 진행한다. 이미지 안에서 다시 빌드하면 컴파일을 두번하게 된다.
#
#
# 빌드:
#   ./gradlew bootJar
#   docker build -t blanken .
#   bootJar를 생략하면 build/libs에 남아 있는, 예전에 빌드된 jar가 들어간다.
#
# 실행 :
#   docker run -p 8080:8080 --env-file .env blanken
#


# jar를 레이어별 디렉터리로 풀기
FROM eclipse-temurin:21-jre AS extractor
WORKDIR /extract

# .dockerignore설정으로 x-plain.jar는 제외됨.
COPY build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted


FROM eclipse-temurin:21-jre

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

WORKDIR /app

COPY --from=extractor --chown=spring /extract/extracted/dependencies/ ./
COPY --from=extractor --chown=spring /extract/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring /extract/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring /extract/extracted/application/ ./

USER spring

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]