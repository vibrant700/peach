import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.huimantaoxiang.app"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.huimantaoxiang.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 加载 local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        // 获取 DeepSeek 配置（优先读取 gradle.properties -> local.properties -> 环境变量，否则使用默认值）
        // 为了连接 DeepSeek 官方 API，默认 Base URL 设为 https://api.deepseek.com
        val dsBaseUrl = (project.findProperty("DEEPSEEK_BASE_URL") as String?)
            ?: localProperties.getProperty("DEEPSEEK_BASE_URL")
            ?: System.getenv("DEEPSEEK_BASE_URL")
            ?: "https://api.deepseek.com"

        val dsBackend = (project.findProperty("DEEPSEEK_BACKEND") as String?)
            ?: localProperties.getProperty("DEEPSEEK_BACKEND")
            ?: System.getenv("DEEPSEEK_BACKEND")
            ?: "openai" // DeepSeek 官方接口兼容 OpenAI

        // 默认模型设为 deepseek-chat
        val dsModel = (project.findProperty("DEEPSEEK_MODEL") as String?)
            ?: localProperties.getProperty("DEEPSEEK_MODEL")
            ?: System.getenv("DEEPSEEK_MODEL")
            ?: "deepseek-chat"

        // API Key 必须配置，否则无法调用官方接口
        val dsApiKey = (project.findProperty("DEEPSEEK_API_KEY") as String?)
            ?: localProperties.getProperty("DEEPSEEK_API_KEY")
            ?: System.getenv("DEEPSEEK_API_KEY")
            ?: ""

        // DashScope API Key (用于 Qwen 文生图)
        val dashScopeApiKey = (project.findProperty("DASHSCOPE_API_KEY") as String?)
            ?: localProperties.getProperty("DASHSCOPE_API_KEY")
            ?: System.getenv("DASHSCOPE_API_KEY")
            ?: ""

        buildConfigField("String", "AI_BASE_URL", "\"$dsBaseUrl\"")
        buildConfigField("String", "AI_BACKEND_TYPE", "\"$dsBackend\"")
        buildConfigField("String", "AI_MODEL", "\"$dsModel\"")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$dsApiKey\"")
        buildConfigField("String", "DASHSCOPE_API_KEY", "\"$dashScopeApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // 添加CardView和GridLayout依赖
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    // 图片加载库 Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
