package app.revanced.manager.ui.screen.subscreens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.KoinApplication.Companion.init

private const val tag = "AppSelector"

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun AppSelectorSubscreen(
    vm: AppSelectorViewModel = getViewModel(),
    pvm: PatcherViewModel = getViewModel()
) {
    val installedApps by vm.installedApps

    when (val installedApps = installedApps) {
        is Resource.Success -> {
            val apps = installedApps.data
            LazyColumn {
                items(count = apps.size) {
                    val app = apps[it]
                    val label = vm.applicationLabel(app)
                    val packageName = app.packageName

                    val same = packageName == label
                    ListItem(modifier = Modifier.clickable {
                        pvm.setSelectedAppPackage(app.packageName)
                    }, icon = {
                        AppIcon(vm.loadIcon(app), packageName)
                    }, text = {
                        if (same) {
                            Text(packageName)
                        } else {
                            Text(label)
                        }
                    }, secondaryText = {
                        if (!same) {
                            Text(packageName)
                        }
                    })
                }
            }
        }
        else -> {}
    }
}