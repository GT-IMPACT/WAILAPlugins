plugins {
    alias(libs.plugins.buildconfig)
    id("minecraft")
    id("publish")
}

repositories {
    maven("http://jenkins.usrv.eu:8081/nexus/content/groups/public/") { isAllowInsecureProtocol = true }
    maven("https://jitpack.io")
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
    maven("https://maven.ic2.player.to/") { metadataSources { mavenPom(); artifact() } }
    maven("https://gregtech.overminddl1.com/") { mavenContent { excludeGroup("net.minecraftforge") } }
    mavenCentral()
    mavenLocal()
}

val modId: String by extra
val modName: String by extra
val modGroup: String by extra

buildConfig {
    packageName("space.impact.$modId")
    buildConfigField("String", "MODID", "\"${modId}\"")
    buildConfigField("String", "MODNAME", "\"${modName}\"")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "GROUPNAME", "\"${modGroup}\"")
    useKotlinOutput { topLevelConstants = true }
}

dependencies {
    api("com.github.GTNewHorizons:waila:1.6.+:dev")
    compileOnly("com.github.GTNewHorizons:EnderCore:0.2.+:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:ForestryMC:4.6.+:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:Railcraft:9.14.+:dev") { isTransitive = false }
    compileOnly("curse.maven:pams-harvestcraft-221857:2270206") { isTransitive = false }
    compileOnly("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev") { isTransitive = false }
    compileOnly("curse.maven:extra-utilities-225561:2264384") { isTransitive = false }
    compileOnly("org.projectlombok:lombok:1.18.22") { isTransitive = false }
    compileOnly("com.mod-buildcraft:buildcraft:7.1.23:dev") { isTransitive = false }
    annotationProcessor("org.projectlombok:lombok:1.18.22")
    compileOnly(fileTree(mapOf("dir" to "libs/", "include" to listOf("*.jar"))))

}
