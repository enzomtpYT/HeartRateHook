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

    var isLogin by serial(false)
    var userName by serial("")
    var userPass by serial("")

    var baseUrl by serial("")

    // Get status - returns true when configuration is valid, false when invalid
    fun getStatus() = isLogin && baseUrl.isNotBlank()

    // Report index 0:DirectLink 1:Cookie
    var reportIndex by serial(0)

    // Get report index text
    fun getReportIndexText() = if (reportIndex == 0) {
        "Direct Link"
    } else {
        "Cookie"
    }

    // Selected base URL 0:Hong Kong 1:Cloudflare
    var baseUrlIndex by serial(0)
    const val HK_BASE_URL = "https://public-heart-rate-api.xihan.website"
    const val CLOUD_FLARE_BASE_URL = "https://public-heart-rate-api.xihan.lat"

    fun getSelectedBaseUrl() = if (baseUrlIndex == 0) {
        HK_BASE_URL
    } else {
        CLOUD_FLARE_BASE_URL
    }

    fun getSelectedBaseUrlText() = if (baseUrlIndex == 0) {
        "Hong Kong"
    } else {
        "CloudFlare"
    }

    var enableNonSportReport by serial(true)
    var did by serial("")
}

