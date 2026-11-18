import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).orElseThrow { IllegalStateException("No version '$alias' in libs.versions.toml") }.requiredVersion

/** `:core:domain` → `com.example.daysmark.core.domain`; the one place module namespaces come from. */
internal val Project.daysmarkNamespace: String
    get() = "com.example.daysmark" + path.replace(':', '.').replace('-', '_')
