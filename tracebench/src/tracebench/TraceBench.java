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

package tracebench;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

/*
 * TraceBench - Part of the WikiBench benchmarking tool.
 * Created by Erik-Jan van Baaren (erikjan@gmail.com)
 * December, 2008
 * 
 * Modified by Felipe Ledesma Botero (felipe.ledesma@gmail.com)
 * 
 * TraceBench is a workload creator. This  utility reads a Wikipedia
 * trace from standard input and outputs a workload file that can be
 * used as an input file for the WikiBench controller.
 * 
 * The parameter is a reduction percentage, an integer between 0 and
 * 100. If it is 0, this tool will be quicker as it will only remove
 * unwanted trace lines without further sampling.
 * 
 * Standard input and  output are used, so you can use Unix pipes to
 * create your own workflow. You may  want to gzip the output stream
 * or directly pipe it to a WikiBench controller.
 * 
 * TraceBench expects a very specific  input format. If  this format
 * changes simply modify the parseTime(), parseUrl() and parseSave()
 * methods to match the new trace format.
 * 
 * If the MediaWiki  software or the  Wikipedia setup  changes,  the
 * required changes can be significant, since filters would have  to
 * be rewritten. It works with MediaWiki 1.13, the version that  was
 * current around 2008/2009.
 * 
 * 
 */

public final class TraceBench {
	static String errorString = null;	/* Will hold the error that occurred, if any */
	static String language = "en";		/* Which language do we work with? Fixed for now */
	static int reduction = 0;			/* The reduction permil, higher # will remove more */
	static String dbUri = "jdbc:mysql://localhost/wikidb?user=root&password=";
	BufferedReader in;
	static Random generator;
	static Database db = null;
	static boolean plSampling = true;
	static boolean date_ts = true; 	/* Default timestamp format for new logs */
	/* The seed for our random number generator. Keep it fixed, if you want to
	 * have reproducible results. */
	final static int SEED = 10;
	
	public static void main(String[] args) { 
		if (args.length == 4) {
			reduction = Integer.parseInt(args[0]);
			dbUri = args[1];
			if (args[2].equals("sampling")) plSampling = false;
			if (args[3].equals("epoch_ts")) date_ts = false;
		} else {
			System.out.println("Usage: java -jar TraceBench <reduction in permil> <db uri> <plsampling|sampling> <date_ts|epoch_ts>");
			System.exit(1);
		}

		TraceBench tb = new TraceBench();
		
		/* We use a fixed value as a seed, so results are reproducible */
		generator = new Random(SEED);
		
		db = new Database(dbUri);
		int result;
		result = tb.parse();
		if (result == 1) {	
			System.err.println("TraceBench encountered an error: " + errorString);
		}
		System.exit(result);
	}
	
	/** 
	 * Create a new TraceBench object
	 * 
	 */
	public TraceBench() {
		in = new BufferedReader(new InputStreamReader(System.in));
	}
	
