package com.hybris.labs.hana.minecraft.data;

public class SensorData {

	private int light;
	private int temp;

	public SensorData(int light, int temperature) {
		this.light = light;
		this.temp = temperature;
	}

	public int getLight() {
		return light;
	}

	public int getTemp() {
		return temp;
	}

	
}