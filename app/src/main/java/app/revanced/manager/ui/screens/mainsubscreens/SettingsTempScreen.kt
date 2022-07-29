package app.revanced.manager.ui.screens.mainsubscreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.manager.PreferencesManager
import app.revanced.manager.ui.models.SettingsViewModel
import app.revanced.manager.ui.theme.Theme
import com.jamal.composeprefs3.ui.Divider
import com.jamal.composeprefs3.ui.GroupHeader
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Destination
@RootNavGraph
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTempScreen(
    viewModel: SettingsViewModel = getViewModel(),
) {
    val prefs = viewModel.prefs
    Scaffold(
        modifier = Modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(paddingValues),
        ) {

            if (viewModel.showThemePicker) {
                ThemePicker(
                    onDismissRequest = viewModel::dismissThemePicker,
                    onConfirm = viewModel::setTheme
                )
            }

            GroupHeader(title = "Appearance")

            ListItem(
                modifier = Modifier.clickable { viewModel.showThemePicker() },
                headlineText = { Text(stringResource(R.string.theme)) },
            )

            Divider()

            ListItem(
                modifier = Modifier.clickable { prefs.dynamicTheming = !prefs.dynamicTheming },
                headlineText = { Text("Dynamic color") },
                trailingContent = {
                    Switch(
                        checked = prefs.dynamicTheming,
                        onCheckedChange = { prefs.dynamicTheming = it })
                }
            )

            Divider()

            ListItem(
                modifier = Modifier.clickable(enabled = !prefs.dynamicTheming) {
                    prefs.amoledMode = !prefs.amoledMode
                },
                headlineText = { Text(stringResource(R.string.amoled_mode)) },
                trailingContent = {
                    Switch(
                        enabled = !prefs.dynamicTheming,
                        checked = prefs.amoledMode,
                        onCheckedChange = { prefs.amoledMode = it }
                    )
                }
            )

            GroupHeader(
                title = "Sources"
            )

            ListItem(
                modifier = Modifier.clickable { prefs.autoUpdate = !prefs.autoUpdate },
                headlineText = { Text("Auto update") },
                trailingContent = {
                    Switch(
                        checked = prefs.autoUpdate,
                        onCheckedChange = { prefs.autoUpdate = it })
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePicker(
    onDismissRequest: () -> Unit,
    onConfirm: (Theme) -> Unit,
    prefs: PreferencesManager = get()
) {
    var selectedTheme by remember { mutableStateOf(prefs.theme) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                Theme.values().forEach { theme ->
                    Row(
                        modifier = Modifier.clickable { selectedTheme = theme },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            theme.displayName,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(Modifier.weight(1f, true))

                        RadioButton(
                            selected = theme == selectedTheme,
                            onClick = { selectedTheme = theme }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedTheme)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    )
}