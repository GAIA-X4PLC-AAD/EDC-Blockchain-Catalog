
plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val groupId: String by project
val edcVersion: String by project

dependencies {
    /*
    implementation("$groupId:control-plane-core:$edcVersion")

    implementation("$groupId:http:$edcVersion")

    //implementation("$groupId:api-observability:$edcVersion")

    implementation("$groupId:configuration-filesystem:$edcVersion")
    implementation("$groupId:iam-mock:$edcVersion")

    implementation("$groupId:auth-tokenbased:$edcVersion")
    implementation("$groupId:management-api:$edcVersion")


    //implementation("$groupId:federated-catalog-api:\${VERSION}")
    implementation("$groupId:federated-catalog-spi:$edcVersion")
    //implementation("$groupId:federated-catalog-core:$edcVersion")


    implementation("$groupId:control-plane-spi:$edcVersion")
    implementation("$groupId:transform-spi:$edcVersion")
    implementation("$groupId:asset-api:$edcVersion")
    */

    api(libs.edc.control.plane.spi)
    implementation(libs.edc.http)
    implementation(libs.edc.dsp)
    implementation(libs.edc.management.api)
    // using the dsp api now instead of ids api
    // dsp-api-configuration should contain the TypeTransformerRegistry which can transform objects to json ld and vise versa
    implementation(libs.edc.dsp)


}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}
