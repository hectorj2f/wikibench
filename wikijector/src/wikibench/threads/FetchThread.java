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

package wikibench.threads;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import wikibench.WikiBench;
import wikibench.io.MediaWiki;
import wikibench.io.LogWriter;
import wikibench.io.TraceReader;

/**
 * FetchThread is used by WikiBench workers to perform HTTP requests. A global
 * TraceReader is used to get trace lines. FetchThread parses control messages
 * and trace lines and performs the appropriate actions.
 * 
 * @author Erik-Jan (erikjan@gmail.com)
 * @author Felipe Ledesma Botero (felipe.ledesma@gmail.com)
 *
 */
public class FetchThread extends java.lang.Thread {
	private TraceReader traceReader;
	private LogWriter logWriter;
	private String currentPath, currentPostData,currentEdittime;
	private long currentTime, startTime, endTime;
	private MediaWiki mediaWiki;
	
	public FetchThread(TraceReader traceReader, LogWriter logWriter) {
		this.traceReader = traceReader;
		this.logWriter = logWriter;
		this.mediaWiki = new MediaWiki(WikiBench.sutHost, WikiBench.sutPort);
		log("created");
	}
	/**
	 * Run a loop that does the following: fetch traceline,
	 * parse the line, process the request: either a control msg or a
	 * GET/POST request
	 */

	private void log(String s)
	{
		if (WikiBench.verbose == true) {
	        System.out.println(getId() + " " + System.currentTimeMillis() + " " + s);
		}
	}
	
