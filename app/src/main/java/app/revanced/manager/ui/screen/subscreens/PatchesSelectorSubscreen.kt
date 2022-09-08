package app.revanced.manager.ui.screen.subscreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.Variables
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.PatchCompatibilityDialog
import app.revanced.manager.ui.navigation.AppDestination
import app.revanced.manager.ui.theme.Typography
import app.revanced.manager.ui.viewmodel.PatchClass
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.version
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

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.card_patches_header),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator::pop) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pvm.selectAllPatches(patches, !pvm.anyPatchSelected())
                    }) {
                        if (!pvm.anyPatchSelected()) Icon(
                            Icons.Default.SelectAll,
                            contentDescription = null
                        ) else Icon(Icons.Default.Deselect, contentDescription = null)
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            when (patchesState) {
                is Resource.Success -> {
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
                                PatchCard(patch, pvm.isPatchSelected(name)) {
                                    pvm.selectPatch(name, !pvm.isPatchSelected(name))
                                }
                            }
                        } else {
                            items(count = patches.size) {
                                val patch = patches[it]
                                val name = patch.patch.patchName
                                if (name.contains(query.lowercase())) {
                                    PatchCard(patch, pvm.isPatchSelected(name)) {
                                        pvm.selectPatch(name, !pvm.isPatchSelected(name))
                                    }
                                }
                            }
                        }
                    }
                }
                else -> LoadingIndicator(null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchCard(patchClass: PatchClass, isSelected: Boolean, onSelected: () -> Unit) {
    val patch = patchClass.patch
    val name = patch.patchName

    var showDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .padding(16.dp, 4.dp),
        enabled = !patchClass.unsupported,
        onClick = onSelected
    ) {
        Column(modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 12.dp)) {
            Row {
                Column(
                    Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = name.replace("-", " ").split(" ")
                            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(4.dp))
                Row(
                    Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = patch.version ?: "unknown",
                        style = Typography.bodySmall
                    )
                }
                Spacer(Modifier.weight(1f, true))
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        if (patchClass.hasPatchOptions) {
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Settings, contentDescription = "Patch Options")
                            }
                        }
                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                            Checkbox(
                                enabled = !patchClass.unsupported,
                                checked = isSelected,
                                onCheckedChange = { onSelected() }
                            )
                        }
                    }
                }
            }
            var isExpanded by remember { mutableStateOf(false) }
            patch.description?.let { desc ->
                Text(
                    text = desc,
                    modifier = Modifier
                        .padding(0.dp, 8.dp, 22.dp, 8.dp)
                        .clickable { isExpanded = !isExpanded },
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (patchClass.unsupported) {
                Column {
                    Row {
                        if (showDialog) {
                            PatchCompatibilityDialog(
                                onClose = { showDialog = false },
                                patchClass = patchClass
                            )
                        }
                        InputChip(
                            selected = false,
                            onClick = { showDialog = true },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(id = R.string.unsupported_version)
                                )
                            },
                            label = { Text(stringResource(id = R.string.unsupported_version)) }
                        )
                    }
                }
            }
        }
    }
}