//	Copyright (c) 2009, E. van Baaren (erikjan@gmail.com)
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.util.Random;

/**
 * The TraceReader class can be used for both reading trace files and for reading
 * from a socket over which traces are streamed. So controller and workers use
 * this class. 
 * 
 * The workers can also send back a message through this class to notify the 
 * controller of an error.
 * 
 * A TraceReader object is a single point of access to a file or stream, so most
 * methods are synchronized to make this class thread safe.
 */
public class TraceReader {
	private String firstLine = null;
	private BufferedReader in;
	private BufferedWriter out = null;
	private long numLinesSend;
	Random generator;
	
	/**
	 * Contructor class
	 * 
	 * @param in InputStreamReader from which the trace lines are read
	 * @param out (Optional) OutputStreamWriter to which a worker can send messages
	 * to the controller. Use null when this is not needed.
	 */
	public TraceReader(InputStreamReader in, OutputStreamWriter out) {
		this.in = new BufferedReader(in);
		if (out != null) this.out = new BufferedWriter(out);
		this.numLinesSend = 0;
	}
	
	/** 
	 * Get the next line from the trace stream. This method is synchronized.
	 * 
	 * @return line A String containing the raw line obtained from the trace
	 * file. Null if end of file has been reached or if an exception occurred
	 */
	public synchronized String getLine() {
		String line = null;
		/* Send the first line to the first one requesting a regular trace line */
		if (numLinesSend==0) {
			if (firstLine != null) {
				numLinesSend=1;
				return firstLine;
			}
		}
		
		try {
			line = in.readLine();
		} catch (SocketException e) {
			/* This sometimes happens, most often the controller has not
			 * closed the connection properly */
			System.err.println(e.getLocalizedMessage());
			return null;
		} catch (IOException e) {
			System.err.println("An I/O exception has occurred while reading from standard input");
			System.err.println(e.getLocalizedMessage());
			return null;
		}
		if (line!=null)	numLinesSend++;
		return line;
	}
	
	/** 
	 * Get the first line from the trace. If the first line has not been fetched yet,
	 * it is obtained from the trace file, otherwise it is cached in this object.
	 * This method is useful for the controller in order to obtain the lowest time
	 * value from the trace file and send that time to the workers.
	 * This method is synchronized.
	 * 
	 * @return	firstLine	A String containing the raw first line from the trace file
	 */
	public synchronized String getFirstLine() {
		if (firstLine == null) {
			firstLine = getLine();
		}
		numLinesSend = 0;
		return firstLine;
	}
	
	/** 
	 * Send a string to the other side, used by workers to notify the
	 * controller of an error.
	 */
	public synchronized boolean writeLine(String msg) {
		try {
			out.write(msg);
			out.newLine();
			out.flush();
			return true;
		} catch (IOException e) {
			System.err.println("Could not write to other side");
			return false;
		}
	}
	
	/**
	 * Properly close input and output streams.
	 */
	public void close() {
		try {
			in.close();
			if (out!=null) out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
