// Fakes shared by every module's tests. Never a dependency of production code.
plugins { alias(libs.plugins.daysmark.kmp.library) }

kotlin { sourceSets { commonMain.dependencies { api(projects.core.domain) } } }
