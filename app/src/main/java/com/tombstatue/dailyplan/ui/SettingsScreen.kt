package com.tombstatue.dailyplan.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tombstatue.dailyplan.pomodoro.AppUsageWatcher
import com.tombstatue.dailyplan.pomodoro.SettingsStore
import com.tombstatue.dailyplan.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var whitelist by remember { mutableStateOf<Set<String>>(emptySet()) }
    var forceCode by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var appsLoaded by remember { mutableStateOf(false) }
    var whitelistExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        whitelist = SettingsStore.whitelist()
        forceCode = SettingsStore.forceExitCode()
        installedApps = loadInstalledApps(context)
        appsLoaded = true
    }

    val usageGranted = AppUsageWatcher.usagePermissionGranted(context)
    val overlayGranted = AppUsageWatcher.hasOverlayPermission(context)

    LazyColumn(
        modifier = Modifier
            .background(Bg)
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("设置", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
        }

        item {
            SectionCard("🎨 主题") {
                var themeId by remember { mutableStateOf(ThemeHolder.ID_NIGHT) }
                LaunchedEffect(Unit) { themeId = SettingsStore.themeId() }

                val pickCustom = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    uri?.let {
                        scope.launch {
                            val path = copyUriToFile(context, it, "custom_bg.jpg")
                            if (path != null) {
                                ThemeCustomizer.buildCustomTheme(path)?.let { spec ->
                                    ThemeHolder.setCustom(spec)
                                    SettingsStore.setCustomBgPath(path)
                                }
                                SettingsStore.setThemeId(ThemeHolder.ID_CUSTOM)
                                themeId = ThemeHolder.ID_CUSTOM
                            }
                        }
                    }
                }
                val pickFocus = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    uri?.let {
                        scope.launch {
                            copyUriToFile(context, it, "focus_bg.jpg")?.let { p ->
                                SettingsStore.setFocusBgPath(p)
                            }
                        }
                    }
                }

                fun pickTheme(id: String) {
                    ThemeHolder.apply(id)
                    scope.launch { SettingsStore.setThemeId(id) }
                    themeId = id
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip("夜间紫", ThemeHolder.NIGHT.bg, ThemeHolder.NIGHT.textMain, ThemeHolder.NIGHT.accent, themeId == ThemeHolder.ID_NIGHT) {
                        pickTheme(ThemeHolder.ID_NIGHT)
                    }
                    ThemeChip("浅色清新", ThemeHolder.FRESH.bg, ThemeHolder.FRESH.textMain, ThemeHolder.FRESH.accent, themeId == ThemeHolder.ID_FRESH) {
                        pickTheme(ThemeHolder.ID_FRESH)
                    }
                    ThemeChip("冷蓝静谧", ThemeHolder.CALM.bg, ThemeHolder.CALM.textMain, ThemeHolder.CALM.accent, themeId == ThemeHolder.ID_CALM) {
                        pickTheme(ThemeHolder.ID_CALM)
                    }
                    ThemeChip(
                        "自定义🌈", ThemeHolder.NIGHT.bg, ThemeHolder.NIGHT.textMain, ThemeHolder.NIGHT.accent,
                        themeId == ThemeHolder.ID_CUSTOM
                    ) {
                        pickCustom.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "自定义主题：上传一张图片作为全局背景，文字颜色会按图片亮度自动深浅适配。",
                    color = TextDim, fontSize = 11.sp
                )
                if (themeId == ThemeHolder.ID_CUSTOM) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = {
                        pickCustom.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("更换图片", color = Accent, fontSize = 12.sp) }
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("专注页背景", color = TextMain, fontSize = 13.sp)
                        Text("不设置则跟随全局主题", color = TextDim, fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        pickFocus.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("上传图片", color = Accent, fontSize = 12.sp) }
                    TextButton(onClick = {
                        scope.launch { SettingsStore.setFocusBgPath(null) }
                    }) { Text("跟随全局", color = TextDim, fontSize = 12.sp) }
                }
            }
        }

        item {
            SectionCard("📴 专注验证码") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("退出专注需输入验证码", color = TextMain, fontSize = 14.sp)
                        Text("关闭时退出只需一键确认", color = TextDim, fontSize = 11.sp)
                    }
                    Switch(
                        checked = forceCode,
                        onCheckedChange = { on ->
                            forceCode = on
                            scope.launch { SettingsStore.setForceExitCode(on) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = TextDim
                        )
                    )
                }
            }
        }

        item {
            SectionCard("📩 白名单应用") {
                Text(
                    "专注页面点击白名单应用图标即可打开；打开其他应用会自动拉回专注页。",
                    color = TextDim,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))

                PermissionRow("使用情况访问", granted = usageGranted) {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                PermissionRow("悬浮窗权限", granted = overlayGranted) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { whitelistExpanded = !whitelistExpanded }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("已添加 ${whitelist.size} 个应用", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (whitelistExpanded) "收起 ▾" else "展开 ▸", color = TextDim, fontSize = 12.sp)
                }

                if (whitelistExpanded) {
                    if (!appsLoaded) {
                        Text("加载应用列表…", color = TextDim, fontSize = 12.sp)
                    } else {
                        installedApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val next = if (app.pkg in whitelist) whitelist - app.pkg else whitelist + app.pkg
                                        whitelist = next
                                        scope.launch { SettingsStore.setWhitelist(next) }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                app.drawable?.let {
                                    Image(
                                        bitmap = it.toBitmap(32, 32).asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(app.label, color = TextMain, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                Checkbox(
                                    checked = app.pkg in whitelist,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = TextDim)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !granted, onClick = onGrant)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMain, fontSize = 13.sp)
        if (granted) {
            Text("已开通", color = AllDoneGreen, fontSize = 12.sp)
        } else {
            Text("去开通 ›", color = Accent, fontSize = 12.sp)
        }
    }
}

private data class InstalledApp(val pkg: String, val label: String, val drawable: Drawable?)

@Composable
private fun ThemeChip(
    label: String,
    previewBg: androidx.compose.ui.graphics.Color,
    previewText: androidx.compose.ui.graphics.Color,
    previewAccent: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(previewBg)
                .then(
                    if (selected) Modifier.border(2.dp, previewAccent, RoundedCornerShape(9.dp))
                    else Modifier
                )
        ) {
            Text("字", color = previewText, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) Accent else TextDim,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private suspend fun copyUriToFile(context: android.content.Context, uri: android.net.Uri, name: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val f = java.io.File(context.filesDir, name)
            context.contentResolver.openInputStream(uri)?.use { ins ->
                f.outputStream().use { ins.copyTo(it) }
            }
            f.absolutePath
        }.getOrNull()
    }

private fun loadInstalledApps(context: android.content.Context): List<InstalledApp> {
    val pm = context.packageManager
    return runCatching {
        val myPkg = context.packageName
        pm.getInstalledApplications(PackageManager.MATCH_ALL).asSequence()
            .filter { !it.packageName.startsWith("com.android") && it.packageName != myPkg }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .map { info ->
                InstalledApp(
                    pkg = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    drawable = pm.getApplicationIcon(info)
                )
            }.toList()
    }.getOrDefault(emptyList())
}
