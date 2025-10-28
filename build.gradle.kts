plugins {
   id("java")
   id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "me.haxx0r"
version = "0.1.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    runServer {
        minecraftVersion("1.21.8")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}