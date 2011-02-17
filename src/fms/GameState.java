package fms;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONObject;

/**
 * A GameState is a fixed length tuple of real numbers
 * @author mike
 *
 */
public class GameState {
    double[] data;
    private Map<String,Integer> index; 
    public GameState(String[] varnames){
        index = new HashMap<String, Integer>();
        for(int i=0;i<varnames.length;i++)
            index.put(varnames[i], i);
        data = new double[varnames.length];
    }
    private GameState(){}
    
    public double get(String name){
        return data[index.get(name)];
    }
    
    public void set(String name, double val){
        data[index.get(name)] = val;
    }
    
    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        for(Entry<String,Integer>ent : index.entrySet()){
            json.put(ent.getKey(), data[ent.getValue()]);
        }
        return json;
    }
    
    public GameState clone(){
        GameState g = new GameState();
        g.data = this.data;
        g.index = this.index;
        return g;
    }
}
