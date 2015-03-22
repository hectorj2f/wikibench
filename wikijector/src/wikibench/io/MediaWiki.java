//	Copyright (c) 2009, E. van Baaren (erikjan@gmail.com)
//	Felipe Ledesma Botero (felipe.ledesma@gmail.com)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package wikibench.io;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Calendar;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;

import wikibench.WikiBench;


/**
 * This fetcher class uses HttpCore 4.0 from Apache Commons.  It is
 * a bit low-level compared to using httpclient. The advantage is that we do
 * not need additional libraries for logging and such. The increased
 * flexibility is also a big plus for WikiBench, since we need the
 * low-level access HttpCore offers us (like creating our own sockets).
 * 
 * Most of this was created by using and adjusting sample code from HttpCore,
 * since the documentation was a bit sparse as of writing this.
 * 
 * @author E. van Baaren (erikjan@gmail.com)
 * @author Felipe Ledesma Botero (felipe.ledesma@gmail.com)
 * 
 */
public class MediaWiki {
	HttpParams params;
	BasicHttpProcessor httpproc;
	HttpRequestExecutor httpexecutor;
	DefaultHttpClientConnection conn;
	public int lastResponseCode = 0;
	public int lastResponseSize = 0;
	public String serverHost;
	public int serverPort;
	/* This is the boundary used in multi-part/formdata POSTs */
	public String contentBoundary = "-----------------WikiBenchAaB03x";

	/**
	 * Constructor method used to create a new MediaWiki object
	 * @param serverHost	The host name of the MediaWiki server
	 * @param serverPort	The port number of the MediaWiki server
	 */
	public MediaWiki(String serverHost, int serverPort) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;

		httpproc = new BasicHttpProcessor();
		httpexecutor = new HttpRequestExecutor();
		conn = new DefaultHttpClientConnection();
		// Interceptors handle header lines, often just one single line
		// Required protocol interceptors
		httpproc.addInterceptor(new RequestContent());
		httpproc.addInterceptor(new RequestTargetHost());
		// Recommended protocol interceptors
		httpproc.addInterceptor(new RequestConnControl());
		httpproc.addInterceptor(new RequestUserAgent());
		httpproc.addInterceptor(new RequestExpectContinue());

		/* Setup some basic parameters */
		params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, WikiBench.userAgent);
		HttpConnectionParams.setConnectionTimeout(params, WikiBench.sutTimeout);
		HttpConnectionParams.setSoTimeout(params, WikiBench.sutTimeout);
		HttpConnectionParams.setLinger(params, 0);
		
		/* Do not use Expect: 100-Continue header, since it will complicate
		 * posting data  */
		HttpProtocolParams.setUseExpectContinue(params, false);
	}
