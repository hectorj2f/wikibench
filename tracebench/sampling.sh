myDir=/home/andreea/wikibench-0.3.1/scripts/sorted
permil=(650 600 550 500 450 400 350 300 250 200 150 100 50 0)
for i in "${permil[@]}"
do
	for file in $myDir/*.gz
	do
		echo "$file goes to plsampling.${i}.gz"
		gunzip -c $file | java -jar build/lib/TraceBench.jar $i 'jdbc:mysql://130.37.193.171/wikidb?user=root&password=MediaW!k!' plsampling date_ts | gzip -c >> samples/plsampling.$i.gz				
	done
done
