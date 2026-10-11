import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

// Exercise the types supplied by CMP-forwarded JVM primitives, not just the
// WinUI modules. A JVM leaf-to-root substitution cycle loses these APIs.
fun densityConsumer(density: Density): Offset = with(density) { Offset(16.dp.toPx(), 8.dp.toPx()) }

@Composable
fun Navigation2Consumer() {
    MaterialTheme {
        NavHost(rememberNavController(), startDestination = "home") {
            composable("home") { Text("WinUI Navigation 2") }
        }
    }
}

@Composable
fun Navigation3Consumer() {
    MaterialTheme {
        NavDisplay(
            backStack = listOf("home"),
            entryProvider = { key -> NavEntry(key) { Text("WinUI Navigation 3") } },
        )
    }
}
