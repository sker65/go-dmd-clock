package com.rinke.solutions.pinball.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

@Slf4j
public class HttpUtil {

	public int postFile(String fileName, String userName, String password, String url) throws Exception {

		HttpClientContext context = createAuthContext(userName, password, url);

		CloseableHttpClient client = HttpClients.createDefault();
		File file = new File(fileName);
		HttpPost post = new HttpPost(url);
		FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
//		StringBody stringBody1 = new StringBody("Message 1", ContentType.MULTIPART_FORM_DATA);
//		StringBody stringBody2 = new StringBody("Message 2", ContentType.MULTIPART_FORM_DATA);
		//
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addPart("fileToUpload", fileBody);
//		builder.addPart("text1", stringBody1);
//		builder.addPart("text2", stringBody2);
		HttpEntity entity = builder.build();
		//
		post.setEntity(entity);

		CloseableHttpResponse response = client.execute(post, context);
		StatusLine status = null;
		try {
			status = response.getStatusLine();
			HttpEntity resEntity = response.getEntity();
			// do something useful with the response body
			// and ensure it is fully consumed
			String res = EntityUtils.toString(resEntity);
			log.info("Status: {} res: {}",status,res);
		} finally {
			response.close();
		}
		return status.getStatusCode();
	}

	private HttpClientContext createAuthContext(String userName, String password, String url) {
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(userName, password);
		HttpClientContext context = null;
		try {
			URL u = new URL(url);
			HttpHost targetHost = new HttpHost(u.getHost(), u.getPort(), u.getProtocol());
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), creds);
			credsProvider.setCredentials(AuthScope.ANY, creds);

			// Create AuthCache instance
			AuthCache authCache = new BasicAuthCache();
			// Generate BASIC scheme object and add it to the local auth cache
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(targetHost, basicAuth);

			// Add AuthCache to the execution context
			context = HttpClientContext.create();
			context.setCredentialsProvider(credsProvider);
			context.setAuthCache(authCache);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return context;
	}
	
	public static void main(String args[]) {
		HttpUtil httpUtil = new HttpUtil();
		try {
			httpUtil.postFile("/Users/stefanri/pin2dmd.properties", "steve", "gbbataPRE7", "http://go-dmd.de/report.php");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
