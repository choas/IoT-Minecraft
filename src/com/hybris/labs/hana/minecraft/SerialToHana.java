// This code is based on http://eclipsesource.com/blogs/2012/10/17/serial-communication-in-java-with-raspberry-pi-and-rxtx/

// Note:
// librxtxSerial.jnilib can be found here:
//   http://blog.iharder.net/2009/08/18/rxtx-java-6-and-librxtxserial-jnilib-on-intel-mac-os-x

// don't forget to use -Djava.library.path=/<Path>/jni

//   sudo mkdir /var/lock
//   sudo chmod 777 /var/lock  

package com.hybris.labs.hana.minecraft;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;

public class SerialToHana {

	public static void main(String[] args) {
		try {
			(new SerialToHana()).connect(args.length > 0 ? args[0]
					: "/dev/cu.usbmodem1411");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void connect(String portName) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier
				.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			System.err.println("Error: Port is currently in use");
		} else {
			int timeout = 2000;
			CommPort commPort = portIdentifier.open(this.getClass().getName(),
					timeout);

			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				InputStream in = serialPort.getInputStream();

				(new Thread(new SerialReader(in))).start();

			} else {
				System.err.println("Error: Only serial ports are handled by this example.");
			}
		}
	}

	public static class SerialReader implements Runnable {

		InputStream in;

		public SerialReader(InputStream in) {
			this.in = in;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int len = -1;
			try {
				String data = "";

				while ((len = this.in.read(buffer)) > -1) {

					String bStr = new String(buffer, 0, len);

					data += bStr;

					if (data.contains("\n")) {
						String lines[] = data.split("\n");

						if (lines.length > 0) {
							String values[] = lines[lines.length - 1].split(";");
							
							if (values.length < 2) {
								continue;
							}
							
							System.out.println("values: " + values[0] + " " + values[1]);
							
							// next Demo :) ...
							SendData.send(
									Integer.parseInt(values[0]), 
									Integer.parseInt(values[1].trim()));
							
						} else {
							System.out.println("*");
						}

						data = "";
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}