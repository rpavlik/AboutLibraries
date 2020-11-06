package com.mikepenz.aboutlibraries.plugin

import groovy.xml.MarkupBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class BasicTests {

    static def agpVersion = "4.1.0"
    static def globalRepositories = [
            "google()",
            "jcenter()"
    ]

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File settingsFile
    private File buildFile
    private File mainDir
    private File manifestFile

    @Before
    void setUp() throws Exception {
        settingsFile = testProjectDir.newFile("settings.gradle")
        buildFile = testProjectDir.newFile("build.gradle")
        mainDir = testProjectDir.newFolder("src", "main")
        manifestFile = new File(mainDir, "AndroidManifest.xml").withWriter { writer ->
            def xml = new MarkupBuilder(new IndentPrinter(writer, "    ", true))
            xml.doubleQuotes = true
            xml.mkp.xmlDeclaration(version: '1.0', encoding: 'utf-8')
            xml.manifest('xmlns:android': "http://schemas.android.com/apk/res/android",
                    package: 'aboutlibrariestest') {
                application {

                }
            }
            xml.mkp.yield("\n")
            xml.mkp.comment("GENERATED - do not modify")
        }
    }

    static String getRepositories(String indent = "") {
        def lines = ["repositories {"]
        globalRepositories.each { lines += ("    " + it) }
        lines += "}"
        lines.each { indent + it }.join("\n")
    }

    static def boilerplate = """
buildscript {
${getRepositories("    ")}
    dependencies {
        classpath("com.android.tools.build:gradle:${agpVersion}")
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin")
    }
}
allprojects {
    ${getRepositories("    ")}
    dependencies {
        classpath("com.mikepenz.aboutlibraries:aboutlibraries")
    }
}
"""
    private static def buildTools = "30.0.2"
    private static def targetSdk = 30
    private static def minSdk = 24

    static def minimalAndroidLib = """
apply plugin: 'com.android.library'
apply plugin: 'com.mikepenz.aboutlibraries.plugin'
android {
    compileSdkVersion $targetSdk
    buildToolsVersion "${buildTools}"

    defaultConfig {
        minSdkVersion $minSdk
        targetSdkVersion $targetSdk
        versionCode 1
        versionName "1.0"
    }
}
"""

    @Test
    void testFindLibraries() throws IOException {
        buildFile.withWriter('utf-8') { writer ->
            writer.write(boilerplate)
            writer.write(minimalAndroidLib)
        }
        def taskName = 'findLibraries'
        def taskResult = ensureTaskSucceeds(taskName)
        assertEquals(taskResult.getOutcome(), SUCCESS)

        // Second call should still do the same
        taskResult = ensureTaskSucceeds(taskName)
        assertEquals(taskResult.getOutcome(), SUCCESS)
    }

    def ensureTaskSucceeds(taskName) {
        //! @todo the build can't find the plugin, why?
        // see https://docs.gradle.org/current/userguide/test_kit.html#sub:test-kit-classpath-injection
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(taskName)
                .withPluginClasspath()
                .build()
        assertNotNull(result.task(taskName))
        result.task(taskName)

    }
}