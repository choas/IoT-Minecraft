package com.hybris.labs.hana.minecraft;

import java.io.IOException;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class SendData {

	public static void main(String[] args) throws IOException {

		// test data
		SendData.send(100,30);
	}

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
