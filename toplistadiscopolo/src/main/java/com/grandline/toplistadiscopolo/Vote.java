package com.grandline.toplistadiscopolo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Vote {

	// constructor
	public Vote() {

	}

	/**
	 * making HTTP request
	 * @param url string
	 * */
	public String setVoteInUrl(String url) throws IOException {
		String html = null;
		String info = "";
		String htmlEnd = "";

		try {
			// defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);

			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			
			html = EntityUtils.toString(httpEntity, "UTF-8");
			
			if (html.indexOf(Constants.TEXT_VOTE_INFO)==0){
				htmlEnd = html.substring(html.indexOf(Constants.TEXT_VOTE_ERROR)+Constants.TEXT_VOTE_ERROR.length());
			}else{
				htmlEnd = html.substring(html.indexOf(Constants.TEXT_VOTE_INFO)+Constants.TEXT_VOTE_INFO.length());
			}
			info = htmlEnd.substring(0, htmlEnd.indexOf("<"));
			

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} 
		// return XML
		return info;
	}
	
}
