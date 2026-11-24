package com.example.ui.media

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.SchoolRepository
import com.example.model.AppLanguage
import com.example.model.ClassActivityMedia
import com.example.model.MediaType
import com.example.model.StudentAssignment
import com.example.model.StudentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class PickedFileInfo(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationSeconds: Int
)

private fun inspectPickedUri(context: Context, uri: Uri, fallbackName: String): PickedFileInfo {
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: "application/octet-stream"
    var name = fallbackName
    var size = 0L
    cr.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: fallbackName
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
        }
    }
    if (size <= 0L) {
        size = try {
            cr.openAssetFileDescriptor(uri, "r")?.use { it.length.coerceAtLeast(0L) } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
    val durationSec = if (mime.startsWith("video/") || mime.startsWith("audio/")) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            (ms / 1000L).toInt()
        } catch (_: Exception) {
            0
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    } else 0
    return PickedFileInfo(uri, name, mime, size, durationSec)
}

private fun readAndPrepareUploadBytes(
    context: Context,
    info: PickedFileInfo,
    mediaType: MediaType
): ByteArray? {
    return try {
        if (mediaType == MediaType.PHOTO) {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, info.uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, info.uri)
            }
            val maxDim = 1200
            val w = bitmap.width
            val h = bitmap.height
            val scaled = if (w > maxDim || h > maxDim) {
                val ratio = w.toFloat() / h.toFloat()
                val (nw, nh) = if (ratio > 1f) maxDim to (maxDim / ratio).toInt() else (maxDim * ratio).toInt() to maxDim
                Bitmap.createScaledBitmap(bitmap, nw.coerceAtLeast(1), nh.coerceAtLeast(1), true)
            } else bitmap
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        } else {
            context.contentResolver.openInputStream(info.uri)?.use { it.readBytes() }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Requirement 1: Student photo
 * Show photo as a small round image in student list, search results, and on student page.
 * If there is no photo, show first letter of the name.
 */
@Composable
fun StudentAvatar(
    student: StudentRecord,
    size: Dp = 46.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasPhoto = !student.photoUri.isNullOrBlank()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(student.avatarColorHex)),
        contentAlignment = Alignment.Center
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(student.photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo of ${student.fullName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // Requirement 1: If there is no photo, show the first letter of the name
            val initials = student.fullName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first() }
                .joinToString("")

            Text(
                text = initials.ifBlank { student.fullName.take(1).uppercase() },
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Requirement 1: "Insert photo" button when adding or editing a student.
 * Take photo with camera or choose one from gallery.
 * Compresses photo smaller before saving/uploading.
 */
@Composable
fun InsertPhotoButton(
    currentPhotoUri: String?,
    language: AppLanguage,
    onPhotoSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val context = LocalContext.current
    var showSelectDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewUriString by remember { mutableStateOf<String?>(null) }
    var isCompressing by remember { mutableStateOf(false) }
    var compressProgress by remember { mutableFloatStateOf(0f) }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Camera launcher with graceful null/cancellation handling
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            // Flow: Select -> Preview
            previewBitmap = bitmap
            previewUriString = null
            showSelectDialog = false
        } else {
            // User cancelled camera capture gracefully
            cameraErrorMessage = null
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                cameraErrorMessage = if (isUrdu) "کیمرہ ایپ دستیاب نہیں ہے۔ براہ کرم گیلری استعمال کریں۔"
                else "Camera application not available on this device. Please select from gallery."
            }
        } else {
            cameraErrorMessage = if (isUrdu) "کیمرہ کی اجازت درکار ہے۔ براہ کرم اجازت دیں یا گیلری منتخب کریں۔"
            else "Camera permission is required. Please grant permission or choose from gallery."
        }
    }

    // Gallery Photo Picker (Zero-permission modern Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Flow: Select -> Preview
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                previewBitmap = bitmap
                previewUriString = null
            } catch (e: Exception) {
                previewBitmap = null
                previewUriString = uri.toString()
            }
            showSelectDialog = false
        }
    }

    OutlinedButton(
        onClick = {
            cameraErrorMessage = null
            showSelectDialog = true
        },
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.testTag("btn_insert_photo")
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (currentPhotoUri.isNullOrBlank()) {
                if (isUrdu) "تصویر شامل کریں (Insert Photo)" else "Insert Photo"
            } else {
                if (isUrdu) "تصویر تبدیل کریں" else "Change Photo"
            },
            style = MaterialTheme.typography.labelMedium
        )
    }

    // 1. SELECT SOURCE DIALOG (Camera vs Gallery)
    if (showSelectDialog) {
        AlertDialog(
            onDismissRequest = { showSelectDialog = false },
            title = {
                Text(
                    text = if (isUrdu) "طالب علم کی تصویر منتخب کریں" else "Select Student Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isUrdu) "کیمرہ سے نئی تصویر بنائیں یا گیلری سے منتخب کریں۔ تصویر کی تصدیق اور پریویو کے بعد محفوظ کی جائے گی۔"
                        else "Take a photo with camera or choose one from gallery. You can preview and confirm before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (cameraErrorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cameraErrorMessage!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Choice 1: Camera
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val hasCameraPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPerm) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                } catch (e: Exception) {
                                    cameraErrorMessage = if (isUrdu) "کیمرہ کھولنے میں مسئلہ پیش آیا۔ گیلری منتخب کریں۔"
                                    else "Could not open camera. Please choose from gallery."
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isUrdu) "کیمرہ سے تصویر کھینچیں" else "Take Photo with Camera",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isUrdu) "ڈیوائس کا کیمرہ استعمال کریں" else "Capture a fresh portrait safely",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Choice 2: Gallery
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isUrdu) "گیلری سے تصویر منتخب کریں" else "Choose from Gallery",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isUrdu) "ڈیوائس فوٹو البم سے منتخب کریں" else "Select from device photos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Choice 3: Remove photo (if exists)
                    if (!currentPhotoUri.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                showSelectDialog = false
                                onPhotoSelected(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUrdu) "تصویر ہٹائیں (پہلا حرف دکھائیں)" else "Remove Photo (Revert to initial)",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelectDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // 2. FLOW: PREVIEW & CONFIRM DIALOG (Select -> Preview -> Confirm -> Save -> Immediate UI Display -> Persistent storage)
    if (previewBitmap != null || previewUriString != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isCompressing) {
                    previewBitmap = null
                    previewUriString = null
                }
            },
            title = {
                Text(
                    text = if (isUrdu) "تصویر کا جائزہ اور تصدیق (Photo Preview)" else "Preview & Confirm Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isUrdu) "منتخب تصویر کا جائزہ لیں۔ 'تصدیق و محفوظ کریں' پر ٹیپ کرنے سے یہ محفوظ ہو جائے گی۔"
                        else "Review the selected photo below. Tap 'Confirm & Save' to apply immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Round Photo Preview Frame
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else if (previewUriString != null) {
                            AsyncImage(
                                model = previewUriString,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                    }

                    if (isCompressing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isUrdu) "تصویر کو کمپریس اور محفوظ کیا جا رہا ہے..." else "Optimizing & saving photo...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { compressProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCompressing = true
                        compressProgress = 0.4f
                        coroutineScope.launch {
                            val savedPhotoData = withContext(Dispatchers.Default) {
                                if (previewBitmap != null) {
                                    MediaUtils.compressBitmapToBase64(previewBitmap!!, maxDimension = 800)
                                } else {
                                    previewUriString ?: ""
                                }
                            }
                            compressProgress = 1.0f
                            isCompressing = false
                            previewBitmap = null
                            previewUriString = null
                            onPhotoSelected(savedPhotoData.ifBlank { null })
                        }
                    },
                    enabled = !isCompressing,
                    modifier = Modifier.testTag("btn_confirm_photo_upload")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isUrdu) "تصدیق و محفوظ کریں" else "Confirm & Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        previewBitmap = null
                        previewUriString = null
                        showSelectDialog = true
                    },
                    enabled = !isCompressing
                ) {
                    Text(if (isUrdu) "دوسری منتخب کریں" else "Change")
                }
            }
        )
    }
}

