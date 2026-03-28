package com.ycngmn.notubetv.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Debug
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebView as AndroidWebView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
import android.net.http.SslError
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.ycngmn.notubetv.R
import com.ycngmn.notubetv.ui.YoutubeVM
import com.ycngmn.notubetv.ui.components.UpdateAppScreen
import com.ycngmn.notubetv.utils.ExitBridge
import com.ycngmn.notubetv.utils.NetworkBridge
import com.ycngmn.notubetv.utils.fetchScripts
import com.ycngmn.notubetv.utils.getUpdate
import com.ycngmn.notubetv.utils.permHandler
import com.ycngmn.notubetv.utils.readRaw
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED = true
private const val ALLOW_SSL_ERRORS_WHEN_DEBUGGER_ATTACHED = true
private const val WEBVIEW_DEBUG_TAG = "YoutubeWV"
private const val YOUTUBE_TV_URL = "https://www.youtube.com/tv"
private const val TV_TOUCH_DRAG_THRESHOLD = 8
private const val TV_INITIAL_SCALE = 25
private const val SPOOFED_VIEWPORT_WIDTH = 3840f
private const val TOUCH_SCALE_MIN = 10
private const val TOUCH_SCALE_MAX = 100
private const val TV_USER_AGENT = "Mozilla/5.0 Cobalt/25 (Sony, PS4, Wired)"
private val NAV_BUTTON_SIZE = 44.dp
private val NAV_PAD_PADDING = 20.dp
private val NAV_BUTTON_COLOR = Color.Black.copy(alpha = 0.22f)
private const val NAV_PAD_AUTO_HIDE_MILLIS = 4000L

@Composable
fun YoutubeWV(youtubeVM: YoutubeVM = viewModel()) {

    val context = LocalContext.current
    val activity = context as Activity
    val isTvDevice = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val lifecycleOwner = LocalLifecycleOwner.current

    val state = rememberWebViewState(YOUTUBE_TV_URL)
    val navigator = rememberWebViewNavigator()

    val jsScript = youtubeVM.scriptData
    val updateData = youtubeVM.updateData
    val coroutineScope = rememberCoroutineScope()
    val webViewRef = remember { mutableStateOf<AndroidWebView?>(null) }
    val isDirectionPadVisible = remember { mutableStateOf(true) }
    val directionPadResetKey = remember { mutableStateOf(0) }

    val loadingState = state.loadingState
    val exitTrigger = remember { mutableStateOf(false) }

    val checkForUpdate: () -> Unit = {
        if (shouldCheckForUpdate()) {
            coroutineScope.launch {
                getUpdate(context, navigator) { update ->
                    if (update != null) youtubeVM.setUpdate(update)
                }
            }
        }
    }

    val showDirectionPad: () -> Unit = {
        isDirectionPadVisible.value = true
        directionPadResetKey.value += 1
    }

    // Translate native back-presses to 'escape' button press
    BackHandler {
        if (state.loadingState is LoadingState.Finished)
            navigator.evaluateJavaScript(readRaw(context, R.raw.back_bridge))
        else exitTrigger.value = true
    }

    // Fetch scripts and updates at launch
    LaunchedEffect(Unit) {
        youtubeVM.setScript(fetchScripts(context))
        checkForUpdate()
        showDirectionPad()
    }

    LaunchedEffect(directionPadResetKey.value) {
        if (!isDirectionPadVisible.value) {
            return@LaunchedEffect
        }

        delay(NAV_PAD_AUTO_HIDE_MILLIS)
        isDirectionPadVisible.value = false
    }

    DisposableEffect(lifecycleOwner, context, navigator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkForUpdate()
                showDirectionPad()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (loadingState == LoadingState.Finished && jsScript != null)
        navigator.evaluateJavaScript(jsScript)
    // Auto-update immediately when a newer GitHub release is detected.
    if (updateData != null) UpdateAppScreen(updateData.tagName, updateData.downloadUrl)
    // If exit button is pressed, 'finish the activity' aka 'exit the app'.
    if (exitTrigger.value) activity.finish()

    // This is the loading screen
    val loading = state.loadingState as? LoadingState.Loading
    if (loading != null) SplashLoading(loading.progress)

    Box(modifier = Modifier.fillMaxSize()) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = state,
            navigator = navigator,
            platformWebViewParams = permHandler(context),
            captureBackPresses = false,
            onCreated = { webView ->
                webViewRef.value = webView

                (activity.window).setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )

                configureCookies(webView)
                configureWebSettings(state)

                webView.apply {
                    webViewClient = createLoggingWebViewClient {
                        showDirectionPad()
                    }
                    configureFocusAndTouch(
                        onUserInteraction = showDirectionPad,
                    )

                    addJavascriptInterface(ExitBridge(exitTrigger), "ExitBridge")

                    /*
                    Youtube's content security policy doesn't allow calling fetch on
                    3rd party websites (eg. SponsorBlock api). This bridge counters that
                    handling the requests on the native side. */
                    addJavascriptInterface(NetworkBridge(navigator), "NetworkBridge")

                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    configureScale(isTvDevice)

                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true
                }
            }
        )

        if (isDirectionPadVisible.value) {
            DirectionPadOverlay(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(NAV_PAD_PADDING),
                onUp = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_UP)
                },
                onDown = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_DOWN)
                },
                onLeft = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_LEFT)
                },
                onRight = {
                    showDirectionPad()
                    dispatchDpadKey(webViewRef.value, KeyEvent.KEYCODE_DPAD_RIGHT)
                },
            )
        }
    }
}

