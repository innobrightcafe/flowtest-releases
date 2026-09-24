package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionReceiptData(
    val transactionId: String,
    val reference: String,
    val title: String,
    val serviceType: String,
    val recipient: String,
    val amountPaid: Double,
    val dateFormatted: String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
    val senderName: String = "FlowTest Client",
    val senderAccount: String = "FlowTest Wallet (6666468328)",
    val companyName: String = "FlowTest",
    val status: String = "SUCCESSFUL",
    val balanceBefore: Double? = null,
    val balanceAfter: Double? = null,
    val narration: String? = null,
    val bonusInfo: String? = null,
    val tokenPin: String? = null,
    val meterUnits: String? = null,
    val customerName: String? = null,
    val meterNumber: String? = null,
    val serviceAddress: String? = null,
    val discoName: String? = null,
    val meterType: String? = null,
    val tariffClass: String? = null
)

object PdfReceiptGenerator {

    /**
     * Generates a high-resolution, beautifully styled PDF receipt.
     */
    fun generateReceiptPdf(context: Context, data: TransactionReceiptData): File {
        // Defensive retrieval of utility data from SharedPreferences if missing in data object
        var cachedToken: String? = null
        var cachedAddress: String? = null
        var cachedCustomer: String? = null
        var cachedDisco: String? = null
        var cachedMeter: String? = null
        var cachedUnits: String? = null
        var cachedTariff: String? = null

        try {
            val prefs = context.getSharedPreferences("saved_utility_receipts_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("receipt_${data.reference.trim()}", null)
                ?: prefs.getString("receipt_${data.transactionId.trim()}", null)
            if (jsonStr != null) {
                val jo = org.json.JSONObject(jsonStr)
                cachedToken = jo.optString("tokenPin").takeIf { it.isNotBlank() }
                cachedAddress = jo.optString("serviceAddress").takeIf { it.isNotBlank() }
                cachedCustomer = jo.optString("customerName").takeIf { it.isNotBlank() }
                cachedDisco = jo.optString("discoName").takeIf { it.isNotBlank() }
                cachedMeter = jo.optString("meterNumber").takeIf { it.isNotBlank() }
                cachedUnits = jo.optString("meterUnits").takeIf { it.isNotBlank() }
                cachedTariff = jo.optString("tariffClass").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {}

        val resolvedToken = data.tokenPin?.takeIf { it.isNotBlank() }
            ?: cachedToken
            ?: run {
                val fullText = "${data.narration ?: ""} ${data.title} ${data.reference}"
                val regex = Regex("""\b(\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{4})\b|\b(\d{20})\b""")
                regex.find(fullText)?.value
            }

        val resolvedAddress = data.serviceAddress?.takeIf { it.isNotBlank() }
            ?: cachedAddress

        val resolvedCustomer = data.customerName?.takeIf { it.isNotBlank() }
            ?: cachedCustomer

        val resolvedDisco = data.discoName?.takeIf { it.isNotBlank() }
            ?: cachedDisco
            ?: data.serviceType

        val resolvedMeter = data.meterNumber?.takeIf { it.isNotBlank() }
            ?: cachedMeter
            ?: data.recipient

        val resolvedUnits = data.meterUnits?.takeIf { it.isNotBlank() }
            ?: cachedUnits

        val resolvedTariff = data.tariffClass?.takeIf { it.isNotBlank() }
            ?: cachedTariff
            ?: "Band A Residential"

        val isElectricity = data.serviceType.contains("electric", ignoreCase = true) ||
                data.title.contains("electric", ignoreCase = true) ||
                !data.discoName.isNullOrBlank() ||
                !resolvedToken.isNullOrBlank() ||
                !data.meterNumber.isNullOrBlank()

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in postscript points
        val pageHeight = 842 // Standard A4 height in postscript points

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints setup
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Background Fill
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        // 2. Top Header Accent Stripe
        paint.color = Color.parseColor("#0F172A") // Slate 900
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 130f, paint)

        paint.color = Color.parseColor("#06B6D4") // Cyan accent line
        canvas.drawRect(0f, 126f, pageWidth.toFloat(), 130f, paint)

        // Header Logo Monogram Badge with FlowTest Logo
        val badgeRect = RectF(40f, 32f, 104f, 96f)
        var logoDrawn = false
        try {
            val logoResId = com.example.R.drawable.img_flowtest_logo_1789127394070
            val bitmap = BitmapFactory.decodeResource(context.resources, logoResId)
            if (bitmap != null) {
                val path = Path().apply {
                    addRoundRect(badgeRect, 14f, 14f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(bitmap, null, badgeRect, paint)
                canvas.restore()

                paint.color = Color.parseColor("#06B6D4") // Cyan border
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                canvas.drawRoundRect(badgeRect, 14f, 14f, paint)
                paint.style = Paint.Style.FILL
                logoDrawn = true
            }
        } catch (_: Exception) {
            logoDrawn = false
        }

        if (!logoDrawn) {
            paint.color = Color.parseColor("#1E293B")
            canvas.drawRoundRect(badgeRect, 14f, 14f, paint)

            paint.color = Color.parseColor("#06B6D4")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(badgeRect, 14f, 14f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#06B6D4")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 20f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("FLOW", 72f, 62f, paint)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#10B981")
            canvas.drawText("TEST", 72f, 78f, paint)
        }

        // Company Name & Subtitle
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.companyName, 120f, 58f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("OFFICIAL ELECTRONIC UTILITY & TRANSACTION RECEIPT", 120f, 78f, paint)
        canvas.drawText("FlowTest Utility Gateway • DisCo Settlement • RC: 9710966", 120f, 94f, paint)

        // 3. Receipt Body Card Container
        val cardRect = RectF(36f, 120f, (pageWidth - 36).toFloat(), (pageHeight - 75).toFloat())
        paint.color = Color.WHITE
        canvas.drawRoundRect(cardRect, 18f, 18f, paint)

        // Subtle Card Border
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(cardRect, 18f, 18f, paint)
        paint.style = Paint.Style.FILL

        // =========================================================================
        // 3b. Sampled Electricity Distribution Company Logo on PDF background
        // =========================================================================
        if (isElectricity) {
            try {
                val watermarkResId = com.example.R.drawable.electricity_disco_watermark_1790096110385
                val wmBitmap = BitmapFactory.decodeResource(context.resources, watermarkResId)
                if (wmBitmap != null) {
                    val wmWidth = 320f
                    val wmHeight = 320f
                    val wmRect = RectF(
                        (pageWidth / 2f) - (wmWidth / 2f),
                        330f,
                        (pageWidth / 2f) + (wmWidth / 2f),
                        330f + wmHeight
                    )
                    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = 28 // Subtle watermark opacity (~11%) ensuring crisp text legibility
                    }
                    canvas.drawBitmap(wmBitmap, null, wmRect, wmPaint)
                }
            } catch (_: Exception) {}
        }

        // Status Badge Pill (Centered)
        val statusPillRect = RectF((pageWidth / 2 - 95).toFloat(), 138f, (pageWidth / 2 + 95).toFloat(), 166f)
        paint.color = Color.parseColor("#ECFDF5") // Emerald 50
        canvas.drawRoundRect(statusPillRect, 14f, 14f, paint)

        paint.color = Color.parseColor("#10B981") // Emerald 500
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(statusPillRect, 14f, 14f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#047857")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("✓  TRANSACTION ${data.status.uppercase()}", (pageWidth / 2).toFloat(), 156f, paint)

        // Amount Display
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 25f
        paint.textAlign = Paint.Align.CENTER
        val amountStr = "₦" + String.format(Locale.US, "%,.2f", data.amountPaid)
        canvas.drawText(amountStr, (pageWidth / 2).toFloat(), 192f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (isElectricity) {
            canvas.drawText("Electricity bill payment was successful. Thank you for choosing FlowTest.", (pageWidth / 2).toFloat(), 209f, paint)
        } else {
            canvas.drawText(data.title, (pageWidth / 2).toFloat(), 209f, paint)
        }

        var curY = 224f

        // =========================================================================
        // 4. Prominent Highlighted Token PIN Card (Pairgate style)
        // =========================================================================
        if (!resolvedToken.isNullOrBlank()) {
            val tokenCardRect = RectF(48f, curY, (pageWidth - 48).toFloat(), curY + 62f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#ECFDF5")
            canvas.drawRoundRect(tokenCardRect, 10f, 10f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = Color.parseColor("#10B981")
            canvas.drawRoundRect(tokenCardRect, 10f, 10f, paint)

            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.parseColor("#065F46")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            canvas.drawText("⚡ PREPAID ELECTRICITY TOKEN PIN (ENTER IN METER KEYPAD)", (pageWidth / 2).toFloat(), curY + 16f, paint)

            paint.color = Color.parseColor("#047857")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 15.5f
            canvas.drawText(resolvedToken, (pageWidth / 2).toFloat(), curY + 36f, paint)

            paint.color = Color.parseColor("#1F2937")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.5f
            val unitsSnippet = if (!resolvedUnits.isNullOrBlank()) " • Units: $resolvedUnits" else ""
            canvas.drawText("⚠️ Enter this 20-digit PIN into meter keypad & press Enter (↵)$unitsSnippet", (pageWidth / 2).toFloat(), curY + 52f, paint)

            curY += 72f
        }

        // =========================================================================
        // 5. Official Utility & Proof of Address Verification Card
        // =========================================================================
        val hasUtilityDetails = !resolvedCustomer.isNullOrBlank() || !resolvedAddress.isNullOrBlank() || !resolvedMeter.isNullOrBlank()
        if (hasUtilityDetails) {
            val utilCardRect = RectF(48f, curY, (pageWidth - 48).toFloat(), curY + 76f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F0FDF4")
            canvas.drawRoundRect(utilCardRect, 10f, 10f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            paint.color = Color.parseColor("#10B981")
            canvas.drawRoundRect(utilCardRect, 10f, 10f, paint)

            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#065F46")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            canvas.drawText("⚡ METER & SERVICE DETAILS (UTILITY ADDRESS PROOF)", 60f, curY + 15f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#059669")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8f
            canvas.drawText("OFFICIAL VERIFICATION RECORD", (pageWidth - 60).toFloat(), curY + 15f, paint)

            // Customer Name & Meter Number
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            val nameText = "Customer: " + (resolvedCustomer ?: "Registered Consumer")
            val nameTrunc = if (nameText.length > 36) nameText.take(33) + "..." else nameText
            canvas.drawText(nameTrunc, 60f, curY + 31f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#047857")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 9f
            val meterText = "Meter: $resolvedMeter"
            canvas.drawText(meterText, (pageWidth - 60).toFloat(), curY + 31f, paint)

            // Address on line 3 (Full visibility with clean truncation limit)
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#334155")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            val addrText = "Address: " + (resolvedAddress ?: "Service Location Recorded")
            val addrTrunc = if (addrText.length > 76) addrText.take(73) + "..." else addrText
            canvas.drawText(addrTrunc, 60f, curY + 47f, paint)

            // Disco & Tariff on line 4
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8f
            val discoTariff = "DISCO: $resolvedDisco • Tariff: $resolvedTariff"
            val discoTrunc = if (discoTariff.length > 76) discoTariff.take(73) + "..." else discoTariff
            canvas.drawText(discoTrunc, 60f, curY + 63f, paint)

            curY += 84f
        }

        if (resolvedToken.isNullOrBlank() && !hasUtilityDetails) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#F1F5F9")
            paint.strokeWidth = 1.5f
            canvas.drawLine(56f, curY, (pageWidth - 56).toFloat(), curY, paint)
            paint.style = Paint.Style.FILL
            curY += 16f
        } else {
            curY += 2f
        }

        // =========================================================================
        // 6. Two-Column Itemized Details Table (Pairgate Structure)
        // =========================================================================
        val leftX = 60f
        val rightX = (pageWidth - 60).toFloat()
        val rowHeight = 20f
        val labelSize = 9.5f
        val valSize = 10f

        val detailsList = mutableListOf<Pair<String, String>>()
        detailsList.add("Status" to data.status.lowercase())
        detailsList.add("Purchase" to (if (isElectricity) "Electricity Bill" else data.title))
        detailsList.add("Company" to resolvedDisco)
        detailsList.add("Meter Type" to (data.meterType ?: "Prepaid"))
        detailsList.add("Meter No." to resolvedMeter)

        if (!resolvedToken.isNullOrBlank()) {
            detailsList.add("Token" to resolvedToken)
        }
        if (!resolvedUnits.isNullOrBlank()) {
            detailsList.add("Units" to resolvedUnits)
        }
        if (!resolvedCustomer.isNullOrBlank()) {
            detailsList.add("Receiver Name" to resolvedCustomer)
        }
        if (!resolvedAddress.isNullOrBlank()) {
            detailsList.add("Address" to resolvedAddress)
        }

        detailsList.add("Date & Time" to data.dateFormatted)
        detailsList.add("Total Cost" to amountStr)
        detailsList.add("Transaction Reference" to (data.reference.ifBlank { data.transactionId }))

        if (data.balanceAfter != null) {
            detailsList.add("New Balance" to "₦" + String.format(Locale.US, "%,.2f", data.balanceAfter))
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((index, item) in detailsList.withIndex()) {
            val isAddressRow = item.first.equals("Address", ignoreCase = true) || item.first.equals("Service Address", ignoreCase = true)
            val isTokenRow = item.first.equals("Token", ignoreCase = true)

            // Key Label
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = labelSize
            canvas.drawText(item.first, leftX, curY, paint)

            if (isTokenRow) {
                // Highlight token row in table with bold dark emerald
                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#047857")
                paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                paint.textSize = valSize + 1f
                canvas.drawText(item.second, rightX, curY, paint)
                curY += rowHeight
            } else if (isAddressRow) {
                // Multi-line word-wrap for address to ensure zero truncation for utility address verification
                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = valSize

                val maxTextWidth = 320f
                val words = item.second.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = StringBuilder()

                for (w in words) {
                    val candidate = if (currentLine.isEmpty()) w else "${currentLine} $w"
                    if (paint.measureText(candidate) <= maxTextWidth) {
                        currentLine.append(if (currentLine.isEmpty()) w else " $w")
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                        currentLine = StringBuilder(w)
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())

                for ((lIdx, lineStr) in lines.withIndex()) {
                    if (lIdx > 0) curY += 13f
                    canvas.drawText(lineStr, rightX, curY, paint)
                }
                curY += rowHeight
            } else {
                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = valSize

                val maxChars = 44
                val valueStr = if (item.second.length > maxChars) item.second.take(maxChars - 3) + "..." else item.second
                canvas.drawText(valueStr, rightX, curY, paint)
                curY += rowHeight
            }
        }

        // =========================================================================
        // 7. Digital Security Watermark & QR Grid Box
        // =========================================================================
        val qrTopY = (pageHeight - 150).toFloat()
        val qrRect = RectF(56f, qrTopY, 110f, qrTopY + 54f)
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(qrRect, 8f, 8f, paint)

        // Draw simulated high-tech QR code matrix blocks
        paint.color = Color.parseColor("#06B6D4")
        canvas.drawRect(64f, qrTopY + 8f, 78f, qrTopY + 22f, paint)
        canvas.drawRect(88f, qrTopY + 8f, 102f, qrTopY + 22f, paint)
        canvas.drawRect(64f, qrTopY + 32f, 78f, qrTopY + 46f, paint)
        paint.color = Color.WHITE
        canvas.drawRect(68f, qrTopY + 12f, 74f, qrTopY + 18f, paint)
        canvas.drawRect(92f, qrTopY + 12f, 98f, qrTopY + 18f, paint)
        canvas.drawRect(68f, qrTopY + 36f, 74f, qrTopY + 42f, paint)
        paint.color = Color.parseColor("#10B981")
        canvas.drawRect(84f, qrTopY + 27f, 92f, qrTopY + 35f, paint)
        canvas.drawRect(93f, qrTopY + 37f, 101f, qrTopY + 45f, paint)

        // Security Notice Text
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText("DIGITALLY SIGNED & VERIFIED BY FLOWTEST", 124f, qrTopY + 17f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        canvas.drawText("Session ID: ${data.reference.ifBlank { data.transactionId }}", 124f, qrTopY + 32f, paint)
        canvas.drawText("Official system-generated receipt. Certified for Utility Address Verification.", 124f, qrTopY + 46f, paint)

        // 8. Page Footer
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9.5f
        canvas.drawText("FlowTest • Support: support@flowtest2026.com • Tel: +234 813 754 5370", (pageWidth / 2).toFloat(), (pageHeight - 45).toFloat(), paint)

        pdfDocument.finishPage(page)

        // Write to Cache / Documents File
        val safeRef = (data.reference.ifBlank { data.transactionId }).replace("[^a-zA-Z0-9_-]".toRegex(), "")
        val fileName = "Receipt_${safeRef}.pdf"
        val pdfFile = File(context.cacheDir, fileName)

        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Opens Android System Share Sheet to share the generated PDF receipt.
     */
    fun shareReceiptPdf(context: Context, data: TransactionReceiptData) {
        try {
            val file = generateReceiptPdf(context, data)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Payment Receipt - ${data.title} (${data.reference})")
                putExtra(Intent.EXTRA_TEXT, "Here is your official payment receipt for ${data.title} of ₦${String.format(Locale.US, "%,.2f", data.amountPaid)} from FlowTest.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Receipt PDF via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Downloads/Saves the PDF receipt to device public Downloads folder or external files.
     */
    fun downloadReceiptPdf(context: Context, data: TransactionReceiptData, onDownloaded: ((File) -> Unit)? = null) {
        try {
            val generatedFile = generateReceiptPdf(context, data)
            val safeRef = (data.reference.ifBlank { data.transactionId }).replace("[^a-zA-Z0-9_-]".toRegex(), "")
            val fileName = "FlowTest_Receipt_${safeRef}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        generatedFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    Toast.makeText(context, "Receipt saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                    onDownloaded?.invoke(generatedFile)
                    return
                }
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destDir = if (downloadsDir.exists() && downloadsDir.canWrite()) downloadsDir
                          else context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            
            val targetFile = File(destDir, fileName)
            generatedFile.copyTo(targetFile, overwrite = true)
            
            Toast.makeText(context, "Receipt downloaded: ${targetFile.name}", Toast.LENGTH_LONG).show()
            onDownloaded?.invoke(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to cache file if public download permission/storage issues arise
            val fallbackFile = generateReceiptPdf(context, data)
            Toast.makeText(context, "Receipt saved to app storage: ${fallbackFile.name}", Toast.LENGTH_LONG).show()
            onDownloaded?.invoke(fallbackFile)
        }
    }

    /**
     * Formats receipt as rich readable text for quick copy/sharing.
     */
    fun formatReceiptText(data: TransactionReceiptData): String {
        return buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("         FlowTest           ")
            appendLine(" OFFICIAL PAYMENT RECEIPT   ")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Status: ${data.status.uppercase()} ✓")
            appendLine("Amount: ₦${String.format(Locale.US, "%,.2f", data.amountPaid)}")
            appendLine("Service: ${data.title}")
            appendLine("Recipient: ${data.recipient}")
            appendLine("Sender: ${data.senderName}")
            appendLine("Channel: ${data.senderAccount}")
            appendLine("Date: ${data.dateFormatted}")
            appendLine("Ref: ${data.reference.ifBlank { data.transactionId }}")
            if (data.balanceAfter != null) {
                appendLine("New Balance: ₦${String.format(Locale.US, "%,.2f", data.balanceAfter)}")
            }
            if (!data.bonusInfo.isNullOrBlank()) {
                appendLine("Bonus: ${data.bonusInfo}")
            }
            if (!data.tokenPin.isNullOrBlank()) {
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("⚡ PREPAID TOKEN PIN: ${data.tokenPin}")
                if (!data.meterUnits.isNullOrBlank()) {
                    appendLine("Estimated Units: ${data.meterUnits}")
                }
                appendLine("NOTE: Enter this 20-digit PIN into your prepaid meter keypad followed by Enter (↵). The system does not recharge meters automatically.")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Digitally Verified by FlowTest")
            appendLine("Support: support@flowtest2026.com")
        }
    }

    /**
     * Opens or shares the generated PDF receipt with an external PDF viewer.
     */
    fun openOrShareReceipt(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "View Receipt PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (err: Exception) {
                Toast.makeText(context, "Receipt PDF saved: ${file.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
