.DEFAULT_GOAL := build

frontend:
	npm ci
	npx build-frontend

setup: frontend
	./gradlew build

build:
	./gradlew clean build

run:
	./gradlew bootRun

test:
	./gradlew test

format:
	./gradlew spotlessApply

lint:
	./gradlew spotlessCheck

.PHONY: frontend setup build run test format lint