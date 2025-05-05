package website.xihan.pbra.utils

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @Project : QDReaderHook
 * @Author : MissYang
 * @Created : 2025/2/25 16:58
 * @Description :
 */
object ToastUtil : KoinComponent {

    private val context by inject<Context>()
    private var toast: Toast? = null

    /** Cancel toast display */
    @JvmStatic
    fun cancel() {
        toast?.cancel()
    }

    /**
     * Show toast
     * @param msg Toast content
     * @param duration Toast display duration 0 short display time 1 long display time
     */
    @SuppressLint("ShowToast")
    private fun showToast(msg: CharSequence?, duration: Int) {
        msg ?: return
        toast?.cancel()
        mainThreadImmediate {
            toast = Toast.makeText(context.applicationContext, msg, duration)
            toast?.show()
        }
    }

    /**
     * Short display toast
     * @param msg Toast content
     */
    fun show(@StringRes msg: Int) {
        showToast(context.getString(msg), 0)
    }

    /**
     * Short display toast
     * @param msg Toast content
     */
    fun show(msg: CharSequence?) {
        showToast(msg, 0)
    }

    /**
     * Long display toast
     * @param msg Toast content
     */
    fun longShow(@StringRes msg: Int) {
        longShow(context.getString(msg))
    }

    /**
     * Long display toast
     * @param msg Toast content
     */
    private fun longShow(msg: CharSequence?) {
        showToast(msg, 1)
    }


}

