package com.redbox.iot.app.system_brightness

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.lang.reflect.Field
import kotlin.properties.Delegates


/** SystemBrightnessPlugin */
class SystemBrightnessPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var methodChannel: MethodChannel

  private lateinit var currentBrightnessChangeEventChannel: EventChannel
  private var brightnessChangeStreamHandler: BrightnessChangeStreamHandler? = null

  private lateinit var context: Context

  private var systemBrightness by Delegates.notNull<Float>()
  private var maximumBrightness by Delegates.notNull<Float>()
  private var changedBrightness: Float? = null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "redbox.iot.app/system_brightness")
    methodChannel.setMethodCallHandler(this)

    currentBrightnessChangeEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "redbox.iot.app/system_brightness/change")

    context = flutterPluginBinding.applicationContext

    try {
      maximumBrightness = getMaxBrightness()
      systemBrightness = getBrightness()
    } catch (e: Settings.SettingNotFoundException) {
      e.printStackTrace()
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "getBrightness" -> {
        result.success(getBrightness())
      }

      "setBrightness" -> {
        val brightness = call.argument<Double>("brightness") ?: 0.0
        setBrightness(brightness.toInt(), result)
      }

      "getScreenTimeout" -> {
        result.success(getScreenTimeout())
      }

      "setScreenTimeout" -> {
        val timeout = call.argument<Int>("timeout") ?: 0
        setScreenTimeout(timeout, result)
      }

      else -> {
        result.notImplemented()
      }
    }
  }

  private fun getBrightness(): Float {
    return Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / maximumBrightness
  }

  private fun getMaxBrightness(): Float {
    try {
      val powerManager: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager? ?: throw ClassNotFoundException()
      val fields: Array<Field> = powerManager.javaClass.declaredFields
      for (field in fields) {
        if (field.name.equals("BRIGHTNESS_ON")) {
          field.isAccessible = true
          return (field[powerManager] as Int).toFloat()
        }
      }
      return 255.0f
    } catch (e: Exception) {
      return 255.0f
    }
  }

  private fun setBrightness(brightness: Int, result: Result) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!Settings.System.canWrite(context)) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setData(Uri.parse("package:" + context.packageName))
        context.startActivity(intent)
      } else {
        setBrightness2(brightness, result)
      }
    } else {
      setBrightness2(brightness, result)
    }
  }

  private fun setBrightness2(brightness: Int, result: Result) {
    try {
      val cr = context.contentResolver
      Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, brightness)
      result.success(null)
    } catch (e: SecurityException) {
      result.error("SecurityException", e.message, null)
    }
  }

  private fun getScreenTimeout(): Int {
    return Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
  }

  private fun setScreenTimeout(timeout: Int, result: Result) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!Settings.System.canWrite(context)) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setData(Uri.parse("package:" + context.packageName))
        context.startActivity(intent)
      } else {
        setScreenTimeout2(timeout, result)
      }
    } else {
      setScreenTimeout2(timeout, result)
    }
  }

  private fun setScreenTimeout2(timeout: Int, result: Result) {
    try {
      Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, timeout)
      result.success(null)
    } catch (e: SecurityException) {
      result.error("SecurityException", e.message, null)
    }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    brightnessChangeStreamHandler = BrightnessChangeStreamHandler(
      binding.activity,
      onListenStart = null,
      onChange = { eventSink ->
        systemBrightness = getBrightness()
        if (changedBrightness == null) {
          eventSink.success(systemBrightness)
        }
      },
    )
    currentBrightnessChangeEventChannel.setStreamHandler(brightnessChangeStreamHandler)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    //
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    //
  }

  override fun onDetachedFromActivity() {
    currentBrightnessChangeEventChannel.setStreamHandler(null)
    brightnessChangeStreamHandler = null
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel.setMethodCallHandler(null)
    currentBrightnessChangeEventChannel.setStreamHandler(null)
    brightnessChangeStreamHandler = null
  }
}
