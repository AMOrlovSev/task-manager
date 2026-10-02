.DEFAULT_GOAL := build

setup:
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

.PHONY: setup build run test format lint