					Wikibench 
                    =========

Wikibench contains three software modules

 1. wikiloader: parses archived Wikipedia XML dumps and inserts data
    into the database
 2. tracebench: pre-processes a Wikipedia trace so that it can be used
    by the load injector
 3. wikijector: the load injector

Wikiloader can be used independently from tracebench and
wikijector. However tracebench and wikijector are to be used according
to a workflow described below.

Up-to-date software, documentation, datasets and publications about
WikiBench can always be found at http://www.wikibench.eu/

/*******************************************************************/ 

wikiloader 

Prerequisites

1. Expat library for XML parsing. It can be downloaded from:
   http://sourceforge.net/projects/expat/ Some Linux distributions
   have this package in their software repository.

Characteristics

The characteristics of the program (also refered to as the dumper, in
this file) are optional and can be activated using the parameters
described in the following sections.

1. Deletion of records from the tables in which data will be inserted,
   with the purpose of avoiding primary key conflicts.

2. Temporarily disabling indexing for the tables in which data will be
   inserted, in order to speed up the process. Indexing will be
   enabled after the insertion process is over.

3. Logging the number of pages processed into a file in order to
   overcome failures. The user can choose an interval between which
   the number of pages processed will be written in the log file.

4. Restarting a failed insertion process. If the logging was enabled
   and the user has the last number of processed pages, it can be used
   to restart the process from the failure point.

Run and compile

The package contains a script that compiles and starts running the
dumper: "wikiloader.sh". The script will show how much time the dumper
worked, measured in seconds.

A typic usage of the dumper is the following:
./wikiloader.sh [parameters]

Execution parameters
The dumper supports the following parameters:

./wikiloader.sh -h                      ; displays a list of the possible 
                                        ; parameters of the script

./wikiloader.sh -d                      ; for deleting information from the 
                                        ; tables: page, revision, text

./wikiloader.sh -i                      ; for turning indexing off before 
                                        ; inserting data into the tables, 
                                        ; and turning it on when finishing 
                                        ; the whole process of dumping the data

./wikiloader.sh -l filepath page_interval  ; for turning on logging, the filepath 
                                           ; of the log file, and the page_interval 
                                           ; showing between how many page counts 
                                           ; the page_id is written into the log file

./wikiloader.sh -r page_id              ; restart page insertion after fail from 
                                        ; the page_id

./wikiloader.sh                         ; default

The dumper can be run using any combination of the above parameters
with the condition that if an option requires more than one parameters
(such as "-l"), the parameters should be given in the order shown
above.

For example: "./wikiloader.sh -d -l filepath page_interval" will work
correctly and delete the data in the tables, while during the insert
statements the page count will be logged into the file.

However, "./wikiloader.sh -d -l page_interval filepath" will result
into an error.  The default run doesn't delete the data in the tables,
doesn't remove indexes from tables, doesn't log page ids to file and
cannot be used to restart the dumping from a certain page id.

Upon the execution a console dialog will be launched and you will be
asked to provide the following additional parameters: database name,
username and path to the dump file. After the sources are compiled and
the script begins the execution, MySQL will prompt you for the
password associated with the user.


/*******************************************************************/ 
	
Workflow for tracebench and wikijector
Prerequisites

1. Sun Java - 1.5, 1.6

2. Java - MySQL connector that can be found in package
   libmysql-java. It is advised that the path to mysql.jar should be
   added to the CLASSPATH.

