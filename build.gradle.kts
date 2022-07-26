import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    // Para generar modelos de DataFrames
    id("org.jetbrains.kotlinx.dataframe") version "0.8.1"
    // Plugin para serializar
    kotlin("plugin.serialization") version "1.7.10"


}

group = "es.joseluisgs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://repo1.maven.org/maven2/")
    maven(url = "https://repo.osgeo.org/repository/release/")
    maven(url = "https://repo.osgeo.org/repository/snapshot/")
}

dependencies {
    testImplementation(kotlin("test"))
    // DataFrames de Kotlin Jetbrains
    implementation("org.jetbrains.kotlinx:dataframe:0.8.1")
    // Si quiero usar DataTime de Jetbrains Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    // Kotlin serialization JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    // Para hacer logs
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha16")
    // LetsPlot
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin:3.2.0")
    implementation("org.jetbrains.lets-plot:lets-plot-image-export:2.3.0")

    // Mapas GeoTools, para evitar el error de java media, quitar
    implementation("org.geotools:gt-main:27-SNAPSHOT") {
        exclude(group = "javax.media", module = "jai_core")
    }
    implementation("org.geotools:gt-geojson:27-SNAPSHOT") {
        exclude(group = "javax.media", module = "jai_core")
    }
    implementation("org.geotools:gt-shapefile:27-SNAPSHOT") {
        exclude(group = "javax.media", module = "jai_core")
    }
    // Puente entre Les Plot y GeoTools
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-geotools:3.1.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

// Data Schema generator
// Make IDE aware of the generated code:
kotlin.sourceSets.getByName("main").kotlin.srcDir("build/generated/ksp/main/kotlin/")