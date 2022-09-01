package app.revanced.manager.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.viewmodel.PatchClass
import app.revanced.patcher.annotation.Package
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages

@Composable
fun PatchCompatibilityDialog(
    patchClass: PatchClass,
    onClose: () -> Unit
) {
    val patch = patchClass.patch
    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(stringResource(id = R.string.compatible_versions))
        },
        text = {
            patch.compatiblePackages!!.forEach { p: Package ->
                Text(
                    p.versions.reversed().joinToString(", ")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = "Dismiss")
            }
        }
    )
}