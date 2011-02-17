package fms;

import java.util.HashMap;
import java.util.Map;

public class SensorData extends HashMap<String,Double>{

    public SensorData(Map<String,String> map){
        for(Map.Entry<String,String> ent : map.entrySet()){
            try{
                Double d = Double.parseDouble(ent.getValue());
                this.put(ent.getKey(), d);
            }catch(Exception e){e.printStackTrace();}
        }
    }
}
