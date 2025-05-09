package website.xihan.pbra.utils


import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.edit
import kotlinx.serialization.json.Json
import website.xihan.pbra.utils.Settings.webhookUrl
import website.xihan.pbra.utils.Settings.enableNonSportReport
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

val kJson = Json {
    isLenient = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

class Weak<T>(val initializer: () -> T?) {
    private var weakReference: WeakReference<T?>? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val value = weakReference?.get()
        if (value == null) {
            val newValue = initializer()
            weakReference = WeakReference(newValue)
            return newValue
        }
        return value
    }
}

val systemContext: Context
    get() {
        val activityThread = "android.app.ActivityThread".findClassOrNull(null)
            ?.callStaticMethod("currentActivityThread")!!
        return activityThread.callMethodAs("getSystemContext")
    }


fun getPackageVersion(packageName: String) = try {
    systemContext.packageManager.getPackageInfo(packageName, 0).run {
        String.format("${packageName}@%s(%s)", versionName, getVersionCode(packageName))
    }
} catch (e: Throwable) {
    Log.e(e)
    "(unknown)"
}


fun getVersionCode(packageName: String) = try {
    @Suppress("DEPRECATION") systemContext.packageManager.getPackageInfo(packageName, 0).versionCode
} catch (e: Throwable) {
    Log.e(e)
    null
} ?: 6080000


inline fun <reified T> Any?.safeCast(): T? = this as? T

fun Context.copyToClipboard(text: String) {
    // Get clipboard manager from system service
    val clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    // Create new clipboard data with the text and set it to the clipboard
    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text))
}

@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T : View> Any.getViews(isSuperClass: Boolean = false) =
    getParamList<T>(isSuperClass)

@Throws(NoSuchFieldException::class, IllegalAccessException::class)
inline fun <reified T> Any.getParamList(isSuperClass: Boolean = false): ArrayList<T> {
    val results = ArrayList<T>()
    val classes =
        if (isSuperClass) generateSequence(javaClass) { it.superclass }.toList() else listOf(
            javaClass
        )
    val type = T::class.java
    for (clazz in classes) {
        clazz.declaredFields.filter { type.isAssignableFrom(it.type) }.forEach { field ->
            field.isAccessible = true
            val value = field.get(this)
            if (type.isInstance(value)) {
                results += value as T
            }
        }
    }
    return results
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

infix fun Int.x(other: Int): ViewGroup.LayoutParams = ViewGroup.LayoutParams(this, other)

fun Activity.restartApplication() = packageManager.getLaunchIntentForPackage(packageName)?.let {
    finishAffinity()
    startActivity(intent)
    exitProcess(0)
}

fun ImageView.setOnClickListener(activity: Activity) {
    setOnClickListener {
        activity.showWebhookConfigDialog()
    }
}

fun Activity.showWebhookConfigDialog() {
    var innerWebhookUrl = webhookUrl
    var innerNonSportReport = enableNonSportReport
    
    val switch = CustomSwitchView(
        context = this,
        isChecked = innerNonSportReport,
        text = "Non-sport mode reporting",
        onCheckedChangeListener = { isChecked, _ ->
            innerNonSportReport = isChecked
            enableNonSportReport = isChecked
        })
    
    val editText = CustomEditText(
        context = this, 
        value = innerWebhookUrl, 
        hint = "Enter webhook URL (https://example.com/webhook)"
    ) {
        innerWebhookUrl = it
    }

    val linearLayout = CustomLinearLayout(
        context = this, isAutoWidth = false, isAutoHeight = true
    ).apply {
        addView(switch)
        addView(editText)
    }

    alertDialog {
        title = "Configure Webhook"
        message = buildString {
            appendLine("Please enter the complete webhook URL where heart rate data will be sent")
            appendLine("Format example: https://example.com/webhook")
            appendLine("Non-sport mode reporting works by reflectively calling the data synchronization method. Initial setup requires accessing the device page to get the reflection class. Reports are sent once per minute.")
        }

        customView = linearLayout
        okButton {
            if (innerWebhookUrl.isBlank()) {
                ToastUtil.show("Webhook URL cannot be empty")
            } else {
                webhookUrl = innerWebhookUrl
                ToastUtil.show("Configuration saved successfully")
            }
        }
        build()
        show()
    }
}