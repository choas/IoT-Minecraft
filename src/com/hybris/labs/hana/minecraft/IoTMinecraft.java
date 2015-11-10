package com.hybris.labs.hana.minecraft;

// This code is based on http://eclipsesource.com/blogs/2012/10/17/serial-communication-in-java-with-raspberry-pi-and-rxtx/

// Note:
// librxtxSerial.jnilib can be found here:
//   http://blog.iharder.net/2009/08/18/rxtx-java-6-and-librxtxserial-jnilib-on-intel-mac-os-x

// don't forget to use -Djava.library.path=/<Path>/jni

//   sudo mkdir /var/lock
//   sudo chmod 777 /var/lock  

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.hybris.labs.hana.minecraft.data.SensorData;

import pi.Block;
import pi.Minecraft;
import pi.Vec;
import pi.event.BlockHitEvent;

public class IoTMinecraft {
	
	public static void main(String[] args) {
		try {
			String minecraftServerIp = args.length > 0 ? args[0]
					: "192.168.1.11";
			String serialPortName = args.length > 1 ? args[1]
					: "/dev/cu.usbmodem1411";
			(new IoTMinecraft()).connect(minecraftServerIp, serialPortName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void connect(String minecraftIp, String portName) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier
				.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			int timeout = 2000;
			CommPort commPort = portIdentifier.open(this.getClass().getName(),
					timeout);

			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				InputStream in = serialPort.getInputStream();

				System.out.println("starting Java Minecraft demo...");
				Minecraft mc = Minecraft.connect(minecraftIp);

				System.out.println("actual player position: "
						+ mc.player.getPosition());
				(new Thread(new SerialReader(in, mc))).start();

			} else {
				System.err
						.println("Error: Only serial ports are handled by this example.");
			}
		}
	}

	public static class SerialReader implements Runnable {

		InputStream in;
		private List<SensorData> sensorData = new ArrayList<SensorData>();
		private Minecraft mc;
		private Vec lastPos = null;
		private boolean init;
		
		int prev_temp = -1;
		int prev_light = -1;

		public SerialReader(InputStream in, Minecraft mc) {
			this.in = in;
			this.mc = mc;
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
							if (values.length > 1) {
								System.out.print("values:");
								for (String value : values) {
									System.out.print(" " + value);
								}
								System.out.println("");

								SensorData v = new SensorData(
										Integer.parseInt(values[0]),
										Integer.parseInt(values[1].trim()));
								this.sensorData.add(v);
							} else {
								System.err.println("unknown data: " + lines[lines.length - 1]);
							}

						} else {
							System.out.println("*");
						}
						// clear data string
						data = "";
					}

