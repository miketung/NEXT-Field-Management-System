package fms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.json.simple.JSONArray;

/**
 * The log format is:
 * 
 *  timestamp: state1=val1&state2=val2..
 * 
 * @author mike
 *
 */
public class Log {
    int gameId;
    
    public Log(int gameId){ this.gameId = gameId; }
    
    public void update(Map<String, String> updatedStates){
        File log = new File("logs/"+gameId+".txt");
        try{
            FileWriter fw = new FileWriter(log, true);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
    
    public JSONArray read(){
        JSONArray out = new JSONArray();
        File log = new File("logs/"+gameId+".txt");
        if(!log.exists()) return out;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(log));
            String line = br.readLine();
            
            
        }catch(IOException ioe){
            ioe.printStackTrace();
            return out;
        }
        return out;
    }
}
