# BearConsole2
This application connect to the altimeter via bluetooth, USB or 3DR telemetry modules.
It is an alternative to the Java application and using a terminal and run commands manually.
It runs some sort of AT commands and allow configuration of the altimeter as well as flight retrieval and telemetry using an Android tablet or phone.
It also has the ability to flash the altimeter firmware using an OTG cable so that it is in sync with the console application.


<img src="/pictures/bearconsole_about.png" width="29%"> <img src="/pictures/bearconsole_bluetooth_list.png" width="29%"> 
<img src="/pictures/bearconsole_app_config1.png" width="29%"> <img src="/pictures/bearconsole_app_config2.png" width="29%">
<img src="/pictures/bearconsole_board_cfg1.png" width="29%"> <img src="/pictures/bearconsole_curves.png" width="29%">

With 3DR module you can also do telemetry                                         
<img src="/pictures/bearconsole_telemetry.png" width="29%">

# Getting the app
You should be able to find the app on the google store, however it will not have all the latest fonctionnalities. You can subscribe to the beta test on the store. If you want the latest one, compile the code from the provided source. Also if your device is not fully supported that might be a good idea to try to fix it and release the code. Android support so many screen sizes and devices that it is difficult to test an application on all of them.
If you do not know how to build an Android application go to the release dirrectory, download the APK and install it on your phone.
Alternatively you could search the Google playstore and get the app from there.  

# Building the code
You will need to use Android studio (I am no longer using Eclipse for Android developement).

# Bug reports
Please make sure you report any bugs so that I can fix them. If you have any ideas to improve the app please feel free to contact me.

# Supported altimeters
The following altimeters can be used with the BearConsole
- [AltiDuo](https://github.com/bdureau/AltiDuo_console) (this has a special firmware)
- [AltiServo](https://github.com/bdureau/AltiServo)
- [Altimulti](https://github.com/bdureau/RocketFlightLogger)(all versions)
- [AltiGPS](https://github.com/bdureau/AltiGPS)

# Ability to flash the altimeter latest firmware
From your Android device you can flash your altimeter with the latest firmware this will make sure that the altimeter firmware is compatible with the board you are using.

# Getting altimeter boards
If you need an altimeter board you can can either build it yourself using Arduino compatible hardware or contact me to get a board

# Disclamer
This is a work in progress project, not all Android devices might not be fully supported but if you find any bugs please make sure that you do report them so that I can fix them. 
