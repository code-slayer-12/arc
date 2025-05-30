// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.2")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.4.4")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive:3.4.3")

    testImplementation("org.springframework.boot:spring-boot-testcontainers:3.4.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.3")
    testImplementation("org.springframework.boot:spring-boot-starter:3.4.3")
    testImplementation("org.testcontainers:mongodb:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
}
