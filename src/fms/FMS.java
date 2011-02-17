package fms;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import fms.games.AYB;

/**
 * REST-ful API for field management
 * @author mike
 *
 */
public class FMS implements HttpRequestHandler {
    
    Connection conn;
    Map<Integer, GameState> games;
    
    public FMS(){
        //init db connection
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:fms.db");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS MATCHES (id integer primary key autoincrement, game int not null default 1, start datetime, end datetime);");
        }catch(Exception e){
            e.printStackTrace();
            System.err.println("Error connecting to db.");
        }
        games = new HashMap<Integer, GameState>();
    }

    @Override
    public void handle(HttpRequest req, HttpResponse resp, HttpContext context)
            throws HttpException, IOException {
        /**
         * Parse method, args
         * Format of request URI should be: /fms/{action}?arg1=val1&arg2=val2...
         */
        final String requestURI = req.getRequestLine().getUri();
        String[] rr = requestURI.split("/");
        String rest = rr[2], method = rest;
        int idx = rest.indexOf('?');
        Map<String,String> args = new HashMap<String,String>();
        String argStr = "";
        if(idx!=-1 && idx < rest.length()){
            argStr = rest.substring(idx+1);
            method = rest.substring(0, idx);
        }
        else if(req.getRequestLine().getMethod().equalsIgnoreCase("POST"))
            argStr = getBody(req);
        for(String pair : argStr.split("&")){
            String[] ss = pair.split("=");
            if(ss.length==2) args.put(ss[0], ss[1]);
        }
        
        method = method.toLowerCase();
        System.err.println("Method:" + method);
        
        try {
        if("create".equals(method)){
            PreparedStatement p = conn.prepareStatement("INSERT INTO MATCHES(game, start) values(?,?);");
            p.setString(1, args.get("game"));
            p.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            p.executeUpdate();
            
            //get autoincrement key
            ResultSet rs = p.getGeneratedKeys();
            if(rs.next()){
                int gameId = rs.getInt(1);
                //assume game is AYB
                //TODO: use game to spawn state
                games.put(gameId, new AYB().initial()); 
            }else
                System.err.println("fail!");
        }else if("list".equals(method)){
            int limit = 10;
            if(args.containsKey("limit"))
                limit = Integer.parseInt(args.get("limit"));
            PreparedStatement p = conn.prepareStatement("SELECT id,game,start,end from MATCHES ORDER BY id DESC LIMIT "+limit+";");
            ResultSet rs = p.executeQuery();
            JSONObject json = new JSONObject();
            JSONArray matches = new JSONArray();
            json.put("matches", matches);
            while(rs.next()){
                JSONObject o = new JSONObject();
                o.put("id", rs.getString("id"));
                o.put("game", rs.getString("game"));
                o.put("start", rs.getLong("start")); //use millis
                o.put("end", rs.getLong("end"));
                matches.add(o);
            }
            resp.setEntity(new StringEntity(args.get("callback")+"("+json.toJSONString()+")"));
        }else if("state".equals(method)){
            int gameId = Integer.parseInt(args.remove("id"));
            GameState state = games.get(gameId); 
            if(state!=null){
                resp.setEntity(new StringEntity(args.get("callback")+"("+state.toJSON().toJSONString()+")"));
            }
        }else if("update".equals(method)){
            int gameId = Integer.parseInt(args.remove("id"));
            GameState state = games.get(gameId); 
            
            //assume game is AYB
            //TODO: use game to spawn state
            GameState newState = new AYB().transition(state, new SensorData(args));
            games.put(gameId, newState);
            //log result
            new Log(gameId).update(args);
        }else if("log".equals(method)){
            int gameId = Integer.parseInt(args.remove("id"));
            //read log
            JSONArray json = new Log(gameId).read();
            resp.setEntity(new StringEntity(args.get("callback")+"("+json.toJSONString()+")"));
        }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    private final static String getBody(HttpRequest req) throws IOException {
        if(req instanceof HttpEntityEnclosingRequest){
            InputStream in = ((HttpEntityEnclosingRequest)req).getEntity().getContent();
            StringBuffer out = new StringBuffer();
            byte[] b = new byte[4096];
            for (int n; (n = in.read(b)) != -1;) {
                out.append(new String(b, 0, n));
            }
            return out.toString();
        }
        return null;
    }
    
    public static void main(String[] args) {
        new FMS();
    }
}
