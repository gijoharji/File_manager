// Top-level build file
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false // <-- Updated to 1.9.25
}


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

