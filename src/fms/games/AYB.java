package fms.games;

import java.util.Map.Entry;

import fms.Game;
import fms.GameState;
import fms.SensorData;

public class AYB extends Game{
    
    //9 sensors - indicates trigger of sensor
    //9 squares - indicates posession of corresponding square
    //won - indicates who won
    static final String[] varNames =  {"sensor1", "sensor2", "sensor3", "sensor4", "sensor5", "sensor6", "sensor7", "sensor8", "sensor9",
            "square1", "square2", "square3", "square4", "square5", "square6", "square7", "square8", "square9",
            "won"
    };
    
    @Override
    public String[] varNames() { return varNames; }
    
    @Override
    public GameState initial() { 
        return new GameState(varNames());  //zeros
    }

    @Override
    public GameState transition(GameState state, SensorData sensorData) {
        state = super.transition(state, sensorData); //propagate last sensor state
        for(Entry<String, Double> sensor : sensorData.entrySet()){
            if(sensor.getValue()!=0)
            {
                double move = sensor.getValue(); //1 for X, 2 for O
                String squareName = sensor.getKey().replace("sensor", "square");
                
                //take posession of square
                state.set(squareName, move);
                
                
                //TODO: string ugliness
                int r = Integer.parseInt(squareName.replace("square", "")) - 1;
                int x = r % 3, y = r / 3;
                int col=0,row=0,diag=0,rdiag=0;
                for(int i=0;i<3;i++){
                    if(state.get("square"+(3*i+x+1))==move) col++;
                    if(state.get("square"+(3*y+i+1))==move) row++;
                    if(state.get("square"+(3*i+i+1))==move) diag++;
                    if(state.get("square"+(3*(2-i)+i+1))==move) rdiag++;
                }
                if(row==3 || col==3 || diag==3 || rdiag==3)
                    state.set("won", move);
            }   
        }
        
        return state;
    }
}
