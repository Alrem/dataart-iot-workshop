Dataart IoT Workshop
=========================================

[DeviceHive](http://devicehive.com)

[DeviceHive Java Library](https://github.com/devicehive/devicehive-java)

[DataArt](http://dataart.com)

Dataart IoT Workshop is an example of using DeviceHive Java Library inside Android Application.
This example shows how to IoT communication works between Android Application and ESP8266 module.

You can see the screenshots of the Application below:

<img src="https://github.com/edubovik/dataart-iot-workshop/blob/master/images/1.png?raw=true" data-canonical-src="https://github.com/edubovik/dataart-iot-workshop/blob/master/images/1.png?raw=true" width="200" height="370" /> <img src="https://github.com/edubovik/dataart-iot-workshop/blob/master/images/2.png?raw=true" data-canonical-src="https://github.com/edubovik/dataart-iot-workshop/blob/master/images/2.png?raw=true" width="200" height="370" />

You just have to add 3 parameters to the strings.xml to make this example work:
```xml
<string name="deviceId">DEVICE_ID</string>
<string name="server_url">SERVER_URL</string>
<string name="refresh_token" formatted="false">REFRESH_TOKEN</string>
```
And basically that's it. 
