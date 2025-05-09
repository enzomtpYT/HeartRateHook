package website.xihan.pbra.utils

import com.drake.serialize.serialize.annotation.SerializeConfig
import com.drake.serialize.serialize.serial


/**
 * @Project : QDReaderHook
 * @Author : MissYang
 * @Created : 2025/1/10 16:56
 * @Description :
 */
@SerializeConfig(mmapID = "heart_rate_hook")
object Settings {

    // Webhook URL for heart rate data
    var webhookUrl by serial("")

    // Enable non-sport mode reporting
    var enableNonSportReport by serial(true)
    
    // Device ID
    var did by serial("")
}

