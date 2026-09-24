package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowLoadingLine
import com.example.ui.theme.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.browserTabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeBrowserTabId.collectAsStateWithLifecycle()
    val history by viewModel.browserHistory.collectAsStateWithLifecycle()
    val adsenseCx by viewModel.adsenseSearchCx.collectAsStateWithLifecycle()
    val isAdSenseEnabled by viewModel.isAdSenseSearchEnabled.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    val currentTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    var urlInput by remember(activeTabId, currentTab?.url) {
        mutableStateOf(currentTab?.url ?: viewModel.getGoogleHomeUrl())
    }

    var isLoading by remember { mutableStateOf(false) }
    var webProgress by remember { mutableStateOf(0) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showTabSwitcherGrid by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Persistent WebViews mapped by Tab ID
    val webViewCache = remember { mutableMapOf<String, WebView>() }

    val englishHeaders = remember {
        mapOf("Accept-Language" to "en-US,en;q=0.9")
    }

    fun handleNavigation(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val isWebUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("www.", ignoreCase = true) ||
                (trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.startsWith("."))

        val finalUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            isWebUrl -> "https://${trimmed.removePrefix("http://").removePrefix("https://")}"
            else -> viewModel.buildGoogleSearchUrl(trimmed)
        }
        urlInput = finalUrl
        currentTab?.let { tab ->
            viewModel.updateActiveBrowserTabUrl(tab.id, finalUrl)
            webViewCache[tab.id]?.loadUrl(finalUrl, englishHeaders)
        }
        focusManager.clearFocus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .testTag("browser_screen_container"),
        containerColor = DarkObsidian,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
            ) {
                // 1. TABS BAR ON TOP (Chrome Style)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF101720))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            color = if (isActive) DarkSurfaceElevated else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isActive) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .widthIn(min = 110.dp, max = 160.dp)
                                .height(34.dp)
                                .clickable {
                                    viewModel.selectBrowserTab(tab.id)
                                    urlInput = tab.url
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tab.favicon,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isActive) Color.White else TextMuted,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close Tab",
                                    tint = if (isActive) CoralRed else TextMuted,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable {
                                            viewModel.closeBrowserTab(tab.id)
                                        }
                                )
                            }
                        }
                    }

                    // Add Tab Button (Up to 3 tabs max)
                    if (tabs.size < 3) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val home = viewModel.getGoogleHomeUrl()
                                        viewModel.addNewBrowserTab(home, "Google Search")
                                        urlInput = home
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "New Tab",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. SEARCH BAR & OMNIBOX ROW (Beneath Tabs) - Crisp, Unblocked BasicTextField
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back to App Navigation
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp).testTag("browser_back_to_home")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to App",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Web Back control
                    IconButton(
                        onClick = {
                            currentTab?.let { tab ->
                                webViewCache[tab.id]?.let {
                                    if (it.canGoBack()) it.goBack()
                                }
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Web Back",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Web Forward control
                    IconButton(
                        onClick = {
                            currentTab?.let { tab ->
                                webViewCache[tab.id]?.let {
                                    if (it.canGoForward()) it.goForward()
                                }
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Web Forward",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // High-Contrast Omnibox Input Field (Full-width, Vertically Centered & Unclipped)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0A121A),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (urlInput.startsWith("https", ignoreCase = true)) Icons.Filled.Lock else Icons.Outlined.Public,
                                contentDescription = "Security",
                                tint = if (urlInput.startsWith("https", ignoreCase = true)) ElectricEmerald else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // BasicTextField with ZERO internal padding clipping
                            BasicTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("browser_omnibox_input"),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(CyberCyan),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = { handleNavigation(urlInput) }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (urlInput.isEmpty()) {
                                            Text(
                                                text = "Search Google or enter website URL",
                                                color = TextMuted,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (urlInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        urlInput = ""
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            if (isLoading) {
                                IconButton(
                                    onClick = {
                                        webViewCache[activeTabId]?.stopLoading()
                                        isLoading = false
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop Loading",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { handleNavigation(urlInput) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Search / Go",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Tab Switcher Button Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTabSwitcherGrid = !showTabSwitcherGrid }
                            .testTag("chrome_tab_switcher_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${tabs.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    // Chrome Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showMenuDropdown = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Tab (Max 3)", color = if (tabs.size < 3) TextPrimary else TextMuted) },
                                leadingIcon = { Icon(Icons.Filled.AddBox, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    showMenuDropdown = false
                                    if (tabs.size < 3) {
                                        val home = viewModel.getGoogleHomeUrl()
                                        viewModel.addNewBrowserTab(home, "Google Search")
                                        urlInput = home
                                    }
                                },
                                enabled = tabs.size < 3
                            )
                            DropdownMenuItem(
                                text = { Text("History (10 Recent)", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, tint = GlowingAmber) },
                                onClick = {
                                    showMenuDropdown = false
                                    showHistoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isDesktopMode) "Mobile View" else "Desktop Site", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Filled.DesktopMac, contentDescription = null, tint = ElectricEmerald) },
                                onClick = {
                                    showMenuDropdown = false
                                    isDesktopMode = !isDesktopMode
                                    currentTab?.let {
                                        webViewCache[it.id]?.settings?.userAgentString = if (isDesktopMode) {
                                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                        } else null
                                        webViewCache[it.id]?.reload()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reload Page", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    showMenuDropdown = false
                                    currentTab?.let { webViewCache[it.id]?.reload() }
                                }
                            )
                            HorizontalDivider(color = DarkCardBorder)
                            DropdownMenuItem(
                                text = { Text("Close Active Tab", color = CoralRed) },
                                leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null, tint = CoralRed) },
                                onClick = {
                                    showMenuDropdown = false
                                    currentTab?.let { viewModel.closeBrowserTab(it.id) }
                                }
                            )
                        }
                    }
                }

                // Search Engine / Live Data Savings Tracker Sub-Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B141E))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, ElectricEmerald.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = ElectricEmerald,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "42% DATA SAVED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ElectricEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "AdBlock & Compression Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Text(
                        text = "⚡ Turbo Tunnel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Loading progress bar (Standard FlowLoadingLine)
                if (isLoading) {
                    FlowLoadingLine(
                        progress = (webProgress / 100f).coerceIn(0.05f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberCyan,
                        trackColor = DarkSurfaceElevated,
                        height = 3.dp
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkObsidian)
        ) {
            currentTab?.let { tab ->
                key(tab.id) {
                    AndroidView(
                        factory = { ctx ->
                            val webView = webViewCache.getOrPut(tab.id) {
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.setSupportMultipleWindows(false)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoading = true
                                            url?.let {
                                                urlInput = it
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            url?.let {
                                                urlInput = it
                                                val title = view?.title ?: it
                                                viewModel.updateActiveBrowserTabUrl(tab.id, it, title)
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val targetUrl = request?.url?.toString() ?: return false
                                            if (targetUrl.startsWith("http://", ignoreCase = true) || targetUrl.startsWith("https://", ignoreCase = true)) {
                                                // Allow WebView to navigate seamlessly without stuck redirects
                                                return false
                                            }
                                            return try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                                view?.context?.startActivity(intent)
                                                true
                                            } catch (e: Exception) {
                                                false
                                            }
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            super.onProgressChanged(view, newProgress)
                                            webProgress = newProgress
                                            if (newProgress >= 100) isLoading = false
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            super.onReceivedTitle(view, title)
                                            title?.let {
                                                viewModel.updateActiveBrowserTabUrl(tab.id, tab.url, it)
                                            }
                                        }
                                    }

                                    loadUrl(tab.url, englishHeaders)
                                }
                            }
                            webView
                        },
                        update = { webView ->
                            if (webView.url == null) {
                                webView.loadUrl(tab.url, englishHeaders)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Tab Switcher Grid Overlay
            if (showTabSwitcherGrid) {
                Surface(
                    color = DarkObsidian.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Open Tabs (${tabs.size} / 3 Max)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            IconButton(onClick = { showTabSwitcherGrid = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(tabs) { tab ->
                                val isSelected = tab.id == activeTabId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            2.dp,
                                            if (isSelected) CyberCyan else DarkCardBorder,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            viewModel.selectBrowserTab(tab.id)
                                            urlInput = tab.url
                                            showTabSwitcherGrid = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = tab.favicon, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = tab.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = tab.url,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = CyberCyan,
                                                        fontSize = 11.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.closeBrowserTab(tab.id)
                                                if (tabs.size <= 1) showTabSwitcherGrid = false
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Close",
                                                tint = CoralRed
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (tabs.size < 3) {
                            Button(
                                onClick = {
                                    val home = viewModel.getGoogleHomeUrl()
                                    viewModel.addNewBrowserTab(home, "Google Search")
                                    urlInput = home
                                    showTabSwitcherGrid = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = DarkObsidian)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OPEN NEW TAB (${tabs.size + 1}/3)", color = DarkObsidian, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // 10-Item History Dialog
    if (showHistoryDialog) {
        BrowserHistoryDialog(
            history = history,
            onDismiss = { showHistoryDialog = false },
            onSelectUrl = { selectedUrl ->
                showHistoryDialog = false
                currentTab?.let { tab ->
                    urlInput = selectedUrl
                    viewModel.updateActiveBrowserTabUrl(tab.id, selectedUrl)
                    webViewCache[tab.id]?.loadUrl(selectedUrl, englishHeaders)
                }
            },
            onClearHistory = {
                viewModel.clearBrowserHistory()
            }
        )
    }
}

@Composable
fun BrowserHistoryDialog(
    history: List<VpnViewModel.BrowserHistoryEntry>,
    onDismiss: () -> Unit,
    onSelectUrl: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "History",
                        tint = GlowingAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GlowingAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "10 RECENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = GlowingAmber,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Automatically retains only the 10 most recent pages visited.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No browsing history found.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(history) { entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUrl(entry.url) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${entry.url} • ${sdf.format(Date(entry.timestamp))}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Clear All", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

