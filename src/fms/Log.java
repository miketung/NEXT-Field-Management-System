package fms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
    
    public void update(SensorData updatedStates){
        File log = new File("logs/"+gameId+".txt");
        try{
            FileWriter fw = new FileWriter(log, true);
            fw.write(System.currentTimeMillis()+":"+updatedStates.toString()+"\n");
            fw.flush();
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
            String line;
            while((line = br.readLine())!=null){
                int a = line.indexOf(':');
                if(a!=-1){
                    long timestamp = Long.parseLong(line.substring(0, a).trim());
                    JSONObject b = new JSONObject();
                    b.put("time", timestamp);
                    for(String c : line.substring(a+1).trim().split("&")){
                        String[] d = c.split("=");
                        if(d.length==2){
                            b.put(URLDecoder.decode(d[0], "utf-8"), URLDecoder.decode(d[1], "utf-8"));
                        }
                    }
                    out.add(b);
                }
            }
            br.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
            return out;
        }
        return out;
    }
}
