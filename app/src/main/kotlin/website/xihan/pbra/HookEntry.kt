package website.xihan.pbra

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import com.alibaba.fastjson2.toJSONString
import com.tencent.mmkv.MMKV
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.lazyModules
import website.xihan.pbra.utils.Ktor
import website.xihan.pbra.utils.Log
import website.xihan.pbra.utils.MiHealthPackage
import website.xihan.pbra.utils.MiHealthPackage.Companion.instance
import website.xihan.pbra.utils.Models
import website.xihan.pbra.utils.Settings.did
import website.xihan.pbra.utils.Settings.enableNonSportReport
import website.xihan.pbra.utils.Settings.disableToastNotifications
import website.xihan.pbra.utils.ToastUtil
import website.xihan.pbra.utils.appModule
import website.xihan.pbra.utils.callMethodOrNullAs
import website.xihan.pbra.utils.from
import website.xihan.pbra.utils.getIntFieldOrNull
import website.xihan.pbra.utils.getLongFieldOrNull
import website.xihan.pbra.utils.getObjectFieldOrNull
import website.xihan.pbra.utils.getObjectFieldOrNullAs
import website.xihan.pbra.utils.getViews
import website.xihan.pbra.utils.hookAfterMethod
import website.xihan.pbra.utils.hookAfterMethodByParameterTypes
import website.xihan.pbra.utils.hookBeforeMethod
import website.xihan.pbra.utils.kJson
import website.xihan.pbra.utils.safeCast
import website.xihan.pbra.utils.setOnClickListener
import java.lang.ref.WeakReference


/**
 * @Project : QDReaderHook
 * @Author : MissYang
 * @Created : 2025/1/4 15:42
 * @Description :
 */
class HookEntry : IXposedHookLoadPackage {


    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.mi.health") return

