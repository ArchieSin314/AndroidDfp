<div>
    <h1> Device fingerprint </h1>
    <p>
        Code from https://github.com/fingerprintjs/fingerprint-android
        <br>
        Suited for particular need
        <br>
        Sample application for testing, generating hashes in manner: 
        <br>
        <b>&lt android ID &gt | &lt gsf ID &gt | &lt media DRM ID &gt</b>
        <br>
        Some hashes generated in this manner may be unavailable. On emulated Android, gsf ID is Null.
        <br>
        Got rid of fingerprinting based on hardware signals, needs testing how durable / variable hashes are.
        Available signal providers for hardware fingerprinting:
        <ul>
            <li>BatteryInfoProvider</li>
            <li>CameraInfoProvider</li>
            <li>CpuInfoProvider</li>
            <li>GpuInfoProvider</li>
            <li>InputDeviceDataSource</li>
            <li>MemInfoProvider</li>
            <li>OsBuildInfoProvider</li>
            <li>SensorDataSource</li>
        </ul>
    </p>
</div>
