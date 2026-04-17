package com.hunterxdk.stayconnected.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.hunterxdk.stayconnected.MainActivity
import com.hunterxdk.stayconnected.R
import com.hunterxdk.stayconnected.receiver.NotificationActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Contact call reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a rich reminder notification:
     *
     * Collapsed: circular photo | "Time to call [Name]" | body text
     *            [ Call Now (teal pill) ] [ Snooze (grey pill) ]
     *
     * Expanded:  circular photo | "Time to call [Name]" | body text
     *            [ 📞 Call Now ] [ ⏰ 1hr ] [ ⏰ 2hr ] [ ⏰ 4hr ] [ ✕ Dismiss ]
     */
    fun showReminderNotification(
        contactId: Long,
        contactName: String,
        contactPhone: String = "",
        daysSinceLastCall: Long? = null,
        photoUri: String? = null
    ) {
        val notifId = contactId.toInt()

        // ── Body copy ─────────────────────────────────────────────────────────
        val bodyText = if (daysSinceLastCall != null && daysSinceLastCall > 0) {
            "You haven't called them in $daysSinceLastCall day${if (daysSinceLastCall == 1L) "" else "s"}."
        } else {
            "You haven't called them yet."
        }

        // ── Tap notification body → open ContactDetail ────────────────────────
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("contactId", contactId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Action PendingIntents ─────────────────────────────────────────────
        val callPi    = makeBroadcast(NotificationActionReceiver.ACTION_CALL_NOW,  contactId, notifId, contactPhone, RC_CALL)
        val snooze1Pi = makeBroadcast(NotificationActionReceiver.ACTION_SNOOZE_1H, contactId, notifId, null,         RC_SNOOZE_1)
        val snooze2Pi = makeBroadcast(NotificationActionReceiver.ACTION_SNOOZE_2H, contactId, notifId, null,         RC_SNOOZE_2)
        val snooze4Pi = makeBroadcast(NotificationActionReceiver.ACTION_SNOOZE_4H, contactId, notifId, null,         RC_SNOOZE_4)
        val dismissPi = makeBroadcast(NotificationActionReceiver.ACTION_DISMISS,   contactId, notifId, null,         RC_DISMISS)

        // ── Contact avatar (circular bitmap) ─────────────────────────────────
        val avatarBitmap = loadCircularBitmap(photoUri) ?: makeInitialsBitmap(contactName)

        // ── Collapsed RemoteViews (photo + text + Call Now + Snooze) ──────────
        val collapsedView = RemoteViews(context.packageName, R.layout.notification_collapsed).apply {
            setImageViewBitmap(R.id.notif_photo, avatarBitmap)
            setTextViewText(R.id.notif_title, "Time to call $contactName")
            setTextViewText(R.id.notif_body, bodyText)
            setOnClickPendingIntent(R.id.notif_btn_call, callPi)
            setOnClickPendingIntent(R.id.notif_btn_snooze, snooze2Pi) // default snooze = 2 hr
        }

        // ── Expanded RemoteViews (larger photo + text + 5 icon buttons) ───────
        val expandedView = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setImageViewBitmap(R.id.notif_exp_photo, avatarBitmap)
            setTextViewText(R.id.notif_exp_title, "Time to call $contactName")
            setTextViewText(R.id.notif_exp_body, bodyText)
            setOnClickPendingIntent(R.id.notif_exp_btn_call,    callPi)
            setOnClickPendingIntent(R.id.notif_exp_btn_1hr,     snooze1Pi)
            setOnClickPendingIntent(R.id.notif_exp_btn_2hr,     snooze2Pi)
            setOnClickPendingIntent(R.id.notif_exp_btn_4hr,     snooze4Pi)
            setOnClickPendingIntent(R.id.notif_exp_btn_dismiss, dismissPi)
        }

        // ── Build notification ────────────────────────────────────────────────
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(0xFF1D9E75.toInt())
            .setContentTitle("Time to call $contactName")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(false)

        notificationManager.notify(notifId, builder.build())
        Log.d(TAG, "Notification posted for $contactName (id=$notifId)")

        // Re-notify via WorkManager in 2 hours if user ignores
        ReminderScheduler.scheduleRenotify(context, contactId, delayMinutes = 120L)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeBroadcast(
        action: String,
        contactId: Long,
        notifId: Int,
        phone: String?,
        requestCodeOffset: Int
    ): PendingIntent {
        val intent = Intent(action, null, context, NotificationActionReceiver::class.java).apply {
            putExtra(NotificationActionReceiver.EXTRA_CONTACT_ID, contactId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            phone?.let { putExtra(NotificationActionReceiver.EXTRA_CONTACT_PHONE, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            notifId + requestCodeOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Loads a contact photo from a content URI and clips it to a circle.
     * Scales the source down to [MAX_AVATAR_PX] before clipping to avoid OOMs.
     */
    private fun loadCircularBitmap(photoUri: String?): Bitmap? {
        if (photoUri == null) return null
        return try {
            val raw = context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                val opts = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, opts)
                opts.inSampleSize = calculateInSampleSize(opts, MAX_AVATAR_PX, MAX_AVATAR_PX)
                opts.inJustDecodeBounds = false
                context.contentResolver.openInputStream(Uri.parse(photoUri))
                    ?.use { s2 -> BitmapFactory.decodeStream(s2, null, opts) }
            } ?: return null
            makeCircular(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load contact photo: $photoUri", e)
            null
        }
    }

    /** Clips a rectangular bitmap to a square circle. */
    private fun makeCircular(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return output
    }

    /**
     * Creates a circular initials avatar (teal background, white 2-letter initials).
     * Used as fallback when no contact photo is available.
     */
    private fun makeInitialsBitmap(name: String): Bitmap {
        val size = 128
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1D9E75.toInt() }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = size * 0.35f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, size / 2f, size / 2f - yOffset, textPaint)
        return output
    }

    private fun calculateInSampleSize(
        opts: android.graphics.BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = opts.outHeight to opts.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID = "reminders_channel"
        private const val MAX_AVATAR_PX = 256

        private const val RC_CALL     = 10_000
        private const val RC_SNOOZE_1 = 20_000
        private const val RC_SNOOZE_2 = 30_000
        private const val RC_SNOOZE_4 = 40_000
        private const val RC_DISMISS  = 50_000
    }
}
