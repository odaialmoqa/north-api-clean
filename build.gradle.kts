plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

// Skip test tasks for all projects
allprojects {
    tasks.withType<AbstractTestTask> {
        enabled = false
    }
    
    tasks.withType<Test> {
        enabled = false
    }
}