					if (!this.sensorData.isEmpty()) {

/***************************************************************************************
 * 
 * 
 * 								D E M O S	
 * 
 * 
 ***************************************************************************************/

						
						lightPixel();

						// lightTempCurve();
						// waterCurve(false);

						// controleRoom();

						// vulcano();
						
						
						
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		private void lightPixel() {

			if (!this.init) {
				this.init = true;
				mc.player.setPosition(Vec.xyz(-9, 8, 13));
				mc.setBlocks(Vec.xyz(0, 0, 0), Vec.xyz(0, 110, 0), Block.AIR);
			}

			if (lastPos != null) {
				mc.setBlocks(lastPos, lastPos, Block.AIR);
			}
			
			int light = this.sensorData.get(this.sensorData.size() - 1)
					.getLight();
			Double y = (50.0 / 1024) * light;
			Vec vec = Vec.xyz(0, y.intValue(), 0);
			mc.setBlocks(vec, vec, Block.DIAMOND_BLOCK);
			lastPos = vec;
		}
		
		private void lightTempCurve() {
			int x = -72;

			if (!this.init) {
				this.init = true;
				clearCurve(x);
			}

			List<BlockHitEvent> hits = mc.events.pollBlockHits();
			for (BlockHitEvent event : hits) {
				if (event.position.x - 1 <= x && event.position.x + 1 >= x) {
					int index = this.sensorData.size() + event.position.z - 1;
					if (index >= 0 || index < this.sensorData.size()) {
						SensorData value = this.sensorData.get(index);
						String message = "light: " + value.getLight()
								+ " temp.: " + value.getTemp();
						mc.postToChat(message);
						System.out.println(message);
					} else {
						System.out.println("index:" + index);
					}
				} else {
					System.out.println("x:" + event.position.x);
				}
			}

			int s = this.sensorData.size() - 1;
			int z = 0;
			for (int i = s; i > s - 40 && i >= 0; i--) {
				// clear column
				mc.setBlocks(Vec.xyz(x, 0, z), Vec.xyz(x, 100, z), Block.AIR);

				int light = this.sensorData.get(i).getLight();
				Double y = (50.0 / 1024) * light;
				Vec vec = Vec.xyz(x, y.intValue(), z);

				int temp = this.sensorData.get(i).getTemp();

				pi.Color color = pi.Color.BLUE;

				if (temp >= 23 && temp < 25) {
					color = pi.Color.YELLOW;
				} else if (temp >= 25 && temp < 28) {
					color = pi.Color.ORANGE;
				} else if (temp >= 28) {
					color = pi.Color.RED;
				}

				mc.setBlock(vec, Block.wool(color));
				// Vec vec2 = Vec.xyz(x, y.intValue() + 1, z);
				// mc.setBlock(vec2, Block.LAVA);
				z--;
			}
		}
		
		private void waterCurve(boolean withWater) {
			int x = -72;

			if (!this.init) {
				this.init = true;
				clearCurve(x);
			}

			int s = this.sensorData.size() - 1;
			int i = s;
			if (i < 120) {
				int z = 0 - s;

				int light = this.sensorData.get(i).getLight();
				Double y = (50.0 / 2024) * light;
				Vec vec = Vec.xyz(x, y.intValue(), z);
				mc.setBlock(vec, Block.STONE_BRICK);
				Vec vec2 = Vec.xyz(x, y.intValue() + 1, z);
				if (withWater) {
					mc.setBlock(vec2, Block.WATER);
				}
				z--;
			}
		}
		
		private void clearCurve(int x) {
			mc.player.setPosition(Vec.xyz(-54, 1, -10));

			// clean all columns
			for (int z = 1; z > -120; z--) {
				mc.setBlocks(Vec.xyz(x - 5, 0, z), Vec.xyz(x + 5, 100, z),
						Block.AIR);
			}
		}

		private void controleRoom() {
			if (!this.init) {
				this.init = true;
				mc.player.setPosition(Vec.xyz(-127, -2, 86));
			}

			int index = this.sensorData.size() - 1;
			int temp = this.sensorData.get(index).getTemp();

			if (temp > 24) {
				mc.setBlock(Vec.xyz(-126, -1, 92), Block.wool(pi.Color.RED));
			} else {
				mc.setBlock(Vec.xyz(-126, -1, 92), Block.wool(pi.Color.LIME));
			}

			int light = this.sensorData.get(index).getLight();
			mc.setBlocks(Vec.xyz(-133, -3, 87), Vec.xyz(-133, -4, 89),
					Block.AIR);

			if (light > 100) {
				mc.setBlock(Vec.xyz(-133, -3, 89), Block.TORCH);
			}
			if (light > 200) {
				mc.setBlock(Vec.xyz(-133, -4, 89), Block.TORCH);
			}
			if (light > 300) {
				mc.setBlock(Vec.xyz(-133, -3, 88), Block.TORCH);
			}
			if (light > 400) {
				mc.setBlock(Vec.xyz(-133, -4, 88), Block.TORCH);
			}
			if (light > 500) {
				mc.setBlock(Vec.xyz(-133, -3, 87), Block.TORCH);
			}
			if (light > 600) {
				mc.setBlock(Vec.xyz(-133, -4, 87), Block.TORCH);
			}

			if (temp != prev_temp || light < prev_light - 30
					|| light > prev_light + 30) {
				prev_temp = temp;
				prev_light = light;
				mc.postToChat("light: " + light + " temp.: " + temp);
			}

		}

		private void vulcano() {

			int x = 41;
			int y = 2;
			int z = -69;

			if (!this.init) {
				this.init = true;
				mc.player.setPosition(Vec.xyz(41, 3, -71));

				mc.setBlocks(Vec.xyz(30, 1, -77), Vec.xyz(50, 4, -62 + 6),
						Block.AIR);
				mc.setBlocks(Vec.xyz(34, 0, -70), Vec.xyz(30, 0, -65),
						Block.AIR);

				for (int i = 1; i < 4; i++) {
					int d = i + 2;
					int dd = i + 2 + (i - 1);
					mc.setBlocks(Vec.xyz(35 + d, i, -77 + dd),
							Vec.xyz(47 - d, i, -62 - d), Block.GRASS);
					if (i == 3) {
						mc.setBlocks(Vec.xyz(35 + d, i + 1, -77 + dd),
								Vec.xyz(47 - d, i + 1, -62 - d - 1), Block.SNOW);
						mc.setBlock(Vec.xyz(40, 4, -68), Block.FLOWER_YELLOW);
					}
				}
				mc.setBlock(Vec.xyz(42, 1 + 1, -64 - 1), Block.GRASS);
				mc.setBlock(Vec.xyz(42, 1, -64), Block.GRASS);

				mc.setBlocks(Vec.xyz(x - 1, 1, z - 1),
						Vec.xyz(x + 1, 2, z + 1), Block.DIRT);

				mc.setBlocks(Vec.xyz(45, 0, -82), Vec.xyz(41, 0, -76),
						Block.AIR);
				mc.setBlocks(Vec.xyz(43, 0, -76), Vec.xyz(41, 0, -76),
						Block.GRASS);
				mc.setBlock(Vec.xyz(41, 0, -77), Block.GRASS);
			}

			int index = this.sensorData.size() - 1;
			int temp = this.sensorData.get(index).getTemp();

			if (temp != prev_temp) {
				prev_temp = temp;
				mc.postToChat("temp.: " + temp);
			}

			int y2 = y + ((temp - 21) / 2);

			Vec vec1 = Vec.xyz(x, y - 1, z);
			Vec vec2 = Vec.xyz(x, y2 - 1, z);
			if (temp > 29)
				mc.setBlocks(vec1, vec2, Block.WATER_FLOWING);
			else
				mc.setBlocks(vec1, vec2, Block.LAVA);
			Vec vec3 = Vec.xyz(x, y + 20, z);
			mc.setBlocks(vec2, vec3, Block.AIR);
		}
	}

}