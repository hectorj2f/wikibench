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

package wikibench;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import wikibench.io.LogWriter;
import wikibench.io.TraceReader;
import wikibench.threads.FetchThread;
import wikibench.threads.WorkerConnection;

/**
 * This is the main class of WikiBench
 * 
 * @author E. van Baaren (erikjan@gmail.com)
 * @author Felipe Ledesma Botero (felipe.ledesma@gmail.com) 
 * 
 */
public final class WikiBench {
	/** Define whether this is a worker or the controller */
	static boolean isWorker = false;
	/** If worker: the host name of the controller */
	static String controllerHost = null;
	/** If worker: the number of threads to start */
	static int numFetchThreads = 100;
	/** If controller: number of workers needed before start signal */
	static int numWorkers = 1;
	/** If true, show verbose information */
	public static boolean verbose = false;
	/** If worker: The host name of the System Under Test */
	public static String sutHost = "localhost";
	/** If worker: The port number of the System Under Test */
	public static int sutPort = 80;				
	/** If worker: wait a maximum of sutTimeout milliseconds for reply */
	public static int sutTimeout = 20000;
	/** If worker: Name of the log file a worker writes to */
	public static String logFileName = "wikibench.log";
	/** If worker: Do not perform HTTP POSTS */
	public static boolean noEdits = false;			
	
	/* Hard coded settings */ 
	/** The port workers connect to and the controller listens on */
	static int controllerPort = 48657;
	/** User agent used by the workers */
	public static String userAgent = "WikiBench";

	/* Globally used variables and flags*/
	/** If true, the traceReader has reached the end of the trace file */
	public static boolean endOfFile = false;	
	/** Time at which the benchmark starts */
	public static long startTime;			
	/** Lowest time, extracted from first trace entry */
	public static long lowestTime;			
	/** Set to true by a WorkerConnection if there is a missed deadline */
	public static boolean missedDeadline = false;
	/** Counter that is increased to calculate the total amount of requests issued */
	private static long totalRequests = 0;
	/** Counter that is increased to calculate the total amount of missed deadlines */
	private static long missedDeadlines = 0; 


	/**
	 * After parsing the arguments, this function either starts WikiBench as
	 * a worker node or as a controller node
	 * 
	 * @param args	String array of arguments from command line
	 */
	public static void main(String[] args) {
		/* Parse the command line arguments, stop if there were errors */
		if (!parseArguments(args)) {
			printUsage();
			return;
		}
		
		/* Start either a worker or a controller */
		if (isWorker) {
			if (!doWorker())		System.exit(1);
		}
		else {
			if (!doController())	System.exit(1);
		}
	}

