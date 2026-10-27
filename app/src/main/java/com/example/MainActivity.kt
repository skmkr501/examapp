package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import java.io.File
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PurpleAccent
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay

const val SITE_URL = "https://examappagkk.vercel.app/"

/** ওয়েব পেজ থেকে PDF সেভ ও পুশ টোকেন পাওয়ার ব্রিজ (JS: window.ExamAndroid) */
class PdfBridge(private val ctx: Context) {
    private val ui = Handler(Looper.getMainLooper())
    private fun toast(t: String) = ui.post { Toast.makeText(ctx, t, Toast.LENGTH_LONG).show() }

    @JavascriptInterface
    fun getPushToken(): String {
        val sp = ctx.getSharedPreferences("push", Context.MODE_PRIVATE)
        return sp.getString("token", "") ?: ""
    }

    @JavascriptInterface
    fun savePdf(base64: String, name: String): Boolean {
        return try {
            val bytes = Base64.decode(base64.substringAfter(","), Base64.DEFAULT)
            val fn = if (name.endsWith(".pdf")) name else "$name.pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fn)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    ?: throw Exception("ফাইল তৈরি হয়নি")
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                toast("✅ Downloads ফোল্ডারে সেভ হয়েছে: $fn")
                ui.post {
                    try {
                        val v = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(v)
                    } catch (_: Exception) { }
                }
            } else {
                val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: ctx.filesDir
                File(dir, fn).writeBytes(bytes)
                toast("✅ সেভ হয়েছে: ${dir.absolutePath}/$fn")
            }
            true
        } catch (e: Exception) {
            toast("❌ সেভ ব্যর্থ: ${e.message}")
            false
        }
    }
}

data class NavTab(
    val title: String,
    val icon: ImageVector,
    val hash: String
)

class MainActivity : ComponentActivity() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intentData = result.data
            val results: Array<Uri>? = when {
                intentData?.dataString != null -> arrayOf(Uri.parse(intentData.dataString))
                intentData?.clipData != null -> {
                    val clip = intentData.clipData!!
                    Array(clip.itemCount) { clip.getItemAt(it).uri }
                }
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrEmpty()) {
                getSharedPreferences("push", Context.MODE_PRIVATE)
                    .edit()
                    .putString("token", token)
                    .apply()
                Log.d("FCM", "Saved token on launch: $token")
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppHost(
                    onOpenFilePicker = { callback ->
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        filePickerLauncher.launch(Intent.createChooser(intent, "ছবি বা ফাইল বাছাই করুন"))
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppHost(
    onOpenFilePicker: (ValueCallback<Array<Uri>>) -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentHash by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0) }

    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Camera permission request - only launched on-demand when website requests camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingWebPermissionRequest?.let { it.grant(it.resources) }
        } else {
            pendingWebPermissionRequest?.deny()
            Toast.makeText(context, "ক্যামেরা পারমিশন দিলে OMR স্ক্যান কাজ করবে", Toast.LENGTH_SHORT).show()
        }
        pendingWebPermissionRequest = null
    }

    // Separate launcher for POST_NOTIFICATIONS
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            delay(3000)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val tabs = listOf(
        NavTab("স্টুডেন্ট", Icons.Default.School, ""),
        NavTab("টিচার", Icons.Default.CoPresent, "#teacher"),
        NavTab("অ্যাডমিন", Icons.Default.AdminPanelSettings, "#admin"),
        NavTab("রিফ্রেশ", Icons.Default.Refresh, "reload")
    )

    BackHandler(enabled = true) {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else if (currentHash.isNotEmpty()) {
            webView?.loadUrl(SITE_URL)
            currentHash = ""
        } else {
            (context as? ComponentActivity)?.finish()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEach { tab ->
                    val selected = when (tab.hash) {
                        "reload" -> false
                        else -> currentHash == tab.hash
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab.hash == "reload") {
                                webViewRef?.reload()
                            } else {
                                currentHash = tab.hash
                                val targetUrl = SITE_URL + tab.hash
                                webViewRef?.loadUrl(targetUrl)
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = {
                            Text(
                                tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleAccent,
                            selectedTextColor = PurpleAccent,
                            indicatorColor = PurpleAccent.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            loadsImagesAutomatically = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            displayZoomControls = false
                            builtInZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "$userAgentString ExamSiteAndroidApp/1.0"
                        }

                        addJavascriptInterface(PdfBridge(ctx), "ExamAndroid")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress
                                isLoading = newProgress < 100
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                if (filePathCallback != null) {
                                    onOpenFilePicker(filePathCallback)
                                    return true
                                }
                                return false
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (request == null) return
                                val hasCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                                if (hasCamera) {
                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        request.grant(request.resources)
                                    } else {
                                        pendingWebPermissionRequest = request
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                } else {
                                    request.grant(request.resources)
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false

                                // Open WhatsApp or telephone or external share links in native apps
                                if (url.startsWith("https://wa.me") || url.startsWith("whatsapp://") ||
                                    url.startsWith("tel:") || url.startsWith("mailto:")
                                ) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        ctx.startActivity(intent)
                                        return true
                                    } catch (_: Exception) {
                                        Toast.makeText(ctx, "অ্যাপটি ফোনে পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                        return true
                                    }
                                }

                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }

                        loadUrl(SITE_URL)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )

            if (isLoading && loadingProgress < 85) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = PurpleAccent,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
