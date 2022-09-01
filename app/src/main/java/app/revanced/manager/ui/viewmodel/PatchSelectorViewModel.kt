package app.revanced.manager.ui.viewmodel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.PatchCompatibilityDialog
import app.revanced.manager.ui.theme.Typography
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.version

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchSelectorViewModel(patchClass: PatchClass, isSelected: Boolean, onSelected: () -> Unit) {
    val patch = patchClass.patch
    val name = patch.name

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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