	/**
	 * Start functioning as a controller. Create a new WorkerConnection thread
	 * for each worker that connects and waits for all threads to finish.
	 * 
	 * @return	false if some error occurred, true otherwise
	 */
	public static boolean doController() {
		ServerSocket server;
		TraceReader traceReader;
		CountDownLatch startSignal = new CountDownLatch(1);
		
		/* Create a TraceReader that reads from standard input */
		try {
			traceReader = new TraceReader(new InputStreamReader(System.in, "UTF8"), null);
		} catch (UnsupportedEncodingException e1) {
			System.err.println("This system does not support UTF-8 enconding.");
			return false;
		}
		Vector<WorkerConnection> workerConnections = new Vector<WorkerConnection>();
		
		try {
			server = new ServerSocket(controllerPort);
			server.setReuseAddress(true);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		while (true) {
			try {
				Socket workerSocket = server.accept();
				WorkerConnection workerConnection = 
					new WorkerConnection(workerSocket, traceReader, startSignal);
				workerConnection.start();
				workerConnections.add(workerConnection);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			/* As soon as the expected number of workers have joined, we continue */
			if (workerConnections.size() == numWorkers) break;
		}
		/* Set the start time */
		startTime = System.currentTimeMillis()+5000;
		/* Signal all WorkerConnections that they can start sending trace lines */
		startSignal.countDown();
		
		/* Wait for all worker connection threads to finish */
		for (int i=0;i<workerConnections.size();i++) {
			try {
				workerConnections.get(i).join();
			} catch (InterruptedException e) {e.printStackTrace();}
		}
		/* Properly close the trace reader */
		traceReader.close();
		
		System.out.println("Total requests: " + getTotalRequests());
		
		return true;
	}
	
	/**
	 * Start functioning as a worker node and try to connect to the controller.
	 * If no controller host name was given on the command line, 127.0.0.1 is the default.
	 * 
	 * @return	true if the worker successfully started and connected, false otherwise.
	 */
	public static boolean doWorker() {
		Socket controllerSocket;
		TraceReader traceReader;
		Vector<FetchThread> fetchers = new Vector<FetchThread>();
		
		/* Connect to server */
		try {
			if (controllerHost == null) controllerHost = "127.0.0.1";
			controllerSocket = new Socket(controllerHost, controllerPort);
			traceReader = new TraceReader
			(new InputStreamReader(controllerSocket.getInputStream(), "UTF8"),
			new OutputStreamWriter(controllerSocket.getOutputStream(), "UTF8"));
		} catch (UnsupportedEncodingException e) {
			System.err.println("This system does not support UTF-8 enconding.");
			return false;
		}
		catch (UnknownHostException e) {
			System.err.println(e.getLocalizedMessage());
			return false;
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			return false;
		}
		
		/* Create a log writer */
		LogWriter logWriter = new LogWriter(WikiBench.logFileName);
		
		/* Start the fetch threads */
		for (int i=0; i<numFetchThreads; i++) {
			FetchThread f = new FetchThread(traceReader, logWriter);
			fetchers.add(f);
			f.start();
		}
		try {
			/* Wait for all threads to finish */
			for (int i=0; i<numFetchThreads; i++) {
				try {
					fetchers.get(i).join();
				} catch (InterruptedException e) {e.printStackTrace();}
			}
		} catch (Exception e) {
			/* Whatever happened, close the writer so we can read the file */
			logWriter.close();
			System.err.println("An exception occured:" + e.getLocalizedMessage());
			System.err.println("The log file " + WikiBench.logFileName 
					+".gz was properly closed for your inspection.");
		}
		/* Clean up, close files and sockets */
		logWriter.close();
		traceReader.close();
		try {
			controllerSocket.close();
		} catch (IOException e) {System.err.println(e.getLocalizedMessage());}
		
		System.out.println("Total missed deadlines: " + getMissedDeadlines());
		return true;
	}
	
	/**
	 * <p>Parse command line options. Supported command line options are:</p>
	 * <p><b>Generic options</b><BR />
	 * -verbose : Enable verbose output
	 * </p>
	 *  <p><B>Worker specific options</B><BR />
	 *  -worker : indicates that WikiBench needs to start in worker mode. By default, it starts as a controller<BR />
	 *  -controller &lt;hostname&gt; : hostname of the controller to which this worker can connect<BR />
	 *  -threads &lt;num&gt; : The number of worker threads that a worker must start<BR />
	 *  -suthost &lt;url&gt; : Hostname of the System Under Test<BR />
	 *  -sutport &lt;url&gt; : Port number of the System Under Test web server<BR />
	 *  -timeout &lt;url&gt; : Maximum waiting time for a HTTP request<BR />
	 *  -logfile &lt;url&gt; : log file location<BR />
	 *  -threads &lt;num&gt; : the number of HTTP fetcher threads
	 *  </p>
	 *  <p><b>Controller specific options</b><BR />
	 *  -controller : indicates that WikiBench needs to start in controller mode<BR />
	 *  -numworkers : the number of worker that need to connect before the controller gives the start signal<BR />
	 *  </p>
	 *  @param	args	String array of arguments (usually obtained from the main method)
	 *  @return			A boolean set to false when an error occurred and true otherwise
	 */
	public static boolean parseArguments(String[] args) {
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-worker")) {
				isWorker = true;
				System.out.println("I'll act as a worker.");
			}
			else if (args[i].equals("-controller")) {
				if (++i >= args.length || args[i].charAt(0) == '-') {
					System.out.println("Option -controller was supplied without a valid host name");
					return false;
				}
				controllerHost = args[i];
				System.out.println("Setting controller host name to " + controllerHost);
			}
			else if (args[i].equals("-threads")) {
				if (++i >= args.length || args[i].charAt(0) == '-') {
					System.out.println("Option -threads was supplied without a valid number");
					return false;
				}
				numFetchThreads = Integer.parseInt(args[i]);
				System.out.println("Setting number of threads to " + args[i]);
			}
			else if (args[i].equals("-numworkers")) {
				if (++i >= args.length || args[i].charAt(0) == '-') {
					System.out.println("Option -numWorkers was supplied without a number");
					return false;
				}
				numWorkers = Integer.parseInt(args[i]);
				System.out.println("Setting number of workers to " + args[i]);
			}
			else if (args[i].equals("-suthost")) {
				if (++i >= args.length || args[i].charAt(0) == '-') {
					System.out.println("Option -suthost was supplied without a url");
					return false;
				}
				sutHost = args[i];
				System.out.println("Setting host name of SUT to " + args[i]);
			}
			else if (args[i].equals("-sutport")) {
				if (++i >= args.length || args[i].charAt(0) == '-') {
					System.out.println("Option -sutport was supplied without a port name");
					return false;
				}
				sutPort = Integer.parseInt(args[i]);
				System.out.println("Setting port number of SUT to " + args[i]);
			}
			else if (args[i].equals("-timeout")) {
				if (++i >= args.length || args[i].charAt(0) == '-') {
					System.out.println("Option -timeout was supplied without a valid number");
					return false;
				}
				sutTimeout = Integer.parseInt(args[i]);
				System.out.println("A maximum response time of " + args[i] + "ms has been set");
			}
			else if (args[i].equals("-logfile")) {
				if (++i >= args.length || args[i].charAt(0) == '-') {
					System.out.println("Option -logfile was supplied without a file name");
					return false;
				}
				logFileName = args[i];
				System.out.println("Setting log file name to " + args[i]);
			}
			else if (args[i].equals("-verbose")) {
				verbose = true;
				System.out.println("Verbose output is enabled.");
			}
			else {
				System.out.println("Unsupported argument:" + args[i]);
				return false;
			}
		}
		return true;
	}
	
