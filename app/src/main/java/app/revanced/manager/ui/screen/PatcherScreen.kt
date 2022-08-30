package app.revanced.manager.ui.screen

 import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.FloatingActionButton
import app.revanced.manager.ui.navigation.PatcherDestination
import app.revanced.manager.ui.screen.subscreens.AppSelectorSubscreen
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import com.xinto.taxi.Taxi
import com.xinto.taxi.rememberBackstackNavigator
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PatcherScreen(
    viewModel: PatcherViewModel = getViewModel()
) {
    val navigator = "" // TODO:uhh help 1
    val selectedAmount = viewModel.selectedPatches
    val selectedAppPackage by viewModel.selectedAppPackage
    val hasAppSelected = selectedAppPackage.isPresent

    Scaffold(floatingActionButton = {
        FloatingActionButton(
            enabled = hasAppSelected && viewModel.anyPatchSelected(),
            icon = { Icon(Icons.Default.Build, "sd") },
            text = { Text("Patch") },
            onClick = { viewModel.startPatcher() },
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                onClick = {
                    // TODO:uhh help 2
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_application_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = selectedAppPackage.orElse(stringResource(R.string.card_application_body)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                enabled = hasAppSelected,
                onClick = {
                    // TODO:uhh help 3
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.card_patches_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (!hasAppSelected) {
                            "Select an application first."
                        } else if (viewModel.anyPatchSelected()) {
                            "$selectedAmount patches selected."
                        } else {
                            stringResource(R.string.card_patches_body_patches)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }
            // Taxi(
            //    modifier = Modifier.weight(1f, true),
            //    navigator = navigator,
            //    transitionSpec = { fadeIn() with fadeOut() }
            //) { destination ->
            //    when (destination) {
                    // TODO:uhh help 5
            //    }
            // }
        }
    }
}
