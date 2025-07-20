package com.grandline.toplistadiscopolo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.nio.charset.StandardCharsets;

public class XMLParser {
	private android.util.Log Log;
	// constructor
	public XMLParser() {

	}

	/**
	 * Getting XML from URL making HTTP request
	 * @param url string
	 * */
	public String getXmlFromUrl(String url) throws IOException {
		String xml = null;
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
				xml = result.toString();
			}
		} finally {
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
		}
		// return XML
		return xml;
	}

	/**
	 * Getting XML from URL making HTTP request
	 * @param url2 string
	 * */
	public String getXmlFromUrl2(String url2) throws IOException {
		String xml_nowosci = null;
		HttpURLConnection httpURLConnection = null;
		
		try {
			URL requestURL = new URL(url2);
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
				xml_nowosci = result.toString();
			}
		} finally {
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
		}
		// return XML
		return xml_nowosci;
	}

	/**
	 * Getting XML from URL making HTTP request
	 * @param urlmoja string
	 * */
	public String getXmlFromUrlMoja(String urlmoja) throws IOException {
		String xml_mojalista = null;
		HttpURLConnection httpURLConnection = null;
		
		try {
			URL requestURL = new URL(urlmoja);
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
				xml_mojalista = result.toString();
			}
		} finally {
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
		}
		// return XML
		return xml_mojalista;
	}
	
	/**
	 * Getting XML DOM element
	 * @param xml string
	 * */
	public Document getDomElement(String xml){
		Document doc;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setCoalescing(true);//aby obsluzyc znak & w xml
		try {

			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
		        is.setCharacterStream(new StringReader(xml));
		        doc = db.parse(is);

		} catch (ParserConfigurationException | SAXException | IOException e) {
			Log.e("Error xx: ", e.getMessage());
			return null;
		}
	        return doc;
	}

		public Document getDomElement2(String xml_nowosci){
			Document doc2;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setCoalescing(true);//aby obsluzyc znak & w xml
			try {

				DocumentBuilder db = dbf.newDocumentBuilder();

				//InputSource is = new InputSource();
				InputStream is = new ByteArrayInputStream(xml_nowosci.getBytes(StandardCharsets.UTF_8));
				//is.setCharacterStream(new StringReader(xml_nowosci));
				doc2 = db.parse(is);

			} catch (ParserConfigurationException | SAXException | IOException e) {
				//	Log.e("Error2: ", e.getMessage());
				return null;
			}

			return doc2;
		}


		public Document getDomElementMoja(String xml_mojalista){
			Document docmoja;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setCoalescing(true);//aby obsluzyc znak & w xml
			try {

				DocumentBuilder db = dbf.newDocumentBuilder();

				//InputSource is = new InputSource();
				InputStream is = new ByteArrayInputStream(xml_mojalista.getBytes(StandardCharsets.UTF_8));
				//is.setCharacterStream(new StringReader(xml_mojalista));
				docmoja = db.parse(is);

			} catch (ParserConfigurationException | SAXException | IOException e) {
				//		Log.e("Error2: ", e.getMessage());
				return null;
			}
			return docmoja;
		}
	/** Getting node value
	  * @param elem element
	  */
	 public final String getElementValue( Node elem ) {
	     Node child;
	     if( elem != null){
	         if (elem.hasChildNodes()){
	             for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
	                 if( child.getNodeType() == Node.TEXT_NODE  ){
	                     return child.getNodeValue();
	                 }
	             }
	         }
	     }
	     return "";
	 }
	 
	 /**
	  * Getting node value
	  * @param item node
	  * @param str string
	  * */
	 public String getValue(Element item, String str) {		
			NodeList n = item.getElementsByTagName(str);		
			return this.getElementValue(n.item(0));
		}
}