/**
 * Requirement 2: Class Activity Media Upload Dialog
 * Triggered by: "Add photo", "Add video", "Add audio"
 * Enforces parent consent, video limits (<= 2min & <= 50MB), and shows upload progress.
 */
@Composable
fun AddClassActivityMediaDialog(
    initialMediaType: MediaType,
    student: StudentRecord,
    currentUser: UserProfile,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSaveMedia: (ClassActivityMedia) -> Unit,
    repository: SchoolRepository? = null
) {
    val isUrdu = language == AppLanguage.URDU
    val context = LocalContext.current
    var selectedMediaType by remember { mutableStateOf(initialMediaType) }
    var activityTitle by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var pickedFile by remember { mutableStateOf<PickedFileInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedFile = inspectPickedUri(context, uri, "activity_photo_${student.permanentStudentId}.jpg")
            validationError = null
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedFile = inspectPickedUri(context, uri, "activity_video_${student.permanentStudentId}.mp4")
            validationError = null
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedFile = inspectPickedUri(context, uri, "activity_audio_${student.permanentStudentId}.mp3")
            validationError = null
        }
    }

    // Check parent consent (Requirement 5)
    val hasConsent = student.parentMediaConsentGiven

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedMediaType) {
                        MediaType.PHOTO -> if (isUrdu) "کلاس سرگرمی: تصویر شامل کریں" else "Add Activity Photo"
                        MediaType.VIDEO -> if (isUrdu) "کلاس سرگرمی: ویڈیو شامل کریں" else "Add Activity Video"
                        MediaType.AUDIO -> if (isUrdu) "کلاس سرگرمی: آڈیو ریکارڈنگ" else "Add Activity Audio"
                        MediaType.DOCUMENT -> if (isUrdu) "دستاویز شامل کریں" else "Add Document"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, enabled = !isUploading) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Media Type Switcher Tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(MediaType.PHOTO, MediaType.VIDEO, MediaType.AUDIO).forEach { type ->
                            val isSel = selectedMediaType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isUploading) {
                                        selectedMediaType = type
                                        pickedFile = null
                                        validationError = null
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (type) {
                                            MediaType.PHOTO -> Icons.Default.CameraAlt
                                            MediaType.VIDEO -> Icons.Default.Videocam
                                            MediaType.AUDIO -> Icons.Default.Mic
                                            MediaType.DOCUMENT -> Icons.Default.PictureAsPdf
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = type.getDisplayName(isUrdu),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Requirement 5: Parent Consent Warning
                if (!hasConsent) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isUrdu) "والدین کی طرف سے اس طالب علم کی فوٹو یا ویڈیو محفوظ کرنے کی اجازت نہیں دی گئی ہے۔ میڈیا محفوظ کرنے کیلئے والدین کی اجازت ضروری ہے۔"
                                    else "Parental media consent has not been granted for this child. Photos and videos cannot be stored until consent is agreed in student settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Activity Title input
                item {
                    OutlinedTextField(
                        value = activityTitle,
                        onValueChange = { activityTitle = it },
                        label = { Text(if (isUrdu) "سرگرمی کا عنوان (Activity Title)" else "Activity Title") },
                        placeholder = { Text(if (isUrdu) "مثلاً سائنس پروجیکٹ، اردو تقریر، مباحثہ" else "e.g. Science Experiment, Debate, Math Olympiad") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_activity_title"),
                        singleLine = true
                    )
                }

                // Short caption input (Requirement 2)
                item {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text(if (isUrdu) "مختصر تفصیل / کیپشن (Short Caption)" else "Short Caption") },
                        placeholder = { Text(if (isUrdu) "طالب علم کی کارکردگی اور ریمارکس" else "Observations, performance remarks") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_media_caption"),
                        maxLines = 3
                    )
                }

                // File Picker Button & Metrics (Requirement 2 & 4)
                item {
                    OutlinedButton(
                        onClick = {
                            when (selectedMediaType) {
                                MediaType.PHOTO -> photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                                MediaType.VIDEO -> videoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                                MediaType.AUDIO -> audioPicker.launch("audio/*")
                                MediaType.DOCUMENT -> audioPicker.launch("*/*")
                            }
                        },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth().testTag("btn_pick_activity_media")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pickedFile?.fileName ?: when (selectedMediaType) {
                                MediaType.PHOTO -> if (isUrdu) "ڈیوائس سے تصویر منتخب کریں" else "Choose Photo from Device"
                                MediaType.VIDEO -> if (isUrdu) "ڈیوائس سے ویڈیو منتخب کریں" else "Choose Video from Device"
                                MediaType.AUDIO -> if (isUrdu) "ڈیوائس سے آڈیو منتخب کریں" else "Choose Audio from Device"
                                MediaType.DOCUMENT -> if (isUrdu) "فائل منتخب کریں" else "Choose File"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isUrdu) "فائل کا سائز:" else "File Size:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = pickedFile?.let { MediaUtils.formatFileSize(it.sizeBytes) }
                                        ?: (if (isUrdu) "کوئی فائل منتخب نہیں" else "No file selected"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if ((selectedMediaType == MediaType.VIDEO || selectedMediaType == MediaType.AUDIO) && pickedFile != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isUrdu) "دورانیہ:" else "Duration:",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = MediaUtils.formatDuration(pickedFile!!.durationSeconds) + " / Max 02:00",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isUrdu) "ویڈیو کی زیادہ سے زیادہ حد: 2 منٹ اور 50 ایم بی۔ تمام فائلیں پرائیویٹ کلاؤڈ میں محفوظ ہوں گی۔"
                                else "Limits: Max 2 minutes & 50 MB for videos. Private encrypted cloud storage linked to ${student.permanentStudentId}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Validation Error Banner
                if (validationError != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = validationError!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Upload Progress (Requirement 4)
                if (isUploading) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isUrdu) "DBUS کلاؤڈ اسٹوریج پر اپ لوڈ جاری ہے (${(uploadProgress * 100).toInt()}%)..."
                                else "Uploading to DBUS Cloud Storage (${(uploadProgress * 100).toInt()}%)...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!hasConsent) {
                        validationError = if (isUrdu) "والدین کی اجازت کے بغیر میڈیا اسٹور نہیں ہو سکتا۔"
                        else "Parental consent is required before storing child media."
                        return@Button
                    }
                    val info = pickedFile
                    if (info == null) {
                        validationError = if (isUrdu) "براہ کرم پہلے اپنی ڈیوائس سے فائل منتخب کریں۔"
                        else "Please select a file from your device before uploading."
                        return@Button
                    }

                    // Validate raw size & video duration limits before reading bytes
                    val (isValid, errorMsg) = MediaUtils.validateMediaUpload(
                        mediaType = selectedMediaType,
                        fileSizeBytes = info.sizeBytes,
                        durationSeconds = info.durationSeconds,
                        isUrdu = isUrdu
                    )
                    if (!isValid) {
                        validationError = errorMsg
                        return@Button
                    }

                    isUploading = true
                    uploadProgress = 0.25f
                    validationError = null
                    coroutineScope.launch {
                        val bytes = withContext(Dispatchers.IO) {
                            readAndPrepareUploadBytes(context, info, selectedMediaType)
                        }
                        if (bytes == null || bytes.isEmpty()) {
                            isUploading = false
                            validationError = if (isUrdu) "منتخب فائل پڑھی نہیں جا سکی۔"
                            else "Failed to read the selected file from device storage."
                            return@launch
                        }
                        uploadProgress = 0.55f
                        val mimeForUpload = if (selectedMediaType == MediaType.PHOTO) "image/jpeg" else info.mimeType
                        val uploadedUri = if (repository != null) {
                            val uploadRes = repository.uploadToPrivateStorage(
                                bucket = "student-media",
                                studentPermanentId = student.permanentStudentId,
                                fileName = info.fileName,
                                bytes = bytes,
                                contentType = mimeForUpload
                            )
                            if (uploadRes.isFailure) {
                                isUploading = false
                                validationError = uploadRes.exceptionOrNull()?.message
                                    ?: if (isUrdu) "کلاؤڈ اسٹوریج اپ لوڈ ناکام ہو گیا۔" else "Cloud storage upload failed."
                                return@launch
                            }
                            uploadRes.getOrNull().orEmpty()
                        } else {
                            info.uri.toString()
                        }

                        uploadProgress = 1.0f
                        isUploading = false
                        val today = repository?.todayDate()
                            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                        val newMedia = ClassActivityMedia(
                            id = "act_" + UUID.randomUUID().toString().take(6),
                            studentPermanentId = student.permanentStudentId,
                            mediaType = selectedMediaType,
                            fileUri = uploadedUri,
                            caption = caption.ifBlank { activityTitle.ifBlank { info.fileName } },
                            activityName = activityTitle.ifBlank { "Classroom Activity" },
                            teacherName = currentUser.name,
                            teacherId = currentUser.id,
                            date = today,
                            fileSizeBytes = bytes.size.toLong(),
                            durationSeconds = info.durationSeconds,
                            isCloudStored = repository != null
                        )
                        onSaveMedia(newMedia)
                    }
                },
                enabled = !isUploading && hasConsent,
                modifier = Modifier.testTag("btn_save_activity_media")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isUrdu) "اپ لوڈ و محفوظ کریں" else "Upload & Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text(if (isUrdu) "منسوخ" else "Cancel")
            }
        }
    )
}

