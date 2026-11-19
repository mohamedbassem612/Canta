package io.github.samolego.canta.data.app

internal fun installedAppsMarkdown(apps: List<AppInfo>, missingDescription: String): String =
    buildString {
        appendLine("# Installed apps")
        for (app in apps.filterNot { it.isUninstalled }.sortedBy { it.packageName }) {
            appendLine()
            appendLine("## `${app.packageName}`")
            appendLine()
            appendLine(app.description?.takeIf { it.isNotBlank() } ?: missingDescription)
        }
    }
