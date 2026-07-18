package com.tombstatue.dailyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tombstatue.dailyplan.ui.HistoryScreen
import com.tombstatue.dailyplan.ui.PlanViewModel
import com.tombstatue.dailyplan.ui.TodayScreen
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.Bg
import com.tombstatue.dailyplan.ui.theme.CardBg
import com.tombstatue.dailyplan.ui.theme.DailyPlanTheme
import com.tombstatue.dailyplan.ui.theme.TextDim

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyPlanTheme { AppRoot() }
        }
    }
}

@Composable
fun AppRoot() {
    val vm: PlanViewModel = viewModel()
    // 每次回到前台检查是否跨天（设计文档 §7）
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.checkRollover() }

    var page by rememberSaveable { mutableIntStateOf(0) }
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Accent,
        selectedTextColor = Accent,
        unselectedIconColor = TextDim,
        unselectedTextColor = TextDim,
        indicatorColor = Accent.copy(alpha = 0.15f)
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = CardBg) {
                NavigationBarItem(
                    selected = page == 0,
                    onClick = { page = 0 },
                    icon = { Text("📋", fontSize = 18.sp) },
                    label = { Text("今天") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = page == 1,
                    onClick = { page = 1 },
                    icon = { Text("✓", fontSize = 18.sp) },
                    label = { Text("已完成") },
                    colors = itemColors
                )
            }
        }
    ) { padding ->
        when (page) {
            0 -> TodayScreen(vm, padding)
            else -> HistoryScreen(vm, padding)
        }
    }
}
