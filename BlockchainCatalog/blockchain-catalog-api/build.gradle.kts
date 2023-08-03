
plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

val groupId: String by project
val edcVersion: String by project

dependencies {
    implementation("$groupId:control-plane-core:$edcVersion")

    implementation("$groupId:http:$edcVersion")

    implementation("$groupId:configuration-filesystem:$edcVersion")
    implementation("$groupId:iam-mock:$edcVersion")

    implementation("$groupId:auth-tokenbased:$edcVersion")
    implementation("$groupId:management-api:$edcVersion")


    //implementation("$groupId:federated-catalog-api:\${VERSION}")
    implementation("$groupId:federated-catalog-spi:$edcVersion")
    //implementation("$groupId:federated-catalog-core:$edcVersion")
    implementation("$groupId:web-spi:$edcVersion")


    api("$groupId:control-plane-spi:$edcVersion")
    implementation("$groupId:http:$edcVersion")
    implementation(project(":BlockchainCatalog:blockchain-catalog-listener"))


    implementation("$groupId:control-plane-spi:$edcVersion")


}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}
