package app.revanced.manager.ui.screen.subscreens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.navigation.AppDestination
import app.revanced.manager.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import com.xinto.taxi.BackstackNavigator
import org.koin.androidx.compose.getViewModel

private const val tag = "AppSelector"

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun AppSelectorSubscreen(
    navigator: BackstackNavigator<AppDestination>,
    vm: AppSelectorViewModel = getViewModel(),
    pvm: PatcherViewModel = getViewModel()
) {
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("hhhhhh") },
                navigationIcon = {
                    IconButton(onClick = navigator::pop) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        val installedApps by vm.installedApps

        when (val installedApps = installedApps) {
            is Resource.Success -> {
                val apps = installedApps.data
                LazyColumn(modifier = Modifier.padding(paddingValues)) {
                    items(count = apps.size) {
                        val app = apps[it]
                        val label = vm.applicationLabel(app)
                        val packageName = app.packageName

                        val same = packageName == label
                        ListItem(modifier = Modifier.clickable {
                            pvm.setSelectedAppPackage(app.packageName)
                            navigator.pop()
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
}