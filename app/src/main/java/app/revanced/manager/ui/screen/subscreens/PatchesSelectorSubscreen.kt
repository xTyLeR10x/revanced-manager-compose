package app.revanced.manager.ui.screen.subscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.navigation.AppDestination
import app.revanced.manager.Variables
import app.revanced.manager.ui.viewmodel.PatchSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import app.revanced.patcher.extensions.PatchExtensions.patchName
import com.xinto.taxi.BackstackNavigator
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchesSelectorSubscreen(
    navigator: BackstackNavigator<AppDestination>,
    pvm: PatcherViewModel = getViewModel()
) {
    val patches = rememberSaveable { pvm.getFilteredPatches() }
    val patchesState by Variables.patches
    var query by mutableStateOf("")

    when (patchesState) {
        is Resource.Success -> {
            Scaffold(floatingActionButton = {
                if (pvm.anyPatchSelected()) {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Default.Check, "Done") },
                        text = { Text("Done") },
                        onClick = { navigator.pop() },
                    )
                }
            }) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp, 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                shape = RoundedCornerShape(12.dp),
                                value = query,
                                onValueChange = { newValue ->
                                    query = newValue
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, "Search")
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = {
                                            query = ""
                                        }) {
                                            Icon(Icons.Default.Clear, "Clear")
                                        }
                                    }
                                },
                            )
                        }
                    }
                    LazyColumn {

                        if (query.isEmpty() || query.isBlank()) {
                            items(count = patches.size) {
                                val patch = patches[it]
                                val name = patch.patch.patchName
                                PatchSelectorViewModel(patch, pvm.isPatchSelected(name)) {
                                    pvm.selectPatch(name, !pvm.isPatchSelected(name))
                                }
                            }
                        } else {
                            items(count = patches.size) {
                                val patch = patches[it]
                                val name = patch.patch.patchName
                                if (name.contains(query.lowercase())) {
                                    PatchSelectorViewModel(patch, pvm.isPatchSelected(name)) {
                                        pvm.selectPatch(name, !pvm.isPatchSelected(name))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> LoadingIndicator(null)
    }
}