	public static synchronized long getTotalRequests() {
		return totalRequests;
	}
	
	public static synchronized void incrementTotalRequests() {
		totalRequests++;
	}
	
	public static synchronized long getMissedDeadlines() {
		return missedDeadlines;
	}
	
	public static synchronized void incrementMissedDeadlines() {
		missedDeadlines++;
	}
	
	/**
	 * Print usage information
	 */
	private static void printUsage() {
		System.out.println("Usage instructions:\n\n");
		System.out.println("Generic options\n" +
				"-verbose               : Enable verbose output\n" +
				"\nWorker specific options\n" +
				"-worker                : indicates that WikiBench needs to start in worker mode. By default, it starts as a controller\n" +
				"-controller <hostname> : hostname of the controller to which this worker can connect\n" +
				"-threads <num>         : The number of worker threads that a worker must start\n" +
				"-suthost <url>         : Hostname of the System Under Test\n" +
				"-sutport <num>         : Port number of the System Under Test web server\n" +
				"-timeout <num>         : Maximum waiting time for a HTTP request\n" +
				"-logfile <path>        : log file location\n" +
				"-threads <num>         : the number of HTTP fetcher threads\n" +
				"\nController specific options\n" +
				"-controller            : indicates that WikiBench needs to start in controller mode\n" +
				"-numworkers <num>      : the number of worker that need to connect before the controller gives the start signal\n");
	}
}
