
# 첫번째 스테이지 -> 빌드 영역
# 도커파일로 gradle build 진행할 때는 jdk-slim 사용하세요!
FROM openjdk:17-jdk-slim AS build

# 소스코드 복사
WORKDIR /app
COPY . /app

# gradle wrapper로 빌드
RUN chmod +x ./gradlew
RUN ./gradlew clean build

# 새로운 스테이지 -> 실행 영역
FROM openjdk:17-jdk-slim

# build라는 별칭으로 이루어진 스테이지에서 *.jar 파일을 app.jar로 복사해서 이미지에 세팅.
COPY --from=build /app/build/libs/*.jar app.jar

# CMD는 기본 실행 명령어를 의미. 컨테이너 실행 시에 다른 명령어가 주어지면 그 명령어로 대체됨.
# ENTRYPOINT는 반드시 실행되어야 할 명령어를 의미. 다른 명령어로 대체되지 않음.
# 스프링 부트는 무조건 -jar 옵션으로 실행되어야 하기에 ENTRYPOINT로 안전하게 선언.
ENTRYPOINT ["java", "-jar", "app.jar"]











