package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.Variables.selectedAppPackage
import app.revanced.manager.Variables.selectedPatches
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherScreen(
    onClickAppSelector: () -> Unit,
    onClickPatchSelector: () -> Unit,
    viewModel: PatcherViewModel = getViewModel()
) {
    val selectedAmount = selectedPatches.size
    val selectedAppPackage by selectedAppPackage
    val hasAppSelected = selectedAppPackage.isPresent
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
            onClick = onClickAppSelector
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
            onClick = onClickPatchSelector
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
    }
}