SHELL := /bin/bash
.DEFAULT_GOAL := help

JOSM_JAR ?= .cache/josm-tested.jar
JOSM_URL ?= https://josm.openstreetmap.de/download/josm-tested.jar
JOSM_PLUGINS_DIR ?= $(HOME)/.local/share/JOSM/plugins
PARQUETRY_DIR ?= .cache/parquetry
PARQUETRY_REPO ?= https://github.com/tileverse-io/parquetry.git
PARQUETRY_REF ?= 05e5bad2bd6fc193e29212e8a596f985e408f703
JAVA ?= java
MVN ?= mvn

.PHONY: help bootstrap bootstrap-josm bootstrap-parquetry build test install run clean inspect
help:
	@echo "make bootstrap            Prepare JOSM and install Parquetry in local Maven cache"
	@echo "make bootstrap-josm       Download JOSM compile-time JAR"
	@echo "make bootstrap-parquetry  Clone and locally install Parquetry"
	@echo "make build                Build the plugin JAR"
	@echo "make install              Copy plugin JAR into JOSM plugin directory"
	@echo "make run                  Launch JOSM with Java preview enabled"
	@echo "make inspect              Inspect plugin manifest"
	@echo "make clean                Remove plugin build outputs"

bootstrap: bootstrap-josm bootstrap-parquetry

bootstrap-josm:
	@mkdir -p "$(dir $(JOSM_JAR))"
	@if [ -s "$(JOSM_JAR)" ]; then \
		echo "JOSM JAR already present: $(JOSM_JAR)"; \
	else \
		curl --fail --location --retry 3 "$(JOSM_URL)" -o "$(JOSM_JAR)"; \
	fi

bootstrap-parquetry:
	@command -v git >/dev/null || { echo "git is required"; exit 1; }
	@command -v java >/dev/null || { echo "Java 25 is required"; exit 1; }
	@java -version 2>&1 | head -n 1
	@if [ ! -d "$(PARQUETRY_DIR)/.git" ]; then \
		mkdir -p "$(dir $(PARQUETRY_DIR))"; \
		git clone --no-checkout "$(PARQUETRY_REPO)" "$(PARQUETRY_DIR)" || exit $$?; \
	else \
		echo "Reusing existing Parquetry checkout: $(PARQUETRY_DIR)"; \
	fi
	@cd "$(PARQUETRY_DIR)" && \
		git fetch --depth 1 origin "$(PARQUETRY_REF)" && \
		git checkout --detach FETCH_HEAD && \
		git submodule update --init --recursive && \
		./mvnw -DskipTests install
	@echo "Parquetry installed in the local Maven repository."

build:
	@test -s "$(JOSM_JAR)" || { echo "Run make bootstrap first"; exit 1; }
	$(MVN) -DskipTests package

test:
	$(MVN) test

install: build
	mkdir -p "$(JOSM_PLUGINS_DIR)"
	cp target/geoparquet.jar "$(JOSM_PLUGINS_DIR)/geoparquet.jar"
	@echo "Restart JOSM with --enable-preview."

run: bootstrap-josm
	java --enable-preview \
		--add-exports=java.base/sun.security.action=ALL-UNNAMED \
		--add-exports=java.desktop/com.sun.imageio.plugins.jpeg=ALL-UNNAMED \
		--add-exports=java.desktop/com.sun.imageio.spi=ALL-UNNAMED \
		-jar .cache/josm-tested.jar

inspect:
	unzip -p target/geoparquet.jar META-INF/MANIFEST.MF

clean:
	$(MVN) clean
