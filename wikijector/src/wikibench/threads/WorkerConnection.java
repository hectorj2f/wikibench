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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import wikibench.WikiBench;
import wikibench.io.TraceReader;

/**
 *  This class is part of the controller. It forms the connection to a
 * worker node. For each node, there is one WorkerConnection object.
 * 
 * @author E. van Baaren (erikjan@gmail.com)
 * @author Felipe Ledesma Botero (felipe.ledesma@gmail.com)
 * 
 */
public class WorkerConnection extends java.lang.Thread {
	Socket connection;
	OutputStream out;
	BufferedReader in;
	TraceReader traceReader;
	CountDownLatch startSignal;
	Random random;
	
	/**
	 * Contruct a new WorkerConnection object
	 * 
	 * @param connection A TCP Socket to the worker
	 * @param traceReader The TraceReader object from which this WorkerConnection
	 * should read trace lines
	 * @param startSignal A CountDownLatch, used to notify all WorkerConnections
	 * that the required number of workers has connected
	 */
	public WorkerConnection(Socket connection, TraceReader traceReader, CountDownLatch startSignal) {
		this.connection = connection;
		this.traceReader = traceReader;
		this.startSignal = startSignal;
		
		try {
			connection.setTcpNoDelay(true);		/* Make sure we turn of Nagles algorithm */
			connection.setReuseAddress(true);	/* Reuse sockets that are in time_wait state */
			
			/* Create our input and output streams */
			out = new BufferedOutputStream(connection.getOutputStream());
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		} catch (SocketException e) {
			// error setting socket options
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// error creating in / out streams
			e.printStackTrace();
			return;
		} finally {
			System.out.println("New WorkerConnection is now running");
		}
	}
	/**
	 * Loop that reads trace lines and sends these lines to the connected worker,
	 * until end of file has been reached.
	 */
	public void run() {
		String line;
		byte[] message;
		random = new Random();
		
		try {
			/* First of all send the first line to the worker, so it can extract the lowest time value */
			line = traceReader.getFirstLine();
			line = "@control firstline " + line + "\n";
			message = line.getBytes("UTF8");
			out.write(message);
			out.flush();
			
			/* Now we wait for the start signal, which occurs when all workers are connected */
			try {
				startSignal.await();
			} catch (InterruptedException e) {
				/* This never happens unless some serious (programming) error occurs */
				e.printStackTrace();
				System.exit(1);
			}
			
			/* Send a control message containing the absolute start time of the benchmark */
			line = "@control starttime " + WikiBench.startTime + "\n";
			message = line.getBytes("UTF8");
			out.write(message);
			out.flush();
			
			/* We now read trace lines and write them into the socket until we reach the end of file */
			while (true) {
				line = traceReader.getLine();
				if (line == null) break;				/* See if we reached EOF */
				line += "\n";
				message = line.getBytes("UTF8");		/* convert to a byte array, encoded in UTF8 */
				out.write(message);						/* The other side can now create a string, using UTF8 encoding too */
				out.flush();							/* Just to be sure, flush the buffer */
				
				WikiBench.incrementTotalRequests();
			}			
			
			/* At this point, EOF is reached, close connection */
			WikiBench.endOfFile = true;
			out.close();
			connection.close();
		} catch (SocketException e) {
			System.err.println("The worker closed the connection, or connection was lost: " + e.getLocalizedMessage());
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
		} finally {
			try {
				out.close();
				connection.close();
			} catch (IOException e) {
				/* At least we tried.. */
			}
		}
	}
}
