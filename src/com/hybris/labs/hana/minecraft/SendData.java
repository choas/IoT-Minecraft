package com.hybris.labs.hana.minecraft;

import java.io.IOException;

import com.squareup.okhttp.*;

public class SendData {

//	private String  sensorId;
//	private String messageType;

	public static void main(String[] args) throws IOException {

//		SendData sendData = new SendData("a9da40f2-61b7-48db-ba04-5bcbae75835e",
//				"17455b98547835aa9e43");
		SendData.send(100,30);
	}
	
//	public SendData(String sensorId, String messageType) {
////		this.sensorId = sensorId;
//		this.messageType = messageType;
//	}

	public static int send(int light, int temperature)
			throws IOException {
		MediaType mediaType = MediaType
				.parse("application/json; charset=utf-8");
		
		RequestBody body = RequestBody.create(mediaType,
				"{"
				+ "\"mode\":\"sync\","
				+ "\"messageType\":\"" + Config.MESSAGE_TYPE + "\","
				+ "\"messages\":"
				+ "[{\"light\":" + light + ","
				+ "\"temperature\":" + temperature + "}]"
				+ "}");
		
		Request request = new Request.Builder()
				.url("https://iotmms" + Config.TRIAL_ACCOUNT
					+ ".hanatrial.ondemand.com/"
					+ "com.sap.iotservices.mms/v1/api/http/data/"
					+ Config.SENSOR_ID).post(body)
				.addHeader("authorization", "Bearer " + Config.TOKEN)
				.addHeader("content-type", "application/json; charset=utf-8")
				.build();

		OkHttpClient client = new OkHttpClient();
		Response response = client.newCall(request).execute();

		if (response.code() != 200) {
			System.out.println(response);
		}

		return response.code();
	}

}
