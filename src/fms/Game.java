package fms;

import java.util.Map.Entry;

public abstract class Game {

    public abstract String[] varNames();
    public abstract GameState initial();
    public GameState transition(GameState state, SensorData sensorData){
        for(Entry<String, Double> ent : sensorData.entrySet()){
            state.set(ent.getKey(), ent.getValue());
        }
        return state;
    }
    
    public int numStates(){
        return varNames().length;
    }
}
