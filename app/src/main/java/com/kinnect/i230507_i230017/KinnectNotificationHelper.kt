package com.kinnect.i230507_i230017

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object KinnectNotificationHelper {

    private const val CHANNEL_MESSAGES = "messages"
    private const val CHANNEL_FRIENDS = "friends"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat message notifications"
            }

            val friendChannel = NotificationChannel(
                CHANNEL_FRIENDS,
                "Friend Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Friend request and confirmation notifications"
            }

            notificationManager.createNotificationChannel(msgChannel)
            notificationManager.createNotificationChannel(friendChannel)
        }
    }

    fun showMessageNotification(
        context: Context,
        senderUid: String,
        senderName: String,
        text: String,
        hasImage: Boolean
    ) {
        if ((senderUid.isNotEmpty()) && (senderUid == ChatActivity.activeChatUid)) return

        createNotificationChannels(context)

        val body = if (hasImage) "Sent a photo" else text.ifEmpty { "Sent a message" }
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("uid", senderUid)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderUid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(senderName.ifEmpty { "Kinnect Message" })
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(senderUid.hashCode(), notification)
    }

    fun showFriendRequestNotification(context: Context, senderUid: String, senderName: String) {
        createNotificationChannels(context)

        val intent = Intent(context, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("req_$senderUid").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FRIENDS)
            .setSmallIcon(R.drawable.ic_add_person)
            .setContentTitle("Friend Request")
            .setContentText("$senderName sent you a friend request.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(("req_$senderUid").hashCode(), notification)
    }

    fun showFriendAcceptedNotification(context: Context, senderUid: String, senderName: String) {
        createNotificationChannels(context)

        val intent = Intent(context, OtherProfileActivity::class.java).apply {
            putExtra("uid", senderUid)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("acc_$senderUid").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FRIENDS)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Friend Request Accepted")
            .setContentText("$senderName accepted your friend request.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(("acc_$senderUid").hashCode(), notification)
    }

    fun startObserving(context: Context, uid: String): () -> Unit {
        return {}
    }
}
