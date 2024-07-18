
plugins {
    `java-library`
    id("application")
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
    implementation(project(":extensions:blockchain:catalog-listener"))


    implementation("$groupId:control-plane-spi:$edcVersion")
    implementation(libs.edc.dsp)



}
