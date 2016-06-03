if [ ! -d results ]; then mkdir results; fi

java -cp ../target/experimental-0.1.0-jar-with-dependencies.jar com.yahoo.sketches.hashmaps.StressTestHashMap > results/StressTestHashMap.csv