	/**
	 * Start parsing the trace file
	 * @return 1 if an error occurs, with errorMessage
	 * set to the error message, or 0 if everything went fine.
	 */
	public int parse() {
		String line, time, url, save, path;
		String[] lineParts;
		while (true) {
			try {
				line = in.readLine();
			} catch (IOException e) { 
				errorString = e.getLocalizedMessage(); 
				return 1; 
			}
			if (line == null) break;
			lineParts = line.split(" ");
			/* We need 3 parts, otherwise this line is not properly formatted */
			if (lineParts.length != 3) {
				System.err.println("Ignoring a faulty trace line: " + line);
				continue;
			}
			time = parseTime(lineParts[0]);
			url = parseUrl(lineParts[1]);
			save = lineParts[2];
			if (url == null) {
				System.err.println("Ignoring a faulty url, line was: " + line);
				continue;
			}
			/* See if this is a keeper.. */
			if (!urlFilter(url)) {
				continue;	/* stop here, repeat the while loop */
			}
			int index = url.indexOf("/");
			if (index!=-1) {
				path = url.substring(index+1); // strip off the domain+first slash
			} else {
				path = "";	// some request do not have a path!
			}
			/* Check if this request resulted in a save. If so, we are going
			 * to fetch the data related to this page from the database. */
			if (save.equals("save")) {
				String pageName;
				pageName = path.substring(path.indexOf("title=")+6);
				pageName = pageName.substring(0, pageName.indexOf('&'));
				try {
					pageName = URLDecoder.decode(pageName, "UTF-8");
					WikiPage p = db.getPage(pageName);
					/* If the page was found, we add its data to the trace */
					if (p != null) { 
						save = p.getTimestamp() + "|" + URLEncoder.encode(p.getPageContent(), "UTF-8");
					}
				} catch (UnsupportedEncodingException e) {
					errorString = e.getLocalizedMessage();
					return 1;
				}
			}
			/* Note that the space character in the urls must be encoded, otherwise
			 * this stream is unparsable! */
			System.out.println(time + " " + path + " " + save);
		};
		return 0;
	}
	/**
	 * Parse the time value as it is found in the trace file, this 
	 * only means removing the dot character between the seconds and milliseconds
	 * part if the timestamp is in epoch format. If it is using the newer date format 
	 * it will convert it to epoch format and return.
	 * 
	 * @param time a string with the timestamp
	 * @return the epoch of the timestamp
	 */
	public String parseTime(String time) {
		if (date_ts) {
			try {
				return getEpoch(time);
			} catch (ParseException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		/* Get the time, strip off the space and dot immediately */
		return time.replace(".", "");
	}
	/**
	 * Parse the url from the trace line, meaning strip off the http:// part.
	 * This method needs work since it is really specific and does not even
	 * recognize https urls
	 * @param url A string containing the full url
	 * @return a string with the stripped url, or null of the url was invalid,
	 * e.g. a url < 12 chars is technically impossible
	 */
	private String parseUrl(String url) {
		// strip off the http:// part
		if (url.length()>12) 
			return url.substring(7);
		else	
			return null;
	}
	
	/** 
	 * This method filters urls. It drops a lot from the trace file. Detailed
	 * descriptions are provided in the comments above each filter line.
	 * 
	 * The filters are not in  arbitrary order. They are  ordered roughly  by
	 * the amount of requests they  effect.  When adapting these  filters you
	 * need to realize that filters after the  one you change might depend on
	 * the one you change or remove.
	 * 
	 * @param url a String containing the fill url, but without the protocol
	 * part (so do not include http:// or https://)
	 * @return true if this url should be keps, false if it can be dropped
	 */
	public boolean urlFilter(String url) {
		String path;
		String pageName = null;
		
		/************************************************************************** 
		 * 						DOMAIN BASED FILTER
		 *************************************************************************/ 
		/* the quick way to filter large parts of the traces */
		if (!url.startsWith(language + ".wikipedia.org")) return false;
		
		/************************************************************************** 
		 * 						PATH BASED FILTERS	
		 *************************************************************************/
		// find the first slash and remove anything up to and including that slash
		int index = url.indexOf("/");
		if (index!=-1) {
			path = url.substring(index+1); // strip off the domain+first slash
		}
		else {
			// set path to empty string
			path="";
		}

		/* Replace the html entities that we want to search for inside paths */
		path = path.replaceAll("%2F", "/");
		path = path.replaceAll("%20", " ");
		path = path.replaceAll("&amp;", "&");
		path = path.replaceAll("%3A", ":");

		/************************************************************************** 
		 * 							DIRECT DROPS
		 * 
		 *  Some stuff can be dropped instantly.
		 *  
		 *  - Search queries: In the real Wikipedia, search is handled by a
		 *  separate system. We filter out all search queries
		 *  
		 *  - We also drop requests related to page revisions, since we will not
		 *  include revision data.
		 * 
		 * 	- Anything that is related to users and user management is also dropped
		 *  
		 *  - Talk pages are also dropped, we don't have the data (we might want
		 *  	to reconsider this or make it an option later on)
		 *  
		 *************************************************************************/
		
		if (path.contains("?search=") || path.contains("&search=")
				|| path.startsWith("wiki/Special:Search")) 	return false;
		
		/* Various other filters */
		/* query.php was the old API, it's unsupported now */
		if (path.startsWith("w/query.php")) 				return false;
		/* Drop all Talk pages */
		if (path.startsWith("wiki/Talk:")) 					return false;
		if (path.contains("User+talk"))						return false;
		if (path.contains("User_talk"))						return false;
		/* User login related Special page */
		if (path.startsWith("wiki/Special:AutoLogin")) 		return false;
		if (path.startsWith("Special:UserLogin"))			return false;
		/* Drop all user pages */
		if (path.contains("User:"))							return false;
		/* Drop Talk pages */
		if (path.contains("Talk:"))							return false;
		/* Do not include revision history comparisons */
		if (path.contains("&diff="))						return false;
		// Drop administrative actions
		if (path.contains("&action=rollback"))				return false;
		/* Watchlist is based on the user that must be logged in. We might
		 * consider it a static page, but that would not be fair since it
		 * probably isn't in most of the requests */
		if (path.contains("Special:Watchlist"))			return false;
		
		/* Handle any API call that are not filtered yet */
		if (path.startsWith("w/api.php")) {
			// for now, drop api calls
			return false;
			//if (path.contains("prop=categories"))		return sample();
		}
		
		/************************************************************************** 
		 * 							SAMPLING FILES
		 * 
		 *  We consider some pages and files as static, even though they are not.
		 *  Obvious static files are the skin-1.5/ files, /images/ files and
		 *  the css files. 
		 *  
		 *  The x most popular wiki pages are considered and sampled like static
		 *  files, because we would not want to risk that such files are removed
		 *  by page-level sampling (which would reduce the size considerably!).
		 *  Such pages include the Main_Page, and the css and javascript 'pages'
		 *   
		 *************************************************************************/
		/* The skin files */
		if (path.startsWith("skins-1.5/")) 				return sample();
		/* The images (buttons, bullets, etc.) */
		if (path.startsWith("images/")) 				return sample();
		/* The favicon.ico file */
		if (path.equals("favicon.ico")) 				return sample();
		
		/* Pages that are treated as static files, because they represent
		 * lots of traffic */
		if (path.startsWith("wiki/Main_Page"))			return sample();
		/* If there is no path, we can sample this as if it is a main page request */
		if (path.length()==0) sample();
		
		if (path.equals("w/index.php"))					return sample();
		/* These are part of the mediawiki software (style sheets) */
		if (path.contains("MediaWiki:Common.css")) 		return sample();
		if (path.contains("MediaWiki:Common.js")) 		return sample();
		if (path.contains("MediaWiki:Monobook.css")) 	return sample();
		if (path.contains("MediaWiki:Print.css")) 		return sample();
		if (path.contains("MediaWiki:Handheld.css")) 	return sample();
		
		/* Extensions tend to be css files, png files, etc. */
		if (path.startsWith("w/extensions/")) 			return sample();
		
		/* Special:Random redirects to a random page, so the best we can
		 * do is sample it */
		if (path.startsWith("wiki/Special:Random"))		return sample();
		
		/* user style sheets and javascript files that are requested
		 * as raw files */
		if ((path.contains("gen=css") || path.contains("gen=js") ||	path.contains("ctype=text/javascript"))
				&& path.contains("action=raw")) 		return sample();

		/* The /Special:Export page itself is static */
		if (path.equals("wiki/Special:Export") || path.equals("wiki/Special:Export/"))
														return sample();
		
		/* Sample recent changes page (both RecentChanges and Recentchanges)
		 * but not the RecentChangesLinked, because that one is related to a
		 * specific page and should be page sampled instead */
		if ((path.contains("Special:Recentchanges") || path.contains("Special:RecentChanges"))
				&& !path.contains("Special:RecentChangesLinked"))
														return sample();
		
		/* Sample the AllPages page */
		if (path.contains("Special:AllPages"))			return sample();
		
		/* Sample the IP blocklist page */
		if (path.contains("Special:Ipblocklist"))		return sample();
		
		/* Returns an XML file describing the opensearch API */
		if (path.startsWith("w/opensearch_desc.php")) 	return sample();
		
		/* These are redirects to the main page and are considered
		 * a static file request or main page request, so we sample */
		if (path.equals("wiki/") || path.equals("w/index.php") || path.equals("wiki")
			|| path.equals("w/") || path.length()==0)	return sample();
		
		/* This following filter catches any static file that was not yet handled
		 * above, like images, javascrips, robots.txt and leftover css files */
		if (path.endsWith(".gif") || path.endsWith(".txt") || path.endsWith(".css") ||
			path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
			path.endsWith(".js"))
														return sample();
		
		/************************************************************************** 
		 * 							SAMPLING PAGES
		 * 
		 * The second type of sampling is page level sampling. It depends
		 * completely on the title. This way, we can reduce the amount of
		 * pages that are in the traces by removing selected page names
		 * from the trace completely. There are many ways of requesting a
		 * page and related services concerning a page name. Some very rare
		 * things might be missed by this parser. For the benchmark, this
		 * does not influence the results considering the extremely small
		 * percentages of these rare requests.
		 * 
		 *************************************************************************/
		
		/* First check if the user supplied the plsamping option on the
		 * command line. If not, we simply call sample() and do no page level
		 * sampling. */
		if (!plSampling) {
			return sample();
		}
		
		/* The largest share of requests: regular page views */
		if (path.startsWith("wiki/") && !path.contains("?")) {
			pageName = path.replace("wiki/", "");	/* simply replace the first part */
			return samplePage(pageName);
		} else if (path.startsWith("wiki/") && path.contains("?")) {
			/* This is mostly some weird kind of hacking attempt that results 
			 * in a regular page view, so we include this and use page level sampling! */
			 pageName = path.substring(0, path.indexOf("?"));
			 return samplePage(pageName);
		}
		
		/* Anything with a "title=..". This will match a lot of pages and related requests. */
		if (path.contains("title=")) {
			// First, strip off anything before title= so we start with the title
			path = path.substring(path.indexOf("title=")+6);
			
			if (!path.contains("&")) {
				/* If there is no ampersand, we are done */
				pageName = path;
			} else {
				/* We split up the string by ampersands, and keep the first part of this */
				pageName = path.split("&")[0];
			}
			return samplePage(pageName);
		}
		
		/* The case of "Special:Export" as a static file is excluded already, so
		 * we now regard this as a special type of page request */
		if (path.startsWith("wiki/Special:Export/")) {
			pageName = path.replace("wiki/Special:Export/", ""); 
			return samplePage(pageName);
		}
		
		if (path.startsWith("Special:RecentChangesLinked/")) {
			pageName = path.replace("Special:RecentChangesLinked/", "");
			return samplePage(pageName);
		}

		if (path.startsWith("wiki/Special:WhatLinksHere/"))	{
			pageName = path.replace("wiki/Special:WhatLinksHere/", "");
			return samplePage(pageName);
		}
		
		/* By now, we have actively filtered the most important trace lines. We
		 * are left with requests like: virus/worm requests, typos and very
		 * rare types of requests. We don't drop these requests, but sample them
		 * to keep the realisticness of the trace high. In a quick test, there
		 * were about 40 in 1,000,000 lines that reached this point, that is
		 * 0,00004% */
		//System.out.println("Unprocessed url: " + url);
		return sample();
	}

	/**
	 * Perform regular sampling by using a  random number generator
	 * 
	 * @returns true if it this trace line should be kept, false otherwise
	 */
	private boolean sample() {
		// First, the easy case to save lots of random number generations
		if (reduction == 0) return true;
		/* We pick a number between 0 (inclusive) and 1000 (exclusive) */
		if (generator.nextInt(1000) >= reduction)  {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Perform page level sampling, based on the supplied page name
	 * @param pageName a string containing the page name
	 * @return true if this page needs to be included, false if this page
	 * can be dropped from the trace file
	 * 
	 */
	private boolean samplePage(String pageName) {
		/* We calculate a cheap hash code from the pageName. This code % 1000
		 * gives a remainder value p with 0 < p < 999. We can now use a simple
		 * comparison to decide if we include the pageName or not. Everything
		 * smaller than the reduction per mil is kept.
		 */
		
		/* Again, the easy case first */
		if (reduction == 0) return true;
		
		if (pageName != null && (pageName.hashCode() % 1000) >= reduction ) {
			//System.out.println("Page name included: " + pageName);
			return true;
		} else if (pageName != null){
			//System.out.println("Page name not included: " + pageName);
			return false;
		} else {
			// Empty page name, this is a bug/wrong usage
			System.err.println("Error notice: samplePage() got an empty pageName, returning false");
			return false;
		}
	}
	
	/**
	 * Get epoch timestamp from the new timestamp format of the Wikipedia logs.
	 * 
	 * @param timestamp a string containing the timestamp with the new format
	 * @return a String with the epoch for the timestamp
	 * 
	 */
    public static String getEpoch(String timestamp) throws ParseException
    {
        String firstPart = timestamp.substring(0,10);
        String secondPart = timestamp.substring(11);        
        String newDate = firstPart + " " + secondPart;
            
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        Date parsed = format.parse(newDate);
        
        Calendar cal = new GregorianCalendar();
        cal.setTime(parsed);
        return "" + cal.getTimeInMillis();
    }
}
