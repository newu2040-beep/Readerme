package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class PlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val bookTitle = intent?.getStringExtra("EXTRA_BOOK_TITLE") ?: "ReaderMe"
        val bookAuthor = intent?.getStringExtra("EXTRA_BOOK_AUTHOR") ?: "Background Playback"

        if (action != null) {
            when (action) {
                ACTION_PLAY -> {
                    TtsEngine.instance?.play()
                }
                ACTION_PAUSE -> {
                    TtsEngine.instance?.pause()
                }
                ACTION_STOP -> {
                    TtsEngine.instance?.stop()
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_NEXT -> {
                    TtsEngine.instance?.nextSentence()
                }
                ACTION_PREV -> {
                    TtsEngine.instance?.previousSentence()
                }
            }
        }

        val isActuallyPlaying = TtsEngine.instance?.isPlaying?.value ?: false
        showNotification(bookTitle, bookAuthor, isActuallyPlaying)

        return START_NOT_STICKY
    }

    private fun showNotification(title: String, author: String, isPlaying: Boolean) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flag)

        // Create PendingIntents for playback actions
        val playPauseIntent = Intent(this, PlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            putExtra("EXTRA_BOOK_TITLE", title)
            putExtra("EXTRA_BOOK_AUTHOR", author)
        }
        val playPausePendingIntent = PendingIntent.getService(this, 1, playPauseIntent, flag)

        val prevIntent = Intent(this, PlaybackService::class.java).apply {
            action = ACTION_PREV
            putExtra("EXTRA_BOOK_TITLE", title)
            putExtra("EXTRA_BOOK_AUTHOR", author)
        }
        val prevPendingIntent = PendingIntent.getService(this, 2, prevIntent, flag)

        val nextIntent = Intent(this, PlaybackService::class.java).apply {
            action = ACTION_NEXT
            putExtra("EXTRA_BOOK_TITLE", title)
            putExtra("EXTRA_BOOK_AUTHOR", author)
        }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, flag)

        val stopIntent = Intent(this, PlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, flag)

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseText = if (isPlaying) "Pause" else "Play"

        // Build elegant notification compatible with Android notifications panel
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(author)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevPendingIntent)
            .addAction(playPauseIcon, playPauseText, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE)
        }

        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ReaderMe Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for background text-to-speech audio reader"
                enableLights(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "readerme_playback_channel"
        const val NOTIFICATION_ID = 4591

        const val ACTION_PLAY = "com.example.player.PLAY"
        const val ACTION_PAUSE = "com.example.player.PAUSE"
        const val ACTION_STOP = "com.example.player.STOP"
        const val ACTION_NEXT = "com.example.player.NEXT"
        const val ACTION_PREV = "com.example.player.PREV"
    }
}
