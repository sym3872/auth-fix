#!/bin/bash

# 이 프로젝트의 Spring Boot 메인 클래스 이름이다.
PROJECT_MARKER="com.example.springblogapi.SpringBlogApiApplication"

# application.yml에서 사용하는 서버 포트다.
SERVER_PORT="8080"

# 현재 8080 포트에서 기다리고 있는 프로세스 번호를 찾는다.
LISTENING_PIDS="$(lsof -tiTCP:${SERVER_PORT} -sTCP:LISTEN 2>/dev/null)"

if [ -n "${LISTENING_PIDS}" ]; then
    for PROCESS_ID in ${LISTENING_PIDS}; do
        # 프로세스의 전체 실행 명령을 읽어 SecureBlog 서버인지 확인한다.
        PROCESS_COMMAND="$(ps -p "${PROCESS_ID}" -o command= 2>/dev/null)"

        if [[ "${PROCESS_COMMAND}" == *"${PROJECT_MARKER}"* ]]; then
            echo "이전에 실행된 SecureBlog 서버를 먼저 종료합니다."

            # 다른 프로그램은 건드리지 않고 확인된 이전 SecureBlog 서버만 종료한다.
            kill "${PROCESS_ID}"

            # 포트가 완전히 정리될 때까지 최대 약 3초간 기다린다.
            for WAIT_COUNT in {1..15}; do
                if ! kill -0 "${PROCESS_ID}" 2>/dev/null; then
                    break
                fi
                sleep 0.2
            done
        else
            echo "실행할 수 없습니다: 다른 프로그램이 8080 포트를 사용 중입니다."
            echo "그 프로그램을 종료한 뒤 다시 실행해 주세요."
            exit 1
        fi
    done
fi

# 이전 서버 정리가 끝나면 Gradle 진행률을 숨기고 새 서버와 메뉴를 실행한다.
exec ./gradlew --quiet --console=plain bootRun