        Instrumentation::class.java.hookBeforeMethod(
            "callApplicationOnCreate", Application::class.java
        ) { param ->
            val application = param.args[0].safeCast<Application>() ?: return@hookBeforeMethod
            MMKV.initialize(application)
            startKoin {
                androidLogger()
                androidContext(application)
                lazyModules(appModule)
            }
            Ktor.startHeartRateUpdates()

            Log.d("Mi Health process launched ...")
            Log.d("SDK: ${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT}); Phone: ${Build.BRAND} ${Build.MODEL}")

            MiHealthPackage(lpparam.classLoader)

            when {
                !lpparam.processName.contains(":") -> {
                    if (enableNonSportReport) {
                        Ktor.startPeriodicSending()
                        instance.apply {
                            deviceTabV4ViewModelClass?.hookAfterMethodByParameterTypes(
                                "syncDevice", 2
                            ) { param ->
                                param.args.first().callMethodOrNullAs<String>("getDid")?.let {
                                    if (did != it) {
                                        did = it
                                    }
                                }
                                val rawContact =
                                    param.thisObject.getObjectFieldOrNull("mDeviceContact")
                                if (mDeviceContact == null || mDeviceContact?.get() == null) {
                                    mDeviceContact = WeakReference(rawContact)
                                    Log.d("mDeviceContact: $mDeviceContact")
                                }
                            }

                            "com.xiaomi.fit.fitness.export.data.aggregation.DailyHrReport".from(
                                lpparam.classLoader
                            )?.hookAfterMethodByParameterTypes("setLatestHrRecord", 1) { param ->
                                param.args[0]?.let {
                                    val time = it.getLongFieldOrNull("time")
                                    if (time != null && time > System.currentTimeMillis() / 1000 - 60) {
                                        Ktor.apply {
                                            if (sportMode) {
                                                sportMode = false
                                                startPeriodicSending()
                                                resetLastHeartRate() // Reset lastHeartRate when exiting sport mode
                                            }
                                        }
                                        val heartRate = it.getIntFieldOrNull("hr")
                                        if (heartRate != null && heartRate > 0) {
                                            Log.d("Detected heart rate outside sport: $heartRate")
                                            Ktor.sendHeartRate(heartRate)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    instance.apply {
                        baseSportVM?.hookAfterMethodByParameterTypes("onSuccess", 1) { param ->
                            Ktor.apply {
                                if (!sportMode) {
                                    sportMode = true
                                    stopPeriodicSending()
                                    resetLastHeartRate() // Reset lastHeartRate when entering sport mode
                                }
                            }
                            val sportingData =
                                param.args.firstOrNull() ?: return@hookAfterMethodByParameterTypes
                            
                            // Log the entire sportingData for debugging
                            Log.d("Sport data received: ${sportingData}")
                            
                            val list = sportingData.getObjectFieldOrNullAs<List<*>>("list")
                            if (list == null) {
                                Log.e("Failed to get list from sportingData")
                                return@hookAfterMethodByParameterTypes
                            }
                            
                            // Log the size of the list
                            Log.d("Sport data list size: ${list.size}")
                            
                            try {
                                val heartRateData = list.toJSONString()
                                Log.d("Sport data JSON: ${heartRateData}")
                                
                                // Try various ways to find heart rate data
                                val heartRateModel = kJson.decodeFromString<List<Models>>(heartRateData)
                                
                                // Option 1: Look for exact match "Heart Rate"
                                var heartRate = heartRateModel.firstOrNull { it.dataDes == "Heart Rate" }?.data?.toIntOrNull()
                                
                                // Option 2: Look for case insensitive match if Option 1 fails
                                if (heartRate == null) {
                                    heartRate = heartRateModel.firstOrNull { 
                                        it.dataDes.equals("Heart Rate", ignoreCase = true) || 
                                        it.dataDes.contains("heart", ignoreCase = true) 
                                    }?.data?.toIntOrNull()
                                }
                                
                                // Option 3: Try to find by looking at all data items
                                if (heartRate == null) {
                                    Log.d("Couldn't find heart rate by name, showing all items:")
                                    heartRateModel.forEach { model ->
                                        Log.d("DataDes: '${model.dataDes}', data: '${model.data}', other: '${model.other}'")
                                    }
                                    
                                    // Assuming the heart rate might be in another field, try to find a numeric value that looks like heart rate
                                    heartRate = heartRateModel.mapNotNull { 
                                        it.data.toIntOrNull() 
                                    }.firstOrNull { it in 30..220 } // Normal heart rate range
                                }
                                
                                if (heartRate != null && heartRate > 0) {
                                    Log.d("Found heart rate in sport mode: $heartRate")
                                    Ktor.sendHeartRate(heartRate)
                                    if (!disableToastNotifications) {
                                        ToastUtil.show("Heart rate detected: $heartRate")
                                    }
                                } else {
                                    Log.e("No valid heart rate found in sport data")
                                    if (!disableToastNotifications) {
                                        ToastUtil.show("No heart rate detected in sport data")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("Error processing sport heart rate: ${e.message}")
                                if (!disableToastNotifications) {
                                    ToastUtil.show("Error processing sport heart rate: ${e.message}")
                                }
                            }
                        }

                        aboutActivity?.hookAfterMethod("onCreate", Bundle::class.java) { param ->
                            val viewBinding = param.thisObject.getObjectFieldOrNull("mBinding")
                                ?: return@hookAfterMethod
                            val imageViews = viewBinding.getViews<ImageView>(isSuperClass = true)
                            if (imageViews.isEmpty()) {
                                Log.e("imageViews is empty")
                                return@hookAfterMethod
                            }
                            val activity =
                                param.thisObject.safeCast<Activity>() ?: return@hookAfterMethod
                            imageViews.forEach { imageView ->
                                imageView.setOnClickListener(activity)
                            }
                        }
                    }

                }

            }


        }
    }


    companion object {
        var mDeviceContact: WeakReference<Any>? = null
    }

}