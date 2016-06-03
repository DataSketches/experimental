# Create data if it doesn't exist
if [ ! -d data ]; then 
    mkdir data;
    python make_data.py
fi

# create a "results" folder if it doesn't exist
if [ ! -d results ]; then mkdir results; fi
java -cp ../target/experimental-0.1.0-jar-with-dependencies.jar com.yahoo.sketches.hashmaps.StressTestHashMap > results/StressTestHashMap.csv