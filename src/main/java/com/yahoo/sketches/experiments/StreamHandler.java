package com.yahoo.sketches.experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import com.yahoo.sketches.hash.MurmurHash3;


public class StreamHandler {
    static long[] readLongsFromFile(String streamFileName) {
	try {
	    List<Long> streamList = new ArrayList<Long>(); 
	    FileInputStream fstream;
	    fstream = new FileInputStream(streamFileName);
    	    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            
            while ((strLine = br.readLine()) != null)   {
                long hash = MurmurHash3.hash(strLine.getBytes(), 0)[0];
                streamList.add(hash);
    	    }
            br.close();
            
            long[] stream = new long[streamList.size()];
            for (int i=0; i<streamList.size(); i++){
        	stream[i] = streamList.get(i);
            }
            return stream;
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return null;
    }
}
