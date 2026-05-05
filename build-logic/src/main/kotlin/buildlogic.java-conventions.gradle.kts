plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    maven {
        url = uri("https://www.jogamp.org/deployment/maven")
    }

    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

group = "org.xast.xide"
version = "0.1.0"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}