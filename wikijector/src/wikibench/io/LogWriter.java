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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
/**
 * The LogWriter class is used to write all requests made by a WikiBench worker
 * to a file. These statistics can later be gathered, merged and sorted.
 * 
 * @author E. van Baaren (erikjan@gmail.com)
 * @author Felipe Ledesma Botero (felipe.ledesma@gmail.com)
 *
 */
public class LogWriter {
	PrintWriter out;
	/**
	 * Construct new LogWriter object
	 * 
	 * @param fileName The full path to the log file name
	 */
	public LogWriter(String fileName) {		
		 
		// open the file for writing
		try {
			/* Open the file for binary writing, overwrites file if exists */
			out = new PrintWriter (new BufferedWriter(new FileWriter(fileName)));
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open the log file " + fileName + " for writing");
			System.exit(1);
		} catch (IOException e) {			
			e.printStackTrace();
			System.exit(1);
		} 
	}
	
	/**
	 * Write a line to the log file
	 * 
	 * @param timeStamp The time at which the request was initiated
	 * @param requestType The time of request (GET or POST)
	 * @param responseTime time it took for the server to respond
	 * @param responseCode the response code as retrieved from the server, or 0 if none
	 * @param responseSize the size of the raw response from the server
	 * @param url the url that was requested
	 * @return true if the entry has been logged, or false in case of an IOException
	 */
	public synchronized boolean log(long timeStamp, String requestType, long responseTime, int responseCode, 
			int responseSize, String url) {
		out.print(timeStamp);
		out.print(" - ");
		out.print(requestType);
		out.print(" - ");
		out.print(responseTime);
		out.print(" - ");
		out.print(responseCode);
		out.print(" - ");
		out.print(responseSize);
		out.print(" - ");
		out.print(url);
		out.println();
		return true;
	}
	
	/**
	 * Properly close the file
	 * @return true on success, false when an IOException occurs
	 */
	public boolean close() {
		out.close();
		return true;
	}
}