3. HttpComponents-Client and HttpComponents-Core (Can be downloaded
   from http://hc.apache.org/downloads.cgi)

4. Ant


Step 1: Preparing the traces

Using the script sort_trace.sh(which can be found in the "scripts"
folder) you can prepair your traces in order to be used with
WikiBench. Traces are sorted chronologically and the line numbers at
the beginning of each line are deleted using the sort_trace.sh
script. The script can sort data from multiple .gz archives and also
select traces from a certain day, required by the user. 

Script usage:

./sort_trace.sh <path_to_traces_folder> <path_to_result_file> <date>"
./sort_trace.sh <path_to_traces_folder> <date>
./sort_trace.sh <path_to_traces_folder>

The user can specify a result file, where the sorted traces will be
saved. The name of the file has to have a ".txt" extension, even
though the result of the script will be a .bz2 archive. The script
processes all the .gz archives in the traces folder. If a specific
date is given, in the result file will appear only the traces from
that date.

Step 2: tracebench

After the traces are sorted they can be piped into tracebench.  In
order to run tracebench it is necessary to first build the TraceBench.jar.

ant build -Dconnector=<path to java-mysql connector>

java -jar build/lib/TraceBench.jar <reduction in permil> '<db uri>' <plsampling|sampling> <date_ts|epoch_ts> 

Parameters:

<reduction in permil> - reduction percentage, an integer between 0 and
100. If it is 0, this tool will be quicker as it will only remove
unwanted trace lines without further sampling.

<db uri> - standard MySQL URI for the MediaWiki database. For more
information please check:
http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-configuration-properties.html.
ex: jdbc:mysql://localhost/wikidb?user=root&password=pass

<plsampling|sampling> - sampling method
plsampling - page level sampling. It depends completely on the
title. This way, we can reduce the amount of pages that are in the
traces by removing selected page names from the trace
completely. There are many ways of requesting a page and related
services concerning a page name. Some very rare things might be missed
by this parser. For the benchmark, this does not influence the results
considering the extremely small percentages of these rare requests.
sampling - We consider some pages and files as static, even though
they are not. Obvious static files are the skin-1.5/ files, /images/
files and the css files. The x most popular wiki pages are considered
and sampled like static files, because we would not want to risk that
such files are removed by page-level sampling (which would reduce the
size considerably!). Such pages include the Main_Page, and the css and
javascript 'pages' 

<date_ts|epoch_ts> - time stamp of the traces. In later traces, date_ts is used.

tracebench uses standard input and output. Its output can be further
archived or piped directly into WikiBench.

Step 3: wikijector

wikijector can run in two modes: controller and worker. The controller
must be started first and the number of workers specified as an input
parameter. After that the declared numbered of workers can be started
one by one.

Basic usage:

Run controller:
./wikijector.sh controller <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname> <number of workers>

Run controller in verbose mode:
./wikijector.sh vcontroller <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname> <number of workers>

Run worker:
/wikijector.sh worker <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname or IP address> <number_of_threads> <string_with_SUT_hostname(s)> <string_with_port_of_SUT> <timeout_in_ms> <path to logfile>

When running the wikijector, an URL of the wikipage is used to launch a 
HTTP request. This URL is based on the traces sorted and sampled in the 
previous steps. The URL is composed using a relative path that in the 
sorted traces has the following form /wiki/Main_Page. "wiki" is the name 
of the Wikipedia installation found in the foloder "/var/www/". The 
wikijector software changes this URL to "/mediawiki/index.php/Main_Page" 
in order to be used by the software. If you are using your own mediawiki 
installation be sure to modify the source file 
wikibench/threads/FetchThread.java and change "/mediawiki" with the name 
of your installation of mediawiki. This is only valuable for content 
pages. For css and script files you also need to replace "/w/" with the 
name of your mediawiki installation. 

Full tracebench - wikijector workflow:
a. Run controller:
If you are running this software from the WikibenchServer virtual 
machine, there is no need for building or running tracebench as the 
machine contains a large variety of sampled traces from the interval 23 
– 31 January 2010. This sampled traces are in the folder 
/home/wikiuser/samples. 

gunzip -c plasampling.xxx.gz | ./wikijector.sh controller <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname> <number of workers>
 
If you are running this software from your own machine we reccomend to 
first build tracebench: 

ant build -Dconnector=<path to java-mysql connector>
And after that pipe its output into the wikijector controller.
bunzip2 -c <path_to_trace_file> | java -jar build/lib/TraceBench.jar <reduction in permil> '<db uri>' <plsampling|sampling> <date_ts|epoch_ts> | ./wikijector.sh controller <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname> <number of workers>

b. Run workers:
/wikijector.sh worker <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname or IP address> <number_of_threads> <string_with_SUT_hostname(s)> <string_with_port_of_SUT> <timeout_in_ms> <path to logfile>

