part of '../system_brightness.dart';

class SystemBrightness {
  SystemBrightness._();

  static final SystemBrightness _instance = SystemBrightness._();

  factory SystemBrightness() => instance;

  static SystemBrightness get instance => _instance;

  static const pluginMethodChannelName = 'redbox.iot.app/system_brightness';
  static const pluginMethodChannel = MethodChannel(pluginMethodChannelName);

  static const pluginEventChannelCurrentBrightnessChangeName = 'redbox.iot.app/system_brightness/change';
  static const pluginEventChannelCurrentBrightnessChange = EventChannel(pluginEventChannelCurrentBrightnessChangeName);

  Stream<double>? _onBrightnessChanged;

  Future<double> get brightness async {
    final result = await pluginMethodChannel.invokeMethod<double>("getBrightness");
    return result ?? 0.0;
  }

  Future<void> setBrightness(double brightness) => pluginMethodChannel.invokeMethod("setBrightness", {"brightness": brightness});

  Future<int> get screenTimeout async {
    final result = await pluginMethodChannel.invokeMethod<int>("getScreenTimeout");
    return result ?? 0;
  }

  Future<void> setScreenTimeout(int timeout) => pluginMethodChannel.invokeMethod("setScreenTimeout", {"timeout": timeout});

  Stream<double> get onBrightnessChanged {
    _onBrightnessChanged ??= pluginEventChannelCurrentBrightnessChange.receiveBroadcastStream().cast<double>();
    return _onBrightnessChanged!;
  }
}
