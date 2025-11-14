package com.for2.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import okhttp3.*
import okio.ByteString
import kotlinx.coroutines.*

class LocationForegroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
        const val CHANNEL_ID = "for2_location"
        const val NOTIF_ID = 0xF02
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locReq: LocationRequest
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    private var ws: WebSocket? = null
    private val wsUrl = "ws://10.0.2.2:8080" // emulator -> host; change to your server
    private val client = OkHttpClient()
    private val wsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locReq = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) startForegroundServiceTasks()
        if (intent?.action == ACTION_STOP_ALARM) stopAlarm()
        return START_STICKY
    }

    private fun startForegroundServiceTasks() {
        createChannel()
        val notif = buildNotification("Tracking active")
        startForeground(NOTIF_ID, notif)

        // request location updates
        fusedClient.requestLocationUpdates(locReq, locCallback, Looper.getMainLooper())

        // open websocket
        openWebSocket()
    }

    private val locCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) {
                handleLocation(loc)
            }
        }
    }

    private fun handleLocation(loc: Location) {
        val payload = JSONObject()
        payload.put("vehicle_id", "ANDROID_BUS_001")
        payload.put("lat", loc.latitude)
        payload.put("lon", loc.longitude)
        payload.put("speed_kmh", loc.speed * 3.6)
        payload.put("bearing", loc.bearing)
        payload.put("timestamp", dateFmt.format(Date()))

        // Send over WebSocket
        wsScope.launch {
            ws?.send(payload.toString())
        }

        // Update notification or UI via broadcast (not implemented)
    }

    private fun openWebSocket() {
        try {
            val req = Request.Builder().url(wsUrl).build()
            client.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    ws = webSocket
                    println("[LocationService] ws open")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    println("[LocationService] ws msg: $text")
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    println("[LocationService] ws bytes")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    println("[LocationService] ws closing: $code / $reason")
                    webSocket.close(1000, null)
                    ws = null
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    println("[LocationService] ws failure: ${'$'}{t.message}")
                    ws = null
                    // schedule reconnect
                    wsScope.launch {
                        delay(3000)
                        openWebSocket()
                    }
                }
            })
        } catch (e: Exception) {
            println("[LocationService] ws open exception: ${'$'}{e.message}")
        }
    }

    private fun stopAlarm() {
        // stop sounds/alerts (stub)
        println("[LocationService] stop alarm requested")
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "For2 tracking", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val i = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("For2")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
