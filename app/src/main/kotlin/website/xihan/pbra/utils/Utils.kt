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
import website.xihan.pbra.utils.Settings.baseUrl
import website.xihan.pbra.utils.Settings.enableNonSportReport
import website.xihan.pbra.utils.Settings.getReportIndexText
import website.xihan.pbra.utils.Settings.getSelectedBaseUrlText
import website.xihan.pbra.utils.Settings.reportIndex
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

val kJson = Json {
    isLenient = true
//    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

class Weak<T>(val initializer: () -> T?) {
    private var weakReference: WeakReference<T?>? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = weakReference?.get() ?: let {
        weakReference = WeakReference(initializer())
        weakReference
    }?.get()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        weakReference = WeakReference(value)
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
        activity.showBaseUrlDialog()
    }
}

fun Activity.showBaseUrlDialog() {
    var innerBaseUrl = baseUrl
    var innerNonSportReport = enableNonSportReport
    val switch = CustomSwitchView(
        context = this,
        isChecked = innerNonSportReport,
        text = "Non-sport mode reporting",
        onCheckedChangeListener = { isChecked, _ ->
            innerNonSportReport = isChecked
            enableNonSportReport = isChecked
        })

    var index = reportIndex
    val postSwitch = CustomSwitchView(
        context = this,
        isChecked = index == 0,
        text = getReportIndexText(),
        onCheckedChangeListener = { isChecked, view ->
            index = if (isChecked) 0 else 1
            reportIndex = index
            view.updateText(getReportIndexText())
            ToastUtil.show("Switch successful")
        })
    val editText = CustomEditText(
        context = this, value = innerBaseUrl, hint = "Enter the complete address"
    ) {
        innerBaseUrl = it
    }

    val linearLayout = CustomLinearLayout(
        context = this, isAutoWidth = false, isAutoHeight = true
    ).apply {
        addView(switch)
        addView(postSwitch)
        addView(editText)
    }

    alertDialog {
        title = "Set Server Address"
        message = buildString {
            appendLine("Please enter the complete data reporting interface address")
            appendLine("There are two reporting methods, method 1 is used first")
            appendLine("1. Direct link mode: directly enter the complete address")
            appendLine("2. Cookie mode: login required")
            appendLine("Non-sport mode reporting works by reflectively calling the data synchronization method. Initial setup requires accessing the device page to get the reflection class. Reports are sent once per minute. For heart rate monitoring, the recommended frequency is once per minute.")
        }

        customView = linearLayout
        okButton {
            if (innerBaseUrl.isBlank()) {
                ToastUtil.show("Server address cannot be empty")
            } else {
                baseUrl = innerBaseUrl
                ToastUtil.show("Setup successful")
            }
        }
        neutralPressed("Login/Register") {
            showLoginOrRegisterDialog()
        }
        build()
        show()
    }
}

fun Activity.showLoginOrRegisterDialog() {
    var innerUserName = Settings.userName
    var innerUserPass = Settings.userPass
    var baseUrlIndex = Settings.baseUrlIndex
    val baseUrlSwitch = CustomSwitchView(
        context = this,
        isChecked = baseUrlIndex == 1,
        text = getSelectedBaseUrlText(),
        onCheckedChangeListener = { isChecked, view ->
            baseUrlIndex = if (isChecked) 1 else 0
            Settings.baseUrlIndex = baseUrlIndex
            view.updateText(getSelectedBaseUrlText())
            ToastUtil.show("Switch successful")
        })
    val editText = CustomEditText(
        context = this, value = innerUserName, hint = "Please enter account (3-50 characters)"
    ) {
        innerUserName = it
    }
    val editText2 = CustomEditText(
        context = this, value = innerUserPass, hint = "Please enter password (8-72 characters)"
    ) {
        innerUserPass = it
    }
    val loginText = "Login"
    val registerText = "Register"

    val linearLayout = CustomLinearLayout(
        context = this, isAutoWidth = false, isAutoHeight = true
    ).apply {
        addView(baseUrlSwitch)
        addView(editText)
        addView(editText2)
    }

    alertDialog {
        title = "Login or Register"
        customView = linearLayout

        positiveButton(loginText) {
            if (innerUserName.isBlank() || innerUserPass.isBlank()) {
                ToastUtil.show("Account and password cannot be empty")
            } else {
                Ktor.login(userName = innerUserName, userPass = innerUserPass, type = loginText)
            }
        }

        negativeButton("Logout") {
            Settings.userName = ""
            Settings.userPass = ""
            Settings.isLogin = false
            context.getSharedPreferences("heart_rate_cookie_prefs", Context.MODE_PRIVATE)
                .edit { clear() }
            ToastUtil.show("Logout successful")
        }
        neutralPressed(registerText) {
            if (innerUserName.isBlank() || innerUserPass.isBlank()) {
                ToastUtil.show("Account and password cannot be empty")
            } else {
                Ktor.login(userName = innerUserName, userPass = innerUserPass, type = registerText)
            }
        }
        build()
        show()
    }

}