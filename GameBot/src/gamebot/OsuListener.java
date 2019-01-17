/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gamebot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Keeeeys
 */
public class OsuListener extends ListenerAdapter{
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        try {
            //Get the readable contents of the message
            String message = event.getMessage().getContentDisplay();
            //Dont respond to other bots, this bot, and only handle basic commands
            if (event.getAuthor().isBot() || !message.startsWith(GameBot.config.getProperty("prefix") + "osu")) return;
            //Split the args on a space to get them all
            ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
            //Get the command without the prefix
            String command = args.get(0).substring(1);
            //Remove the command from the argument list
            args.remove(0);
            //Geth the amount of arguments given
            int numArgs = args.size();
            //Hold current json
            JSONObject playerJson;
            StringBuilder outputString = new StringBuilder();
            //Switch used to process the command given
            switch(command){
                case "osuPlayer":
                    //Must have a summoner to search for
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !lol <Summoner_Name>**").queue();
                        break;
                    }
                    String osuName = StringUtils.join(args, ' ');
                    playerJson = makeRequest( osuName, "get_user");
                    System.out.println(playerJson);
                    //Send a message in the channel it was recieved
                    event.getChannel().sendMessage(playerInfo(playerJson)).queue();
            }
        } catch (ProtocolException ex) {
            Logger.getLogger(OsuListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OsuListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
         /**
     * makeRequest - Makes a request to the FortniteTracker API to get the given players information
     * @param platform - A String indicating the platform the player is on
     * @param epicName - A String of the players EpicGames name
     * @return obj - A JSONObject containing all the player information given from the API 
     */
    private JSONObject makeRequest(String user, String endpoint) throws MalformedURLException, ProtocolException, IOException{
        //Replace spaces for proper url
        user = user.replace(" ", "%20");
        String urlString;
        urlString = "https://osu.ppy.sh/api/" + endpoint + "?u=" + user + "&k=" + GameBot.config.getProperty("osuKey");
        URL url = new URL(urlString);
        //Make sure to throttle if needed
        //throttle.acquire();
        //Create Connection
        System.out.println(user);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        
        int responseCode = con.getResponseCode();
        
        System.out.println("\nSending 'GET' request to URL : " + urlString);
        System.out.println("Response Code : " + responseCode);
        //Create json from outputed lines
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
           response.append(inputLine);
        }
        in.close();
        String newResponse = response.substring(1, response.length());
        JSONObject obj= new JSONObject(newResponse);
        return obj;
    }
    
    private String playerInfo(JSONObject playerJson){
        StringBuilder header = new StringBuilder();
        //Format the players header
        String level = playerJson.getString("level");
        String acc= playerJson.getString("accuracy");
        
        int levelDec = level.indexOf(".");
        int accDec = acc.indexOf(".");
        
        header.append("__**~ ").append(playerJson.getString("username"))
                .append(" ~**__\n\n")
                .append("Level: ").append(level.substring(0, levelDec)).append("\n")
                .append("Play Count: ").append(playerJson.getString("playcount")).append("\n")
                .append("Global Rank: ").append(playerJson.getString("pp_rank")).append("\n")
                .append("Performance Points: ").append(playerJson.getString("pp_raw")).append("\n")
                .append("Accuracy: ").append(acc.substring(0, accDec+3));
        return header.toString();
    }
    
}
