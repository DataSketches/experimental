# Create data if it doesn't exist
if [ ! -d data ]; then 
    mkdir data;
    python make_data.py 1000
fi

# create a "results" folder if it doesn't exist
if [ ! -d results ]; then mkdir results; fi
#echo 'creating results/StressTestHashMap.csv'
#java -cp ../target/experimental-0.1.0-jar-with-dependencies.jar com.yahoo.sketches.experiments.StressTestHashMap
java -cp ../target/experimental-0.1.0-jar-with-dependencies.jar com.yahoo.sketches.experiments.StressTestFrequentItems
