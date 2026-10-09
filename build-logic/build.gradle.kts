plugins {
    `kotlin-dsl`
}

// 컨벤션 플러그인이 적용하는 외부 플러그인을 의존성으로 선언한다. 그래서 서비스의 plugins 블록에는 버전이 등장하지 않는다.
fun plugin(plugin: Provider<PluginDependency>) = plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }

dependencies {
    implementation(plugin(libs.plugins.spotless))
    implementation(plugin(libs.plugins.errorprone))
    implementation(plugin(libs.plugins.spring.boot))
    implementation(plugin(libs.plugins.spring.dependency.management))
}
