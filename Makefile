SHELL := /bin/sh
.DEFAULT_GOAL := help

JAVA ?= java
JAVAC ?= javac
OUT_DIR := out
CLASSPATH := $(OUT_DIR):lib/*
SOURCES := $(shell find src -name '*.java')
PG_VERSION := 42.7.14
PG_JAR := lib/postgresql-$(PG_VERSION).jar

.PHONY: help deps build run test integration-test db-test clean

help:
	@echo 'make build             Compile the application'
	@echo 'make test              Run offline regression tests'
	@echo 'make run               Load .env and start Main'
	@echo 'make db-test           Check the database connection'
	@echo 'make integration-test  Test in a new isolated database schema'
	@echo 'make deps              Download the PostgreSQL driver'
	@echo 'make clean             Remove compiled classes'

deps: $(PG_JAR)

$(PG_JAR):
	@mkdir -p lib
	curl --fail --location --retry 3 --output "$@.tmp" "https://jdbc.postgresql.org/download/postgresql-$(PG_VERSION).jar"
	mv "$@.tmp" "$@"

build: deps
	@mkdir -p "$(OUT_DIR)"
	$(JAVAC) -cp 'lib/*' -d "$(OUT_DIR)" $(SOURCES)

test: build
	$(JAVAC) -cp '$(CLASSPATH)' -d "$(OUT_DIR)" $(wildcard tests/*.java)
	@set -e; for test in $(notdir $(basename $(wildcard tests/*Test.java))); do $(JAVA) -cp '$(CLASSPATH)' "$$test"; done

integration-test: build
	$(JAVAC) -cp '$(CLASSPATH)' -d "$(OUT_DIR)" tests/DatabaseIntegrationCheck.java
	@set -a; if [ -f .env ]; then . ./.env; fi; set +a; exec $(JAVA) -cp '$(CLASSPATH)' DatabaseIntegrationCheck

run: build
	@set -a; if [ -f .env ]; then . ./.env; fi; set +a; exec $(JAVA) -cp '$(CLASSPATH)' Main

db-test: build
	@set -a; if [ -f .env ]; then . ./.env; fi; set +a; exec $(JAVA) -cp '$(CLASSPATH)' database.DatabaseConnectionTest

clean:
	rm -rf "$(OUT_DIR)"
