package fms;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    
    public String toString(){
        StringBuffer sb = new StringBuffer();
        try{
        for(Map.Entry<String, Double> ent : this.entrySet()){
            if(sb.length() > 0) sb.append("&");
            sb.append(URLEncoder.encode(ent.getKey(), "utf-8")+"="+URLEncoder.encode(ent.getValue().toString(), "utf-8"));
        }}catch(UnsupportedEncodingException uee){uee.printStackTrace(); /*ignore*/}
        return sb.toString();
    }
}
