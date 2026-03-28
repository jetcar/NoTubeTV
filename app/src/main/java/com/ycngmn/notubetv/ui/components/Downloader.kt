package com.ycngmn.notubetv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ycngmn.notubetv.ui.UpdateViewModel


@Composable
fun UpdateAppScreen(tagName: String, downloadUrl: String) {
    val context = LocalContext.current
    val viewModel: UpdateViewModel = viewModel()
    val progress = viewModel.downloadProgress.collectAsState()

    val isShowDownload = remember { mutableStateOf(true) }

    LaunchedEffect(downloadUrl, tagName) {
        viewModel.downloadAndInstall(
            context = context,
            url = downloadUrl,
            isShowDialog = isShowDownload,
            tagName = tagName
        )
    }

    if (isShowDownload.value) {

        Dialog(
            onDismissRequest = { }
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp))
            ) {

                Column(
                    modifier = Modifier.background(Color(0XFF201c1c))
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Downloading $tagName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LinearProgressIndicator(
                        progress = { progress.value / 100F },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = Color.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}