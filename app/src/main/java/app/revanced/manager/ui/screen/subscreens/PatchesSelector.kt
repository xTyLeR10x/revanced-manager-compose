package app.revanced.manager.ui.screen.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.revanced.manager.ui.navigation.AppDestination
import com.xinto.taxi.BackstackNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchesSelector(
        navigator: BackstackNavigator<AppDestination>
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
        Column(modifier = Modifier.padding(paddingValues)) {
            Text("hi")
        }
    }
}