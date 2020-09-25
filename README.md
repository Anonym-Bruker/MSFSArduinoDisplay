# MSFSArduinoDisplay

This project is the Java-code for a MSFS2020-display

It is used for displaying information sent over the serial port to a 16x2-display and to light up three gear indicator LEDS

The information flow is:
MSFS2020->
  FSUIPC7->
    Mouseeviator JavaJDK->
      com.fazecast.jSerialComm library.->
        Arduino Sketch code ->
          LiquidCrystal library (for the LCD-display)

The LEDS are displaying status for each gear
  - LED off if gear is up
  - LED on if gear is down
  - Flashing LED if moving/in transition

The 16x2-display is showing:
  - The COM1 frequency and STDBY frequency with three decimals 
  - The heading as an integer

This is a project based on the following libraries:

  https://mouseviator.com/fsuipc-java-sdk/

  https://github.com/mschoeffler/arduino-java-serial-communication


A picture of the ugly-looking prototype:

  https://github.com/Anonym-Bruker/MSFSArduinoDisplay/blob/master/20200925-MSFS2020-Panel.jpg