@Composable
private fun DirectionPadOverlay(
    modifier: Modifier = Modifier,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavigationButton(symbol = "^", onClick = onUp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavigationButton(symbol = "<", onClick = onLeft)
            NavigationButton(symbol = ">", onClick = onRight)
        }
        NavigationButton(symbol = "v", onClick = onDown)
    }
}

@Composable
private fun NavigationButton(
    symbol: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(NAV_BUTTON_SIZE)
            .clip(CircleShape)
            .background(NAV_BUTTON_COLOR)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = Color.White)
    }
}

private fun shouldCheckForUpdate(): Boolean {
    val isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    return !DISABLE_AUTO_UPDATE_WHEN_DEBUGGER_ATTACHED || !isDebuggerAttached
}

private fun configureCookies(webView: AndroidWebView) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)
    cookieManager.flush()
}

private fun configureWebSettings(state: com.multiplatform.webview.web.WebViewState) {
    state.webSettings.apply {
        customUserAgentString = TV_USER_AGENT
        isJavaScriptEnabled = true

        androidWebSettings.apply {
            useWideViewPort = true
            domStorageEnabled = true
            hideDefaultVideoPoster = true
            mediaPlaybackRequiresUserGesture = false
        }
    }
}

private fun createLoggingWebViewClient(
    onPageNavigated: () -> Unit,
): WebViewClient {
    return object : WebViewClient() {
        override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: android.graphics.Bitmap?) {
            onPageNavigated()
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: AndroidWebView?, url: String?) {
            onPageNavigated()
            super.onPageFinished(view, url)
        }

        override fun shouldInterceptRequest(
            view: AndroidWebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString()
            if (!url.isNullOrBlank()) {
                Log.d(WEBVIEW_DEBUG_TAG, "request ${request?.method ?: "GET"} $url")
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onReceivedError(
            view: AndroidWebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "load error url=${request?.url} code=${error?.errorCode} description=${error?.description}"
            )
            super.onReceivedError(view, request, error)
        }

        override fun onReceivedHttpError(
            view: AndroidWebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "http error url=${request?.url} status=${errorResponse?.statusCode}"
            )
            super.onReceivedHttpError(view, request, errorResponse)
        }

        override fun onReceivedSslError(
            view: AndroidWebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            val debuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
            Log.e(
                WEBVIEW_DEBUG_TAG,
                "ssl error url=${error?.url} primaryError=${error?.primaryError}"
            )
            if (ALLOW_SSL_ERRORS_WHEN_DEBUGGER_ATTACHED && debuggerAttached) {
                Log.w(
                    WEBVIEW_DEBUG_TAG,
                    "proceeding despite ssl error because debugger is attached"
                )
                handler?.proceed()
                return
            }

            handler?.cancel()
        }
    }
}

private fun AndroidWebView.configureFocusAndTouch(onUserInteraction: () -> Unit) {
    isFocusable = true
    isFocusableInTouchMode = true
    requestFocus()

    setOnKeyListener { _, _, event ->
        if (event?.action == KeyEvent.ACTION_DOWN) {
            onUserInteraction()
        }
        false
    }

    setOnGenericMotionListener { _, event ->
        if (event != null) {
            onUserInteraction()
        }
        false
    }

    var lastTouchY = 0f
    var lastTouchX = 0f
    var isDragging = false

    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onUserInteraction()
                lastTouchY = event.y
                lastTouchX = event.x
                isDragging = false
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }

            MotionEvent.ACTION_MOVE -> {
                onUserInteraction()
                val deltaY = (lastTouchY - event.y).toInt()
                val deltaX = (lastTouchX - event.x).toInt()
                if (abs(deltaY) > TV_TOUCH_DRAG_THRESHOLD || abs(deltaX) > TV_TOUCH_DRAG_THRESHOLD) {
                    isDragging = true
                    scrollBy(deltaX, deltaY)
                    lastTouchY = event.y
                    lastTouchX = event.x
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                isDragging
            }

            else -> false
        }
    }
}

private fun AndroidWebView.configureScale(isTvDevice: Boolean) {
    if (isTvDevice) {
        setInitialScale(TV_INITIAL_SCALE)
        return
    }

    val screenWidthPx = resources.displayMetrics.widthPixels.coerceAtLeast(1)
    val fitScale = ((screenWidthPx / SPOOFED_VIEWPORT_WIDTH) * 100f)
        .roundToInt()
        .coerceIn(TOUCH_SCALE_MIN, TOUCH_SCALE_MAX)
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    setInitialScale(fitScale)
    overScrollMode = View.OVER_SCROLL_NEVER
}

private fun dispatchDpadKey(webView: AndroidWebView?, keyCode: Int) {
    webView ?: return
    webView.requestFocus()
    webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}