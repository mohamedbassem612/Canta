package io.github.samolego.canta.ui.menu

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.badge_info
import io.github.samolego.canta.generated.resources.export_app_list
import io.github.samolego.canta.generated.resources.logs
import io.github.samolego.canta.generated.resources.presets
import io.github.samolego.canta.generated.resources.settings
import io.github.samolego.canta.ui.navigation.Screen

@Composable
fun MoreOptionsMenu(
        showMenu: Boolean,
        showBadgeInfoDialog: () -> Unit,
        navigateToPage: (route: String) -> Unit,
        onDismiss: () -> Unit,
        onExportAppList: (() -> Unit)?,
        exportEnabled: Boolean,
) {

    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(200.dp)
    ) {
        if (onExportAppList != null) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.export_app_list)) },
                enabled = exportEnabled,
                onClick = {
                    onDismiss()
                    onExportAppList()
                },
            )
        }

        // Badge info dialog
        DropdownMenuItem(
                text = { Text(stringResource(Res.string.badge_info)) },
                onClick = {
                    showBadgeInfoDialog()
                    onDismiss()
                }
        )

        // Logs page
        DropdownMenuItem(
                text = { Text(stringResource(Res.string.logs)) },
                onClick = {
                    navigateToPage(Screen.Logs.route)
                    onDismiss()
                }
        )

        // Settings page
        DropdownMenuItem(
                text = { Text(stringResource(Res.string.settings)) },
                onClick = {
                    navigateToPage(Screen.Settings.route)
                    onDismiss()
                }
        )

        DropdownMenuItem(
            text = { Text(stringResource(Res.string.presets)) },
            onClick = {
                navigateToPage(Screen.Presets.route)
                onDismiss()
            }
        )
    }
}
