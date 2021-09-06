# Device fingerprint
Original code from [fingerprintjs-android](https://github.com/fingerprintjs/fingerprint-android)
Suited for particular need, no hardware based fingerprinting. 
Sample application for testing, generating hashes in manner: 

```
<Android ID> | <GSF ID> | &lt <Media DRM ID>
```
GSF ID and Android ID are regenerated after factory reset, Media DRM ID stays the same.
GSF ID needs `https://bytefreaks.net/android/android-get-gsf-id-google-services-framework-identifier` permission.  

Available signal providers for hardware fingerprinting:
* BatteryInfoProvider
* CameraInfoProvider
* CpuInfoProvider
* GpuInfoProvider
* InputDeviceDataSource
* MemInfoProvider
* OsBuildInfoProvider
* SensorDataSource