/**
 * Perform a HTTP post request to the MediaWiki server. The function is better
 * not called directly, but instead through doEdit() since that method will
 * contruct a proper postContent string
 * 
 * @param serverPath	The full path to use in the post request, including the initial '/'
 * @param postContent	The raw content to be posted
 * @return true if the post was successful, false otherwise
 */
	public boolean doPOST(String serverPath, String postContent) {  		
		log("initializing POST request");
		HttpContext context = new BasicHttpContext(null);
		HttpHost host = new HttpHost(serverHost, serverPort);
		//ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
		this.lastResponseCode = 0;
		this.lastResponseSize = 0;
		try {
			log("creating socket");
			Socket socket = new Socket();
			log("finished creating socket... now binding with a null");
			socket.bind(null);
			log("finished binding with a null, now setting some of its variables");				
			socket.setSoTimeout(WikiBench.sutTimeout);
			socket.setSoLinger(true, 0);
			log("connecting to host...");	
			socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), WikiBench.sutTimeout);
			log("finished connecting, now binding socket with the connection");
			conn.bind(socket, params);		
			conn.setSocketTimeout(WikiBench.sutTimeout);

			HttpEntity requestBody = new StringEntity(postContent, "UTF8");
			BasicHttpEntityEnclosingRequest request = 
				new BasicHttpEntityEnclosingRequest("POST", serverPath);
			request.setEntity(requestBody);
			
			/* MediaWiki expects a multipart encoded message */
			request.setHeader("Content-Type", "multipart/form-data; boundary=" + contentBoundary);
			//request.setHeader("Referer", "Here you can insert a referer field");
			context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
			request.setParams(params);
			httpexecutor.preProcess(request, httpproc, context);
			log("executing POST");
			HttpResponse response = httpexecutor.execute(request, conn, context);
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);

			String content = EntityUtils.toString(response.getEntity());
			this.lastResponseSize = content.length();
			this.lastResponseCode = response.getStatusLine().getStatusCode();

			log("shutting down connection");
			conn.shutdown();
			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (ConnectException e) {
			if (WikiBench.verbose)	System.out.println(e.getLocalizedMessage());
			return false;
		} catch (SocketException e) {
			if (WikiBench.verbose)	System.out.println(e.getLocalizedMessage());
			return false;
		} catch (SocketTimeoutException e) {
			if (WikiBench.verbose)	System.out.println("Socket timeout!");
			return false;
		} catch (NoHttpResponseException e) {
			if (WikiBench.verbose)	System.out.println("No http response!");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (HttpException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			return false;
		} finally {
			try {
				conn.shutdown();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
	
	/**
	 * Perform a GET operation.
	 * 
	 * @param	serverPath	the full path on the server, including the initial
	 * 						'/' and also including all GET parameters.
	 * @return true if the get request was successful, false otherwise
	 */
	public boolean doGET(String serverPath) {
		log("initializing GET request");
		HttpContext context = new BasicHttpContext(null);
		HttpHost host = new HttpHost(serverHost, serverPort);
		//ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
		this.lastResponseCode = 0;
		this.lastResponseSize = 0;
		
        
		try {
			log("creating socket");
			Socket socket = new Socket();
			log("finished creating socket... now binding with a null");
			socket.bind(null);
			log("finished binding with a null, now setting some of its variables");				
			socket.setSoTimeout(WikiBench.sutTimeout);
			socket.setSoLinger(true, 0);
			log("connecting to host...");	
			socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), WikiBench.sutTimeout);
			log("finished connecting, now binding socket with the connection");
			conn.bind(socket, params);		
			conn.setSocketTimeout(WikiBench.sutTimeout);				
			log("basic http request");
			BasicHttpRequest request = new BasicHttpRequest("GET", serverPath);
			context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
			log("request params");
			request.setParams(params);
			httpexecutor.preProcess(request, httpproc, context);
			log("executing GET");
			HttpResponse response = httpexecutor.execute(request, conn, context);
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);

			/* Get the content, get the size, throw away content */
			String content = EntityUtils.toString(response.getEntity());
			this.lastResponseSize = content.length();
			this.lastResponseCode = response.getStatusLine().getStatusCode();
			log("shutting down connection");
			conn.shutdown();
			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (SocketTimeoutException e) {
			log("Socket timeout!");
			return false;
		} catch (NoHttpResponseException e) {
			log("No http response!");
			return false;
		} catch (ConnectException e) {
			log("Could not connect to remote host. ");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (HttpException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			//System.err.println(e.getLocalizedMessage());
			return false;
		} finally {
			try {
				conn.shutdown();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
/**
 * Perform a page edit. This function uses doPOST() and takes care of constructing
 * the proper post parameters.
 * @param The full path to use in the post request, including the initial '/'
 * @param content The full wiki page content to be posted
 * @param wpEdittime use null to let this method construct a proper wpEdittime value
 * @return true if the page edit was successful, false otherwise
 */
	public boolean doEdit(String path, String content, String wpEdittime) {
		/* If this page is new, for example, it has no Edittime.
		 * Using the current time is what MediaWiki does in such cases */
		if (wpEdittime == null) {
			wpEdittime = getStartTime();
		}
		String post = constructPost(content, wpEdittime);
		doPOST(path,post);
		return true;
	}
	
	String constructPost(String wpTextbox1, String wpEdittime) {
		String content=
			multiPartContent("wpSummary", "summary") +
			multiPartContent("wpSection", "") +
			multiPartContent("wpTextbox1", wpTextbox1) +
			multiPartContent("wpEditToken", "+\\") +
			multiPartContent("wpStarttime", getStartTime()) +
			multiPartContent("wpEdittime", wpEdittime)+
			multiPartContent("wpScrolltop", "") +
			multiPartContent("wpAutoSummary", "d41d8cd98f00b204e9800998ecf8427e") +
			multiPartContent("wpSave", "Save page") +
			multiPartTail();
		return content;
	}
	
	/** Format an additional  (field,data) tuple to add to the POST request
	 * 
	 * @return a String containing the field and the data, properly formatted
	 * so it can be appended to a new or existing string with other (field,data)
	 * tuples.
	 */
	private String multiPartContent(String field, String data) {
		return 	"--"+contentBoundary+"\r\n" +
				"Content-Disposition: form-data; name=\""+
				field+"\"\r\n\r\n"
				+data+"\r\n";
	}
	
	/** Format the tail of a multipart/formdata POST
	 * 
	 * @return	a string containing the content boundary and an additional
	 * carriage return to end multipart/formdata request
	 */
	String multiPartTail() {
		return contentBoundary+"\r\n";
	}
	/**
	 * Construct a valid wpEdittime to be used by MediaWiki. This method will 
	 * use the current system time to construct this value
	 * @return a String containing a valid wpEdittime based on current system time
	 */
	private String getStartTime() {
		Calendar now = Calendar.getInstance();
		String year = Integer.toString(now.get(Calendar.YEAR));
		String month = Integer.toString(now.get(Calendar.MONTH));
		String day = Integer.toString(Calendar.DAY_OF_MONTH);
		String hour = Integer.toString(Calendar.HOUR_OF_DAY);
		String minutes = Integer.toString(Calendar.MINUTE);
		String seconds = Integer.toString(Calendar.SECOND);
		return year + month + day + hour + minutes + seconds;
	}
	
	private static void log(String s)
	{
		if (WikiBench.verbose == true) {
	        System.out.println(Thread.currentThread().getId() + " " + System.currentTimeMillis() + " " + s);
		}
	}
}