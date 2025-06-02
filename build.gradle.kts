plugins {
	java
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

fun getGitHash(): String {
	return providers.exec {
		commandLine("git", "rev-parse", "--short", "HEAD")
	}.standardOutput.asText.get().trim()
}

group = "kr.hhplus.be"
version = getGitHash()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
	}
}

dependencies {
    // Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")

    // DB
	runtimeOnly("com.mysql:mysql-connector-j")

	// Swagger
	implementation ("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

	// Lombok
	compileOnly ("org.projectlombok:lombok:1.18.30")
	annotationProcessor ("org.projectlombok:lombok:1.18.30")
	testCompileOnly ("org.projectlombok:lombok:1.18.30")
	testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")

	// QueryDSL (JPA + APT)
	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
	annotationProcessor("jakarta.persistence:jakarta.persistence-api")
	annotationProcessor("jakarta.annotation:jakarta.annotation-api")

	// instancio
	implementation ("org.instancio:instancio-core:2.0.0")

	//redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	//redisson
	implementation("org.redisson:redisson-spring-boot-starter:3.24.0")

	//kafka
	implementation("org.springframework.kafka:spring-kafka")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testImplementation("org.testcontainers:kafka:1.20.4")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.awaitility:awaitility:4.2.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

//sourceSets {
//	getByName("main").java.srcDir("build/generated/sources/annotationProcessor/java/main")
//}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
}
