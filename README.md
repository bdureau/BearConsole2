# BearConsole2
This application connect to the altimeter via bluetooth, USB or 3DR telemetry modules.
It is an alternative to the Java application and using a terminal and run commands manually.
It runs some sort of AT commands and allow configuration of the altimeter as well as flight retrieval and telemetry using an Android tablet or phone.


<img src="/pictures/altimulti_bluetooth.png" width="40%"> <img src="/pictures/altimulti_config.png" width="40%">
<img src="/pictures/altimulti_flight_graph.png" width="69%">
<img src="/pictures/altimulti_flight_list.png" width="40%"> <img src="/pictures/altimulti_mainscreen.png" width="40%">

With 3DR module you can also do telemetry                                         
<img src="/pictures/altimulti_telemetryV2.jpg" width="40%">

# Getting the app
You should be able to find the app on the google store, however it will not have all the latest fonctionnalities. If you want the latest one, compile the code from the provided source. Also if your device is not fully supported that might be a good idea to try to fix it and release the code. Android support so many screen sizes and devices that it is difficult to test an application on all of them.

# Building the code
You will need to use Android studio (I am no longer using Eclipse for Android developement).

# Supported altimeters
The following altimeters can be used with the BearConsole
- [AltiDuo] (https://github.com/bdureau/AltiDuo_console) (this has a special firmware)
- [AltiServo] (https://github.com/bdureau/AltiServo)
- [Altimulti] (https://github.com/bdureau/RocketFlightLogger)(all versions)

# Disclamer
This is a work in progress project, not all Android devices might not be fully supported but if you find any bugs please make sure that you do report them so that I can fix them. 
