plugins {
	application
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.1.0"
	jacoco
	id("org.sonarqube") version "7.5.0.8588"
}

group = "hexlet.code"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

application {
	mainClass = "hexlet.code.AppApplication"
}

repositories {
	mavenCentral()
}

dependencies {
	// --- Spring Boot starters ---
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-flyway")

	// --- Безопасность ---
	implementation("org.springframework.security:spring-security-crypto")

	// --- H2 (только для разработки) ---
	runtimeOnly("com.h2database:h2")
	developmentOnly("org.springframework.boot:spring-boot-h2console")

	// --- Сериализация / OpenAPI ---
	implementation("org.openapitools:jackson-databind-nullable:0.2.10")

	// --- Lombok + MapStruct (порядок процессоров важен!) ---
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	implementation("org.mapstruct:mapstruct:1.6.3")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

	// --- Runtime: драйверы и миграции БД ---
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")

	// --- Тестовые стартеры ---
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")

	// --- Testcontainers ---
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

spotless {
	java {
		googleJavaFormat().aosp()
		formatAnnotations()
	}
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.check {
	dependsOn(tasks.jacocoTestReport)
}

sonar {
	properties {
		property("sonar.projectKey", "AMOrlovSev_task-manager")
		property("sonar.organization", "amorlovsev")
	}
}
