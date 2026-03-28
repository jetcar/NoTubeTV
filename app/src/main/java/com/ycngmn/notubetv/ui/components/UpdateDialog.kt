package com.ycngmn.notubetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.multiplatform.webview.web.WebViewNavigator
import com.ycngmn.notubetv.utils.ReleaseData
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(releaseData: ReleaseData, navigator: WebViewNavigator) {
    val isShowDialog = rememberSaveable { mutableStateOf(true) }
    val isDownload = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    if (isDownload.value)
        UpdateAppScreen(releaseData.tagName, releaseData.downloadUrl)

    if (isShowDialog.value) {
        Dialog(
            onDismissRequest = { isShowDialog.value = false }
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(2.dp, Color.White,RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.background(Color(0XFF201c1c))
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "VidroX - ${releaseData.tagName} available!",
                        modifier = Modifier.padding(bottom = 20.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )

                   Text(
                       text = AnnotatedString.fromHtml(releaseData.changelog),
                       modifier = Modifier.padding(horizontal = 20.dp),
                       color = Color.White,
                       fontSize = 16.sp
                   )

                    Row {
                        YTButton("Cancel") { isShowDialog.value = false }

                        Spacer(modifier = Modifier.width(10.dp))

                        YTButton(
                            "Update Now",
                            Modifier.focusRequester(focusRequester)
                        ) {
                            isDownload.value = true
                            isShowDialog.value = false
                        }

                        Spacer(modifier = Modifier.weight(1F))

                        YTButton("Skip this version") {
                            navigator.evaluateJavaScript(
                                "configWrite('skipVersionName', '${releaseData.tagName}')"
                            ) { isShowDialog.value = false }
                        }
                    }
                }
            }
        }
        // Request focus for Update button.
        LaunchedEffect(Unit) {
            scope.launch { focusRequester.requestFocus() }
        }
    }
}

@Composable
fun YTButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.colors(
            containerColor = Color.DarkGray.copy(alpha = 0.5F),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) { Text(text = text, fontSize = 16.sp) }
}