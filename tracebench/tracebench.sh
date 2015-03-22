d=$0
d1=${d#./}
build_dir=${d1%/*} 
if [ -d $build_dir ]
	then
		echo "cd $build_dir"
		cd $build_dir
	fi
	
if [ $# -ne 5 ] 	
	then
		echo " Usage:"
		echo " $0 <reduction permil> '<dbUri>' <sampling method> <time stamp>"
	else
		ant run -Dreduction=$1 -DdbUri=$2 -Dsampling=$3 -Dtime=$4 -Dconnector=$5
fi
