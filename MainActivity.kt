package vn.sitephotopro

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : androidx.activity.ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var deviceInput: EditText
    private lateinit var projectInput: EditText
    private lateinit var statusText: TextView
    private var imageCapture: ImageCapture? = null
    private var logoUri: Uri? = null
    private var lastStampedUri: Uri? = null
    private lateinit var cameraExecutor: ExecutorService

    private val pickLogo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            logoUri = uri
            statusText.text = "Đã chọn logo công ty."
        }
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else statusText.text = "Cần cấp quyền camera để chụp ảnh."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        buildUi()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(245, 247, 250))
        }
        val title = TextView(this).apply {
            text = "SITE PHOTO PRO"
            textSize = 22f
            setTextColor(Color.rgb(20, 53, 90))
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        root.addView(previewView, LinearLayout.LayoutParams(-1, 0, 1f))

        deviceInput = EditText(this).apply {
            hint = "Tên thiết bị (VD: PUMP-01, MCC-01)"
            singleLine = true
            textSize = 15f
            setText("Nhập tên thiết bị khi chụp")
        }
        root.addView(deviceInput, LinearLayout.LayoutParams(-1, -2))

        projectInput = EditText(this).apply {
            hint = "Tên dự án (không bắt buộc)"
            singleLine = true
            textSize = 15f
        }
        root.addView(projectInput, LinearLayout.LayoutParams(-1, -2))

        val logoButton = Button(this).apply {
            text = "Chọn logo công ty"
            setOnClickListener { pickLogo.launch(arrayOf("image/*")) }
        }
        root.addView(logoButton, LinearLayout.LayoutParams(-1, -2))

        val captureButton = Button(this).apply {
            text = "CHỤP ẢNH & ĐÓNG DẤU"
            textSize = 16f
            setOnClickListener { capturePhoto() }
        }
        root.addView(captureButton, LinearLayout.LayoutParams(-1, -2))

        val shareButton = Button(this).apply {
            text = "Chia sẻ ảnh đã đóng dấu"
            setOnClickListener { shareLastPhoto() }
        }
        root.addView(shareButton, LinearLayout.LayoutParams(-1, -2))

        statusText = TextView(this).apply {
            text = "Ảnh gốc và ảnh đóng dấu sẽ được lưu riêng trong thư viện."
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(dp(2), dp(6), dp(2), 0)
        }
        root.addView(statusText, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setJpegQuality(100)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                statusText.text = "Camera sẵn sàng. Có thể chụp ảnh hiện trường."
            } catch (e: Exception) {
                statusText.text = "Không mở được camera: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val capture = imageCapture
        if (capture == null) {
            statusText.text = "Camera chưa sẵn sàng."
            return
        }
        val device = deviceInput.text.toString().trim().ifBlank { "Chưa nhập tên thiết bị" }
        val project = projectInput.text.toString().trim()
        val temp = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        statusText.text = "Đang chụp ảnh..."
        val options = ImageCapture.OutputFileOptions.Builder(temp).build()
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            val bitmap = decodeCorrectOrientation(temp)
                            val originalUri = saveBitmapOrBytes(temp, null, "Original")
                            val stamped = addWatermark(bitmap, device, project)
                            val stampedUri = saveBitmapOrBytes(null, stamped, "Stamped")
                            bitmap.recycle()
                            stamped.recycle()
                            temp.delete()
                            Pair(originalUri, stampedUri)
                        }
                        lastStampedUri = result.second
                        statusText.text = "Đã lưu ảnh gốc và ảnh đóng dấu riêng."
                        Toast.makeText(this@MainActivity, "Chụp ảnh thành công", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        statusText.text = "Lỗi xử lý ảnh: ${e.localizedMessage}"
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                runOnUiThread { statusText.text = "Chụp ảnh thất bại: ${exception.localizedMessage}" }
            }
        })
    }

    private fun decodeCorrectOrientation(file: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalStateException("Không đọc được ảnh.")
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun addWatermark(source: Bitmap, device: String, project: String): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val scale = result.width / 1080f
        val pad = (24 * scale).coerceAtLeast(12f)
        val lineHeight = (34 * scale).coerceAtLeast(22f)
        val titleSize = (27 * scale).coerceAtLeast(18f)
        val textSize = (22 * scale).coerceAtLeast(15f)
        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val lines = mutableListOf("SITE PHOTO PRO", "Ngày giờ: $time", "Thiết bị: $device")
        if (project.isNotBlank()) lines.add("Dự án: $project")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = textSize; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = titleSize; typeface = Typeface.create("sans-serif", Typeface.BOLD) }

        var logo: Bitmap? = null
        try {
            logoUri?.let { uri ->
                contentResolver.openInputStream(uri)?.use { logo = BitmapFactory.decodeStream(it) }
            }
        } catch (_: Exception) {}

        val logoSize = (72 * scale).coerceAtLeast(48f)
        val logoGap = if (logo != null) logoSize + pad else 0f
        val maxTextWidth = result.width - pad * 2 - logoGap
        val measured = lines.mapIndexed { i, line -> (if (i == 0) boldPaint else paint).measureText(line) }
        val textBlockWidth = (measured.maxOrNull() ?: 0f).coerceAtMost(maxTextWidth)
        val boxWidth = (textBlockWidth + logoGap + pad * 2).coerceAtMost(result.width.toFloat())
        val boxHeight = pad * 2 + titleSize + (lines.size - 1) * lineHeight
        val left = pad
        val top = result.height - boxHeight - pad
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 0, 0, 0) }
        canvas.drawRoundRect(left, top, left + boxWidth, top + boxHeight, 12f * scale, 12f * scale, bg)

        logo?.let {
            val dest = RectF(left + pad, top + pad, left + pad + logoSize, top + pad + logoSize)
            canvas.drawBitmap(it, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            it.recycle()
        }
        val textX = left + pad + logoGap
        var y = top + pad + titleSize
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, textX, y, if (index == 0) boldPaint else paint)
            y += lineHeight
        }
        return result
    }

    private fun saveBitmapOrBytes(sourceFile: File?, bitmap: Bitmap?, folder: String): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "${timestamp}_${folder.lowercase(Locale.US)}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SitePhotoPro/$folder")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Không tạo được file ảnh trong thư viện.")
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                if (sourceFile != null) FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                else bitmap!!.compress(Bitmap.CompressFormat.JPEG, 98, out)
            } ?: throw IllegalStateException("Không ghi được ảnh.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                contentResolver.update(uri, done, null, null)
            }
            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun shareLastPhoto() {
        val uri = lastStampedUri
        if (uri == null) {
            Toast.makeText(this, "Hãy chụp ít nhất một ảnh trước.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ ảnh hiện trường"))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
    }
}
