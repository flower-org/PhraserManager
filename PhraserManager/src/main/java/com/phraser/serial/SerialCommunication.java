package com.phraser.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;

public class SerialCommunication {
    public static void main(String[] args) throws IOException {
        SerialPort serialPort = SerialPort.getCommPort("/dev/ttyACM0"); // Change to your port

        serialPort.setComPortParameters(115200, 8, 1, 0); // Match the baud rate to your RP2040
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

        if (serialPort.openPort()) {
            System.out.println("Port opened successfully.");

            // Continuously read from the serial port
            while (true) {
                // Check if data is available to read
                if (serialPort.bytesAvailable() > 0) {
                    byte[] readBuffer = new byte[1024]; // Buffer to hold incoming data
                    int numRead = serialPort.getInputStream().read(readBuffer);
                    if (numRead > 0) {
                        String receivedData = new String(readBuffer, 0, numRead);
                        System.out.print("Data received: " + receivedData); // Print received data
                    }
                }
            }


        } else {
            System.out.println("Failed to open the port. Error: " + serialPort.getLastErrorCode() + " / " + serialPort.getLastErrorLocation());
        }
    }
}