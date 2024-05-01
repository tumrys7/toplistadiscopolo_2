package com.grandline.toplistadiscopolo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

		try {
			// defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);

			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			xml = EntityUtils.toString(httpEntity, "UTF-8");

		} catch (UnsupportedEncodingException | ClientProtocolException e) {
			e.printStackTrace();
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

		try {
			// defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url2);

			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			xml_nowosci = EntityUtils.toString(httpEntity, "UTF-8");

		} catch (UnsupportedEncodingException | ClientProtocolException e) {
			e.printStackTrace();
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

		try {
			// defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(urlmoja);

			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			xml_mojalista = EntityUtils.toString(httpEntity, "UTF-8");

		} catch (UnsupportedEncodingException | ClientProtocolException e) {
			e.printStackTrace();
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
				InputStream is = new ByteArrayInputStream(xml_nowosci.getBytes("UTF-8"));
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
				InputStream is = new ByteArrayInputStream(xml_mojalista.getBytes("UTF-8"));
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
