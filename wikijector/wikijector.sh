print_usage()
{
	echo " Usage:"
	echo " $0 <vcontroller|controller> <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname> <number of workers>"
	echo " $0 <worker> <path to httpcomponents-core/lib> <path to httpcomponents-client/lib> <controller hostname or IP address> <number_of_threads> <string_with_SUT_hostname(s)> <string_with_port_of_SUT> <timeout_in_ms> <path to logfile>"
}

if [ $# -le 1 ]
 then 
 print_usage
 else
	d=$0
	d1=${d#./}
	build_dir=${d1%/*} 
	echo $2
	echo $3
	if [ -d $build_dir ]
	then
		echo "cd $build_dir"
		cd $build_dir		
	fi
	
	jar_dir="build/lib"
	if [ -d $jar_dir ]
	then
		echo "$jar_dir"
	else
		ant build -Dhttp.core=$2 -Dhttp.client=$3
	fi
	
	case "$1" in
	'vcontroller')
	if [ $# -ne 5 ]
	then 
		print_usage
	else
		ant run_verbose_controller -Dhttp.core=$2 -Dhttp.client=$3 -Dcontroller=$4 -Dnumworkers=$5
	fi
	;;
	'controller')
	if [ $# -ne 5 ]
	then 
		print_usage
	else
		ant run_controller -Dhttp.core=$2 -Dhttp.client=$3 -Dcontroller=$4 -Dnumworkers=$5
	fi
	;;
	'worker')
	if [ $# -ne 9 ]
	then
		print_usage
	else
		ant run_worker -Dhttp.core=$2 -Dhttp.client=$3 -Dcontroller=$4 -Dthreads=$5 -Dsuthost=$6 -Dsutport=$7 -Dtimeout=$8 -Dlogfile=$9
	fi
	;;
	*)
	print_usage
	;;
	esac 
 fi
