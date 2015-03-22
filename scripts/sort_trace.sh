result_file=~/traces.txt.gz

if [ $# -lt 1 ]
then
	echo " Usage: "
	echo " $0   <path_to_traces_folder> <path_to_result_file> <date>"
	echo " $0   <path_to_traces_folder> <date>"
	echo " $0   <path_to_traces_folder>"
    exit 1
fi

if [ -d $1 ]
then
	if [ $# -gt 2 ]
	then			
		result_file=$2			
	fi
	for file in $1/*.gz;
		do
			if [ $# -gt 2 ] 
			then
			gunzip -v -c $file | cut -f 2,3,4 -d ' ' | grep ^$3 |  sort -k1,1M -k2n | gzip -c >> $result_file
			else
				if  [ $# -eq 2 ]
				then
				gunzip -v -c $file | cut -f 2,3,4 -d ' ' | grep ^$2 | sort -k1,1M -k2n | gzip -c >> $result_file			
				else
				gunzip -v -c $file | cut -f 2,3,4 -d ' ' | sort -k1,1M -k2n | gzip -c >> $result_file
				fi				
			fi			
		done
		#bzip2 $result_file	
else
	echo "The directory name you entered is invalid"
	exit 1
fi