	public void run() {
		log("running");
		String line;
		while(true) {
			log("reading trace from controller");
			line = traceReader.getLine();
			log("finished reading trace from controller. going to parse line");
			if (line==null) { /* Check if EOF is reached */
				log("exiting due to EOF");
				return;  
			}
			if (lineParser(line)) 				/* Extract values from line.. */
				
				//if (!processRequest()) return;	/* ..and process request if it 
				processRequest();	// modified by Felipe to avoid shutting down the experiment when missed deadlines happen
													/* was not a control message */					
		}
	}
	/**
	 * Parse a raw trace line. If the line contains a control message,
	 * perform the appropriate action and return directly, otherwise a HTTP
	 * POST or GET request is performed and logged to the logfile.
	 * 
	 * @param line	The raw String from the trace file
	 * @return false if a control message was detected, true otherwise
	 */
	private boolean lineParser(String line) {
		log("parsing line");
		String[] parts;
		
		/* First check if this is a control message or a regular trace line */
		if (line.startsWith("@control")) {
			/* Split into parts: slow, but it only happens a few times */
			parts = line.split(" ");
			if (parts[1].equals("firstline")) {
				/* extract the lowest time value from the first trace line */
				WikiBench.lowestTime = Long.parseLong(parts[2].trim());;
			} else
			if (parts[1].equals("starttime")) {
				WikiBench.startTime = Long.parseLong(parts[2]);
			}
			/* return false, so we don't continue processing this request */
			return false;
		}
		parts = line.split("\\s");				/* Split the trace line on white space */
		currentTime = Long.parseLong(parts[0]); /* Get the time stamp  */
		currentPath = "/" + parts[1];			/* Get the url, add a slash */
		/*replace wiki or w with /mediawiki/*/
		//currentPath = parts[1];
		
		// Corina - modifications for running under ConPaaS
		if (currentPath.startsWith("/wiki/") && !currentPath.contains(".php"))
				currentPath = currentPath.replaceFirst("/wiki/", "/index.php?title=");
		if(currentPath.startsWith("/w/"))
			currentPath = currentPath.replaceFirst("/w/", "/");
		if(currentPath.startsWith("/skins-1.5/"))
                        currentPath = currentPath.replaceFirst("/skins-1.5/", "/skins/");


		currentPostData = parts[2];				/* Get the post data, if any */
		
		if (currentPostData.length()>4) {		/* Check if there is post data */
			/* In this case we extract the page data and the timestamp */
			int splitPoint = currentPostData.indexOf('|');

			try {
				currentPostData = URLDecoder.decode(currentPostData.substring(splitPoint), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				/* This error should be very rare */
				System.err.println(e.getLocalizedMessage());
				System.err.println("Since there is no UTF-8 support, we will be" +
						"posting url encoded data. This is ugly!");
			}
		} else if (currentPostData.equals("save")){
			/* Reset the timestamp from any previous parse actions */
			currentEdittime = null;
			/* Add some dummy data */
			currentPostData = "This content was posted from WikiBench, the" +
					" benchmarking tool. This content is only posted when there" +
					"is no content available in the trace file used by WikiBench." +
					"This can for example happen when the page did not exist yet.";
		}
		log("parsing line (end)");
		return true;
	}

	/**
	 * Process a GET or a POST request.
	 * 
	 * @return true if the request was processed, false an error occurred. Errors
	 * can occur when a deadline was missed or when GET or POST requests failed 
	 * because of an exception in which case the error is printed on System.out.
	 */
	private boolean processRequest()  {
		log("Starting new processRequest. Calculating sleep time");
		boolean result;
		
		long sleepTime = WikiBench.startTime + (currentTime - WikiBench.lowestTime) - System.currentTimeMillis();
		if (sleepTime < 0) {
			System.out.println("Missed a deadline by " + sleepTime + " milliseconds for url:"+currentPath);
			//traceReader.writeLine("Missed a deadline by " + sleepTime + " milliseconds.");
			WikiBench.incrementMissedDeadlines();
			log("processing request (sleep time < 0, MD, exiting)");
			return false;
		}
		log("finished calculating sleep time. sleeping " + sleepTime + " ms ");
		try { sleep(sleepTime); } catch (InterruptedException e) {}
		
		/* Fetch the page if this is a GET request */
		if (currentPostData.equals("-")) {
			log("finished sleeping. sending GET");
			//startTime = System.currentTimeMillis();
			startTime = System.nanoTime();
			//result = mediaWiki.doGET(currentPath);
			result = mediaWiki.doGET(currentPath);
			//result = mediaWiki.doGET(currentPath);
			//endTime = System.currentTimeMillis();
			endTime = System.nanoTime();
			log("obtained response to GET. writing to log file");
			logWriter.log((currentTime - WikiBench.lowestTime), "GET", (endTime-startTime)/1000000, mediaWiki.lastResponseCode, 
					mediaWiki.lastResponseSize, currentPath);
            log("finished writing to log file " + (endTime-startTime)/1000000);
		}
		/* This is a POST request */
		else {
			//startTime = System.currentTimeMillis();
			startTime = System.nanoTime();
			/* Do the edit, pass on the currentTime as obtained from the trace */
			if (WikiBench.noEdits) {
				/* Do the best next thing, GET the page */
                log("finished sleeping. sending GET");
				result = mediaWiki.doGET(currentPath);
                log("obtained response to GET. writing to log file");
			}
			else { 
                log("finished sleeping. sending POST");
				result=mediaWiki.doEdit(currentPath, currentPostData, currentEdittime);
                log("obtained response to POST. writing to log file");
			}
			//endTime = System.currentTimeMillis();
			endTime = System.nanoTime();
			logWriter.log((currentTime - WikiBench.lowestTime), "POST", (endTime-startTime)/1000000, mediaWiki.lastResponseCode, 
					mediaWiki.lastResponseSize, currentPath);
            log("finished writing to log file. ended processRequest " + (endTime-startTime)/1000000);
			
		}
		if (WikiBench.verbose == true) {
			System.out.println("T: " + (currentTime - WikiBench.lowestTime) + 
					"\tR: " + mediaWiki.lastResponseCode + "\tRT: " + (endTime-startTime)/1000000 + 
					"\tRS: " + mediaWiki.lastResponseSize + "\tU: " + currentPath);
		}
		log("processing request (end)");
		return result;
	}
}