/**
 * Requirement 2: Class activity media gallery on student page
 * Tap to open full screen. Videos play inside the app.
 * Teacher who added file or Administrator can delete it.
 */
@Composable
fun ClassActivityMediaGallery(
    mediaList: List<ClassActivityMedia>,
    currentUser: UserProfile?,
    language: AppLanguage,
    onAddMediaClick: (MediaType) -> Unit,
    onDeleteMedia: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEditMedia: ((ClassActivityMedia) -> Unit)? = null
) {
    val isUrdu = language == AppLanguage.URDU
    var selectedFilter by remember { mutableStateOf<MediaType?>(null) }
    var fullScreenItem by remember { mutableStateOf<ClassActivityMedia?>(null) }
    var deleteCandidate by remember { mutableStateOf<ClassActivityMedia?>(null) }

    val canAdd = currentUser?.role == UserRole.Teacher ||
        currentUser?.role == UserRole.Administrator ||
        currentUser?.role == UserRole.SuperAdmin

    val filteredList = remember(mediaList, selectedFilter) {
        if (selectedFilter == null) mediaList else mediaList.filter { it.mediaType == selectedFilter }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isUrdu) "کلاس سرگرمی میڈیا گیلری" else "Class Activity Media Gallery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUrdu) "${mediaList.size} فائلیں (تصاویر، ویڈیوز اور آڈیو ریکارڈنگز)"
                    else "${mediaList.size} media files (photos, videos & audio clips)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Quick Add Buttons for Teachers / Admin (Requirement 2)
            if (canAdd) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onAddMediaClick(MediaType.PHOTO) },
                        modifier = Modifier.testTag("btn_quick_add_photo")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { onAddMediaClick(MediaType.VIDEO) },
                        modifier = Modifier.testTag("btn_quick_add_video")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Add Video", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { onAddMediaClick(MediaType.AUDIO) },
                        modifier = Modifier.testTag("btn_quick_add_audio")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Add Audio", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips (All, Photos, Videos, Audio)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text(if (isUrdu) "تمام (${mediaList.size})" else "All (${mediaList.size})") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == MediaType.PHOTO,
                    onClick = { selectedFilter = MediaType.PHOTO },
                    label = { Text(if (isUrdu) "تصاویر" else "Photos") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == MediaType.VIDEO,
                    onClick = { selectedFilter = MediaType.VIDEO },
                    label = { Text(if (isUrdu) "ویڈیوز" else "Videos") },
                    leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == MediaType.AUDIO,
                    onClick = { selectedFilter = MediaType.AUDIO },
                    label = { Text(if (isUrdu) "آڈیو" else "Audio") },
                    leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredList.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isUrdu) "اس زمرے میں فی الحال کوئی میڈیا فائل موجود نہیں ہے۔"
                        else "No activity media uploaded in this category yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Media Grid/Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredList.forEach { item ->
                    MediaItemCard(
                        media = item,
                        currentUser = currentUser,
                        language = language,
                        onClick = { fullScreenItem = item },
                        onEdit = onEditMedia?.let { editCb -> { editCb(item) } },
                        onDelete = { deleteCandidate = item }
                    )
                }
            }
        }
    }

    // Full Screen Viewer Dialog
    fullScreenItem?.let { media ->
        FullScreenMediaViewerDialog(
            media = media,
            currentUser = currentUser,
            language = language,
            onDismiss = { fullScreenItem = null },
            onDelete = {
                onDeleteMedia(media.id)
                fullScreenItem = null
            }
        )
    }

    // Delete Confirmation Dialog (Requirement 2)
    deleteCandidate?.let { media ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(if (isUrdu) "میڈیا فائل ڈیلیٹ کریں؟" else "Delete Media File?")
            },
            text = {
                Text(
                    text = if (isUrdu) "کیا آپ واقعی سرگرمی '${media.activityName}' کی اس فائل کو ڈیلیٹ کرنا چاہتے ہیں؟"
                    else "Are you sure you want to delete '${media.activityName}'? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMedia(media.id)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isUrdu) "ڈیلیٹ کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

/**
 * Single card representing a media item (Photo, Video, Audio)
 */
@Composable
fun MediaItemCard(
    media: ClassActivityMedia,
    currentUser: UserProfile?,
    language: AppLanguage,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    val isUrdu = language == AppLanguage.URDU
    val canDelete = currentUser?.role == UserRole.Administrator ||
            currentUser?.role == UserRole.SuperAdmin ||
            (currentUser?.role == UserRole.Teacher && media.teacherId == currentUser.id)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("media_card_${media.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media Visual Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (media.mediaType) {
                    MediaType.PHOTO -> {
                        AsyncImage(
                            model = media.fileUri,
                            contentDescription = media.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MediaType.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            if (media.durationSeconds > 0) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = MediaUtils.formatDuration(media.durationSeconds),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                    MediaType.AUDIO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio recording",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    MediaType.DOCUMENT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Document",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Information Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = media.activityName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = media.mediaType.getDisplayName(isUrdu),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = media.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 ${media.date} • ${media.teacherName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (canDelete) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onEdit != null) {
                                com.example.ui.components.DbusEditIconButton(
                                    onClick = onEdit,
                                    contentDescription = if (isUrdu) "ترمیم" else "Edit Caption"
                                )
                            }
                            com.example.ui.components.DbusDeleteIconButton(
                                onClick = onDelete,
                                contentDescription = if (isUrdu) "حذف کریں" else "Delete"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Requirement 2: Full-screen viewer dialog
 * Videos play inside the app via native VideoView with playback controls!
 */
@Composable
fun FullScreenMediaViewerDialog(
    media: ClassActivityMedia,
    currentUser: UserProfile?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val isUrdu = language == AppLanguage.URDU
    val context = LocalContext.current
    val canDelete = currentUser?.role == UserRole.Administrator ||
            currentUser?.role == UserRole.SuperAdmin ||
            (currentUser?.role == UserRole.Teacher && media.teacherId == currentUser.id)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.activityName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${media.mediaType.getDisplayName(isUrdu)} • ${media.date}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (media.mediaType) {
                    MediaType.PHOTO -> {
                        AsyncImage(
                            model = media.fileUri,
                            contentDescription = media.caption,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black)
                        )
                    }
                    MediaType.VIDEO -> {
                        var videoPlaybackError by remember(media.fileUri) { mutableStateOf<String?>(null) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (media.fileUri.isBlank() || videoPlaybackError != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = videoPlaybackError
                                            ?: if (isUrdu) "ویڈیو فائل دستیاب نہیں ہے۔" else "Video playback unavailable.",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            val mediaController = MediaController(ctx)
                                            mediaController.setAnchorView(this)
                                            setMediaController(mediaController)
                                            setOnErrorListener { _, _, _ ->
                                                videoPlaybackError = if (isUrdu) "ویڈیو چلانے میں خرابی پیش آئی۔"
                                                else "Unable to play this video inside the app."
                                                true
                                            }
                                            try {
                                                setVideoURI(Uri.parse(media.fileUri))
                                                start()
                                            } catch (e: Exception) {
                                                videoPlaybackError = e.message ?: "Video load error"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    MediaType.AUDIO -> {
                        var isPlaying by remember(media.fileUri) { mutableStateOf(false) }
                        var isPrepared by remember(media.fileUri) { mutableStateOf(false) }
                        var audioError by remember(media.fileUri) { mutableStateOf<String?>(null) }
                        val mediaPlayer = remember(media.fileUri) { MediaPlayer() }

                        DisposableEffect(media.fileUri) {
                            if (media.fileUri.isNotBlank()) {
                                try {
                                    mediaPlayer.setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .build()
                                    )
                                    mediaPlayer.setDataSource(context, Uri.parse(media.fileUri))
                                    mediaPlayer.setOnPreparedListener { isPrepared = true }
                                    mediaPlayer.setOnCompletionListener { isPlaying = false }
                                    mediaPlayer.setOnErrorListener { _, _, _ ->
                                        audioError = if (isUrdu) "آڈیو فائل چلانے میں خرابی پیش آئی۔"
                                        else "Unable to play this audio file."
                                        isPlaying = false
                                        true
                                    }
                                    mediaPlayer.prepareAsync()
                                } catch (e: Exception) {
                                    audioError = e.message ?: "Audio load error"
                                }
                            } else {
                                audioError = if (isUrdu) "آڈیو فائل دستیاب نہیں ہے۔" else "Audio file unavailable."
                            }
                            onDispose {
                                try {
                                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                } catch (_: Exception) {
                                }
                                mediaPlayer.release()
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isUrdu) "ان ایپ آڈیو پلیئر" else "In-App Audio Player",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Duration: ${MediaUtils.formatDuration(media.durationSeconds)} • ${MediaUtils.formatFileSize(media.fileSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                if (audioError != null) {
                                    Text(
                                        text = audioError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            if (isPrepared) {
                                                if (isPlaying) {
                                                    mediaPlayer.pause()
                                                    isPlaying = false
                                                } else {
                                                    mediaPlayer.start()
                                                    isPlaying = true
                                                }
                                            }
                                        },
                                        enabled = isPrepared
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when {
                                                !isPrepared -> if (isUrdu) "تیار ہو رہا ہے..." else "Preparing..."
                                                isPlaying -> if (isUrdu) "روکیں" else "Pause"
                                                else -> if (isUrdu) "چلائیں" else "Play Audio"
                                            }
                                        )
                                    }
                                    if (isPlaying) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                    MediaType.DOCUMENT -> {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isUrdu) "پی ڈی ایف دستاویز تیار ہے" else "PDF Document Attached",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Caption
                Text(
                    text = "📝 ${media.caption}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cloud Storage & Teacher Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUrdu) "محفوظ کلاؤڈ اسٹوریج" else "DBUS Cloud • Encrypted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (isUrdu) "استاد: ${media.teacherName}" else "Teacher: ${media.teacherName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (canDelete) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isUrdu) "ڈیلیٹ کریں" else "Delete Media")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        },
        dismissButton = {
            if (canDelete) {
                TextButton(onClick = onDismiss) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        }
    )
}

/**
 * Requirement 3: Student Assignments section with photo, PDF, audio, and video upload options.
 */
@Composable
fun StudentAssignmentsSection(
    assignments: List<StudentAssignment>,
    student: StudentRecord,
    currentUser: UserProfile?,
    language: AppLanguage,
    onSubmitAssignment: (String, MediaType, String, String, Long) -> Unit,
    modifier: Modifier = Modifier,
    repository: SchoolRepository? = null,
    onEditAssignment: ((StudentAssignment) -> Unit)? = null,
    onDeleteAssignment: ((StudentAssignment) -> Unit)? = null
) {
    val isUrdu = language == AppLanguage.URDU
    var submitDialogAssignment by remember { mutableStateOf<StudentAssignment?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isUrdu) "طالب علم اسائنمنٹس و ہوم ورک" else "Student Assignments & Submissions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUrdu) "فوٹو، پی ڈی ایف، آڈیو یا ویڈیو اپ لوڈ کی سہولت دستیاب ہے"
                    else "Upload solutions via Photo, PDF, Audio, or Video",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (assignments.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    text = if (isUrdu) "کوئی اسائنمنٹ زیر التواء نہیں ہے۔" else "No assignments scheduled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                assignments.forEach { assignment ->
                    AssignmentItemCard(
                        assignment = assignment,
                        language = language,
                        onOpenSubmit = { submitDialogAssignment = assignment },
                        onEdit = onEditAssignment?.let { cb -> { cb(assignment) } },
                        onDelete = onDeleteAssignment?.let { cb -> { cb(assignment) } }
                    )
                }
            }
        }
    }

    // Submission Dialog with Photo, PDF, Audio, Video
    submitDialogAssignment?.let { assign ->
        SubmitAssignmentDialog(
            assignment = assign,
            student = student,
            language = language,
            repository = repository,
            onDismiss = { submitDialogAssignment = null },
            onSubmit = { mediaType, fileUri, fileName, sizeBytes ->
                onSubmitAssignment(assign.id, mediaType, fileUri, fileName, sizeBytes)
                submitDialogAssignment = null
            }
        )
    }
}

@Composable
fun AssignmentItemCard(
    assignment: StudentAssignment,
    language: AppLanguage,
    onOpenSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isUrdu = language == AppLanguage.URDU

    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { base -> if (onEdit != null) base.clickable(onClick = onEdit) else base },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${assignment.subject} • Due: ${assignment.dueDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (assignment.isSubmitted) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (assignment.isSubmitted) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = if (assignment.isSubmitted) Color(0xFF2E7D32) else Color(0xFFF57C00),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (assignment.isSubmitted) {
                                if (isUrdu) "جمع شدہ (${assignment.grade ?: "A"})" else "Submitted (${assignment.grade ?: "A"})"
                            } else {
                                if (isUrdu) "زیر التواء" else "Pending"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (assignment.isSubmitted) Color(0xFF2E7D32) else Color(0xFFF57C00)
                        )
                    }
                }
            }

            if (assignment.isSubmitted) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (assignment.mediaType) {
                                MediaType.PHOTO -> Icons.Default.Image
                                MediaType.VIDEO -> Icons.Default.Videocam
                                MediaType.AUDIO -> Icons.Default.Audiotrack
                                MediaType.DOCUMENT -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.UploadFile
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${assignment.fileName ?: "submission_file"} • ${MediaUtils.formatFileSize(assignment.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenSubmit,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (assignment.isSubmitted) Icons.Default.UploadFile else Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (assignment.isSubmitted) {
                            if (isUrdu) "نئی فائل اپ لوڈ کریں" else "Update Submission File"
                        } else {
                            if (isUrdu) "اسائنمنٹ اپ لوڈ کریں" else "Submit Assignment"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onEdit != null) {
                    com.example.ui.components.DbusEditIconButton(
                        onClick = onEdit,
                        contentDescription = if (isUrdu) "ترمیم" else "Edit Assignment",
                        modifier = Modifier.testTag("btn_edit_assignment_${assignment.id}")
                    )
                }
                if (onDelete != null) {
                    com.example.ui.components.DbusDeleteIconButton(
                        onClick = onDelete,
                        contentDescription = if (isUrdu) "حذف" else "Delete Assignment",
                        modifier = Modifier.testTag("btn_delete_assignment_${assignment.id}")
                    )
                }
            }
        }
    }
}

/**
 * Dialog supporting uploads for: Photo, PDF, Audio, Video (Requirement 3)
 */
@Composable
fun SubmitAssignmentDialog(
    assignment: StudentAssignment,
    student: StudentRecord,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (MediaType, String, String, Long) -> Unit,
    repository: SchoolRepository? = null
) {
    val isUrdu = language == AppLanguage.URDU
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(MediaType.DOCUMENT) }
    var pickedFile by remember { mutableStateOf<PickedFileInfo?>(null) }
    var fileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val assignmentFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val info = inspectPickedUri(context, uri, "submission_${student.permanentStudentId}")
            pickedFile = info
            fileName = info.fileName
            uploadError = null
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Text(
                text = if (isUrdu) "اسائنمنٹ اپ لوڈ کریں" else "Upload Assignment Submission",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = assignment.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isUrdu) "اپ لوڈ فارمیٹ منتخب کریں (فوٹو، پی ڈی ایف، آڈیو یا ویڈیو):"
                    else "Select upload format (Photo, PDF, Audio, or Video):",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(MediaType.DOCUMENT, MediaType.PHOTO, MediaType.AUDIO, MediaType.VIDEO).forEach { type ->
                        val isSel = selectedType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isUploading) {
                                    selectedType = type
                                    pickedFile = null
                                    fileName = ""
                                    uploadError = null
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        MediaType.DOCUMENT -> Icons.Default.PictureAsPdf
                                        MediaType.PHOTO -> Icons.Default.Image
                                        MediaType.AUDIO -> Icons.Default.Audiotrack
                                        MediaType.VIDEO -> Icons.Default.Movie
                                    },
                                    contentDescription = null,
                                    tint = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (type) {
                                        MediaType.DOCUMENT -> "PDF"
                                        MediaType.PHOTO -> "Photo"
                                        MediaType.AUDIO -> "Audio"
                                        MediaType.VIDEO -> "Video"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val mime = when (selectedType) {
                            MediaType.DOCUMENT -> "application/pdf"
                            MediaType.PHOTO -> "image/*"
                            MediaType.AUDIO -> "audio/*"
                            MediaType.VIDEO -> "video/*"
                        }
                        assignmentFilePicker.launch(mime)
                    },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth().testTag("btn_pick_assignment_file")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pickedFile?.let { "${it.fileName} (${MediaUtils.formatFileSize(it.sizeBytes)})" }
                            ?: if (isUrdu) "ڈیوائس سے فائل منتخب کریں" else "Choose File from Device",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(if (isUrdu) "فائل کا نام" else "File Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUploading
                )

                uploadError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isUrdu) "کلاؤڈ پر اپ لوڈ ہو رہا ہے (${(uploadProgress * 100).toInt()}%)..." else "Uploading solution to DBUS Cloud (${(uploadProgress * 100).toInt()}%)...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val info = pickedFile
                    if (info == null) {
                        uploadError = if (isUrdu) "براہ کرم پہلے اپنی ڈیوائس سے فائل منتخب کریں۔"
                        else "Please choose a file from your device first."
                        return@Button
                    }
                    val (isValid, errMsg) = MediaUtils.validateMediaUpload(
                        mediaType = selectedType,
                        fileSizeBytes = info.sizeBytes,
                        durationSeconds = info.durationSeconds,
                        isUrdu = isUrdu
                    )
                    if (!isValid) {
                        uploadError = errMsg
                        return@Button
                    }

                    isUploading = true
                    uploadProgress = 0.25f
                    uploadError = null
                    coroutineScope.launch {
                        val bytes = withContext(Dispatchers.IO) {
                            readAndPrepareUploadBytes(context, info, selectedType)
                        }
                        if (bytes == null || bytes.isEmpty()) {
                            isUploading = false
                            uploadError = if (isUrdu) "منتخب فائل پڑھی نہیں جا سکی۔"
                            else "Failed to read the selected file."
                            return@launch
                        }
                        uploadProgress = 0.6f
                        val effectiveName = fileName.ifBlank { info.fileName }
                        val effectiveMime = if (selectedType == MediaType.PHOTO) "image/jpeg" else info.mimeType
                        val uploadedUri = if (repository != null) {
                            val res = repository.uploadToPrivateStorage(
                                bucket = "assignments",
                                studentPermanentId = student.permanentStudentId,
                                fileName = effectiveName,
                                bytes = bytes,
                                contentType = effectiveMime
                            )
                            if (res.isFailure) {
                                isUploading = false
                                uploadError = res.exceptionOrNull()?.message
                                    ?: if (isUrdu) "کلاؤڈ اسٹوریج اپ لوڈ ناکام ہو گیا۔" else "Upload failed."
                                return@launch
                            }
                            res.getOrNull().orEmpty()
                        } else {
                            info.uri.toString()
                        }
                        uploadProgress = 1.0f
                        isUploading = false
                        onSubmit(selectedType, uploadedUri, effectiveName, bytes.size.toLong())
                    }
                },
                enabled = !isUploading
            ) {
                Text(if (isUrdu) "جمع کروائیں" else "Submit File")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text(if (isUrdu) "منسوخ" else "Cancel")
            }
        }
    )
}
