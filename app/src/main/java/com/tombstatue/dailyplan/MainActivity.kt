package com.tombstatue.dailyplan

import android.content.Intent
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
import com.tombstatue.dailyplan.data.Task
import com.tombstatue.dailyplan.ui.CalendarScreen
import com.tombstatue.dailyplan.ui.HistoryScreen
import com.tombstatue.dailyplan.ui.PlanViewModel
import com.tombstatue.dailyplan.pomodoro.PomodoroEngine
import com.tombstatue.dailyplan.ui.FocusLockActivity
import com.tombstatue.dailyplan.ui.PomodoroScreen
import com.tombstatue.dailyplan.ui.PomodoroViewModel
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

    override fun onResume() {
        super.onResume()
        // 番茄钟运行中 → 重新弹出锁屏（防止用户按 Home 键绕过）
        if (PomodoroEngine.state.value.running) {
            startActivity(
                Intent(this, FocusLockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
            )
        }
    }
}

@Composable
fun AppRoot() {
    val vm: PlanViewModel = viewModel()
    val pomoVm: PomodoroViewModel = viewModel()
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
                    icon = { Text("📅", fontSize = 18.sp) },
                    label = { Text("日历") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = page == 2,
                    onClick = { page = 2 },
                    icon = { Text("🍅", fontSize = 18.sp) },
                    label = { Text("番茄") },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = page == 3,
                    onClick = { page = 3 },
                    icon = { Text("✓", fontSize = 18.sp) },
                    label = { Text("已完成") },
                    colors = itemColors
                )
            }
        }
    ) { padding ->
        when (page) {
            0 -> TodayScreen(
                vm = vm,
                padding = padding,
                onStartPomodoro = { task ->
                    pomoVm.bindTask(task)
                    page = 2  // 跳转到番茄钟
                }
            )
            1 -> CalendarScreen(vm, padding, onGoToday = { page = 0 })
            2 -> PomodoroScreen(padding, todayVm = vm)
            else -> HistoryScreen(vm, padding)
        }
    }
}
