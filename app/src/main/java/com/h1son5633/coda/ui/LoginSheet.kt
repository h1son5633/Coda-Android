package com.h1son5633.coda.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay

private fun qrBitmap(content: String, size: Int = 540): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(
                x, y,
                if (matrix.get(x, y)) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            )
        }
    }
    return bitmap
}

/** NetEase QR login sheet, dimensions and copy copied from the original's LoginSheet. */
@Composable
fun LoginSheet(vm: AppViewModel, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val login by vm.login.collectAsState()

    LaunchedEffect(Unit) { vm.startQrLogin() }
    LaunchedEffect(login.qrStatus) {
        if (login.qrStatus == QrStatus.SUCCESS) {
            delay(700)
            onSuccess()
        }
    }

    Dialog(onDismissRequest = {
        vm.cancelQrLogin()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(CodaTheme.Radius.panel),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "登录网易云音乐",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "使用网易云音乐 App 扫码登录",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                // QR frame: white card 208, code 180.
                Box(
                    modifier = Modifier
                        .size(208.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp))
                        .background(Color.White, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val content = login.qrContent
                    if (content != null) {
                        val bitmap = remember(content) { qrBitmap(content) }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "登录二维码",
                            modifier = Modifier.size(180.dp)
                        )
                    } else {
                        CircularProgressIndicator(color = CodaTheme.accent)
                    }
                    if (login.qrStatus == QrStatus.SCANNED) {
                        OverlayCard {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("已扫码", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "请在手机上确认",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (login.qrStatus == QrStatus.EXPIRED) {
                        OverlayCard {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = CodaTheme.accent,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("二维码已失效", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { vm.startQrLogin() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CodaTheme.accent
                                )
                            ) {
                                Text("刷新", color = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    when (login.qrStatus) {
                        QrStatus.IDLE, QrStatus.WAITING ->
                            if (login.qrContent == null) "正在获取二维码…"
                            else "打开网易云音乐 App，扫一扫登录"
                        QrStatus.SCANNED -> "等待手机确认…"
                        QrStatus.EXPIRED -> "二维码已失效，请刷新"
                        QrStatus.SUCCESS -> "登录成功！"
                        QrStatus.FAILED -> "获取二维码失败，请重试"
                    },
                    fontSize = 13.sp,
                    color = if (login.qrStatus == QrStatus.FAILED) CodaTheme.accent
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (login.qrStatus == QrStatus.FAILED) {
                    TextButton(onClick = { vm.startQrLogin() }) {
                        Text("重试", color = CodaTheme.accent)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "只有一台设备？截图二维码，到网易云音乐 App 的扫一扫里选择相册识别，然后回到这里即可",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = {
                    vm.cancelQrLogin()
                    onDismiss()
                }) {
                    Text(
                        "取消",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun OverlayCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
