package com.grandline.toplistadiscopolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Vote {

	// constructor
	public Vote() {

	}

	/**
	 * making HTTP request
	 * @param url string
	 * */
	public String setVoteInUrl(String url) throws IOException {
		String html;
		String info = "";
		String htmlEnd;
		HttpURLConnection httpURLConnection = null;

		try {
			URL requestURL = new URL(url);
			httpURLConnection = (HttpURLConnection) requestURL.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setConnectTimeout(30000); // 30 seconds
			httpURLConnection.setReadTimeout(30000); // 30 seconds
			httpURLConnection.setRequestProperty("User-Agent", "Android App");

			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));
				StringBuilder result = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					result.append(line);
				}
				reader.close();
				html = result.toString();

				if (html.indexOf(Constants.TEXT_VOTE_INFO)==0){
					htmlEnd = html.substring(html.indexOf(Constants.TEXT_VOTE_ERROR)+Constants.TEXT_VOTE_ERROR.length());
				}else{
					htmlEnd = html.substring(html.indexOf(Constants.TEXT_VOTE_INFO)+Constants.TEXT_VOTE_INFO.length());
				}
				info = htmlEnd.substring(0, htmlEnd.indexOf("<"));
			}
		} finally {
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
		}
		// return info
		return info;
	}

}
