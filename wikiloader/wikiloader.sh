print_usage()
{
	echo " Parameters:"
	echo "-d"
	echo "		::: for deleting information from the tables: page, revision, text"
	echo "-i"
	echo "		::: for turning indexing off before inserting data into the tables, and turning it on when finishing the whole process of dumping the data"
	echo "-l filepath page_interval"
	echo "		::: for turning on logging, the filepath of the log file, and the page_interval showing between how many page counts the page_id is written into the log file"
	echo "-r page_id"
	echo "		::: restart page insertion after fail from the page_id"
}

if [ $# -eq 1 ]
 then
	if [ "$1" = '-h' ]
	then
		print_usage
	fi
else
	echo "Please enter the database name:"
	read dbname
	echo "Please enter the database username:"
	read username
	echo "Please enter the complete path to your dump file:"
	read path
	
	if [ -d "${build}" ]
	then
		rm build/*		
	else
		mkdir -p "build"
	fi
	cd src
	make clean
	make
	cd .. 
	cp src/wikiloader build/wikiloader	
	
	START=$(date +%s)
	# do something
	echo "7zr e -so $path | ./build/wikiloader $@ | mysql -f -u $username -D $dbname -h 130.37.193.171 -p"
	# start your script work here
	7zr e -so $path | ./build/wikiloader $@ | mysql -f -u $username -D $dbname -h 130.37.193.171 -p
	ls -R /etc > /tmp/x
	rm -f /tmp/x
	# your logic ends here

	END=$(date +%s)
	DIFF=$(( $END - $START ))
	echo "It took $DIFF seconds"
fi

 
