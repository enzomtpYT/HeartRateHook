package website.xihan.pbra.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import website.xihan.pbra.HookEntry.Companion.mDeviceContact
import website.xihan.pbra.utils.Settings.webhookUrl
import website.xihan.pbra.utils.Settings.did
import website.xihan.pbra.utils.Settings.enableNonSportReport
import kotlin.time.Duration.Companion.minutes

/**
 * @项目名 : HeartRateHook
 * @作者 : MissYang
 * @创建时间 : 2024/8/7 21:35
 * @介绍 :
 */

object Ktor : KoinComponent {
    private val httpClient: HttpClient by inject()
    var sportMode = false
    private var periodicSendingJob: Job? = null
    private val heartRateChannel = Channel<Int>(
        Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    // Track the last reported heart rate to avoid sending duplicates
    private var lastHeartRate: Int = -1

    @OptIn(FlowPreview::class)
    fun startHeartRateUpdates() = ioThread {
        heartRateChannel
            .consumeAsFlow()
            .debounce(250) // Debounce for 250ms of inactivity before processing the next value
            .catch {
                Log.e("Flow exception: ${it.message}")
            }
            .onEach(::updateHeartRate)
            .conflate()
            .collect()
    }

    fun updateHeartRate(heartRate: Int) = ioThread {
        // Skip if heart rate hasn't changed from last reported value
        if (heartRate == lastHeartRate) {
            Log.d("Heart rate unchanged ($heartRate), skipping report")
            return@ioThread
        }
        
        // Don't send if URL is blank
        if (webhookUrl.isBlank()) {
            Log.e("Heart rate not sent: webhookUrl is blank")
            ToastUtil.show("Failed to send heart rate: Webhook URL not configured")
            return@ioThread
        }
        
        // Ensure webhook URL starts with http/https
        val url = if (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
            "https://$webhookUrl"
        } else {
            webhookUrl
        }
        
        Log.d("Sending heart rate: $heartRate (changed from $lastHeartRate) to URL: $url")
        try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(HeartRateModel(HeartRateModel.DataModel(heartRate)))
            }
            Log.d("Server response: ${response.status}, ${response.bodyAsText()}")
            if (response.status.value !in 200..299) {
                ToastUtil.show("Server error: ${response.status} - Failed to send heart rate")
            } else {
                // Only update the last heart rate when successfully sent
                lastHeartRate = heartRate
            }
        } catch (e: Exception) {
            Log.e("Failed to send heart rate: ${e.message}")
            ToastUtil.show("Network error: Failed to send heart rate - ${e.message}")
        }
    }

    // Reset last heart rate when entering/exiting sport mode to ensure first reading is sent
    fun resetLastHeartRate() {
        lastHeartRate = -1
        Log.d("Last heart rate value reset")
    }

    fun sendHeartRate(heartRate: Int) = ioThread {
        Log.d("Sending heart rate: $heartRate")
        heartRateChannel.send(heartRate)
    }

    fun startPeriodicSending() {
        if (!enableNonSportReport) return
        
        resetLastHeartRate() // Reset when starting periodic mode
        
        periodicSendingJob?.cancel()
        periodicSendingJob = ioThread {
            try {
                Log.d("startPeriodicSending")
                while (isActive) {
                    if (sportMode) break
                    if (mDeviceContact?.get() != null) {
                        mDeviceContact?.get()?.callMethod("syncData", did, false)
                    } else {
                        ToastUtil.show("Failed to get device. Please go to the device page to verify connection and sync data once")
                    }
                    delay(1.minutes)
                }
            } catch (e: Exception) {
                Log.e("Periodic sending error: ${e.message}")
            }
        }
    }

    fun stopPeriodicSending() {
        periodicSendingJob?.cancel()
        periodicSendingJob = null
    }
}