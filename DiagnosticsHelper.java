plugins {
    id 'com.android.application'
}

android {
    namespace 'com.grimliterev.overlay'
    compileSdk 34

    defaultConfig {
        applicationId "com.grimliterev.overlay"
        minSdk 24
        targetSdk 34
        versionCode 2
        versionName "2.0-executor"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'org.json:json:20231013'
}
