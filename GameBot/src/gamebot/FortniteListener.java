/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gamebot;

import com.google.common.util.concurrent.RateLimiter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
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
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *Purpose: This class implements a ListenerAdapter for a discord bot. It checks
 * for all the Fortnite related commands. The listener outputs data from
 * the FortniteTracker API through the use of HTTPS 'GET' requests
 * 
 * @author Colin Keys
 * 
 * Variables                Description
 * 
 * private
 * 
 * outputString             StringBuilder - used to format output
 * throttler                A RateLimiter to throttle request to the API
 * playerJson               A JSONObject to hold the currently requested players statistics
 * 
 */
public class FortniteListener extends ListenerAdapter {
    private final StringBuilder outputString = new StringBuilder();
    //1 request per 2 seconds
    private RateLimiter throttle = RateLimiter.create(0.5);
    private JSONObject playerJson;
    private String epicName;
    
    /**
     * onMessageReceived - Handles the Listeners actions when a message is received
     * in a channel the bot has read access to.
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        //Get the readable contents of the message
        String message = event.getMessage().getContentDisplay();
        //Dont respond to other bots, this bot, and only handle league commands
        if (event.getAuthor().isBot() || !message.startsWith(GameBot.config.getProperty("prefix") + "fn")) return;
        //Split the args on a space to get them all
        ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
        //Get the command without the prefix
        String command = args.get(0).substring(1);
        //Remove the command from the argument list
        args.remove(0);
        
        //Get number of args
        int numArgs = args.size();
        outputString.setLength(0);
        //Switch used to process the command given
        switch(command){
            //Outputs all the Fortnite related commands
            case "fnHelp":
                outputString.setLength(0);
                outputString.append("__**Fortnite Commands**__\n");
                //Send message in channel it was received
                event.getChannel().sendMessage(outputString.toString()).queue();
                break;
            case "fn":
                if(numArgs < 1){
                   event.getChannel().sendMessage("**Usage: !fn <Epic_Name>**").queue();
                   break;
                }
                epicName = StringUtils.join(args, ' ');
                {
                    try {
                        outputString.append("__**~ ").append(epicName).append(" ~**__\n\n");
                        playerJson = makeRequest("pc", epicName);
                        outputString.append(getLifeTimeStats(playerJson)).append("\n\n");
                        outputString.append(getOverallSolos(playerJson));
                        event.getChannel().sendMessage(outputString.toString()).queue();
                    } catch (ProtocolException ex) {
                        Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
        }
    }
    
     /**
     * makeRequest - Makes a request to the FortniteTracker API to get the given players information
     * @param platform - A String indicating the platform the player is on
     * @param epicName - A String of the players EpicGames name
     * @return obj - A JSONObject containing all the player information given from the API 
     */
    private JSONObject makeRequest(String platform, String epicName) throws MalformedURLException, ProtocolException, IOException{
        //Replace spaces for proper url
        epicName = epicName.replace(" ", "%20");
        
        String urlString = "https://api.fortnitetracker.com/v1/profile/" + platform + "/" + epicName;
        URL url = new URL(urlString);
        //Make sure to throttle if needed
        throttle.acquire();
        //Create Connection
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        con.setRequestProperty("TRN-Api-Key", "746c6319-f383-4a1e-8b87-b18617337950");
        int responseCode = con.getResponseCode();
        
        System.out.println("\nSending 'GET' request to URL : " + urlString);
        System.out.println("Response Code : " + responseCode);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
           response.append(inputLine);
        }
        in.close();
        JSONObject obj = new JSONObject(response.toString());
        return obj;
    }
    
    /**
     * getLifeTimeStats - Gets the lifetime statistics from the given playerStats JSONobject
     * @param playerStats - A JSONObject of the requested players statistics
     * @return tempString - A String of the wanted lifetime statistics
     */
    private String getLifeTimeStats(JSONObject playerStats){
        StringBuilder tempString = new StringBuilder();
        tempString.append("__***Lifetime***__\n");
        JSONArray lifetimeArray = playerStats.getJSONArray("lifeTimeStats");
        
        JSONObject info = lifetimeArray.getJSONObject(8);
        tempString.append("**Wins:** ").append(info.getString("value")).append("\n");
        
        info = lifetimeArray.getJSONObject(9);
        tempString.append("**Win %:** ").append(info.getString("value")).append("\n");
        
        info = lifetimeArray.getJSONObject(10);
        tempString.append("**Kills:** ").append(info.getString("value")).append("\n");
        
        info = lifetimeArray.getJSONObject(11);
        tempString.append("**K/D:** ").append(info.getString("value"));
        return tempString.toString();
    }
    
    /**
     * getLifeTimeStats - Gets the lifetime statistics from the given playerStats JSONobject
     * @param playerStats - A JSONObject of the requested players statistics
     * @return tempString - A String of the needed statistics 
     */
    private String getOverallSolos(JSONObject playerStats){
        StringBuilder tempString = new StringBuilder();
        tempString.append("__***Overall Solos***__\n");
        JSONObject stats = playerStats.getJSONObject("stats");
        
        JSONObject solos = stats.getJSONObject("p2");
        JSONObject tempObj = solos.getJSONObject("top1");
        tempString.append("**Wins:** ").append(tempObj.getString("displayValue")).append("\n");
        
        tempObj = solos.getJSONObject("winRatio");
        tempString.append("**Win %:** ").append(tempObj.getString("displayValue")).append("\n");
        
        tempObj = solos.getJSONObject("kills");
        tempString.append("**Kills:** ").append(tempObj.getString("displayValue")).append("\n");
        
        tempObj = solos.getJSONObject("kd");
        tempString.append("**K/D:** ").append(tempObj.getString("displayValue"));
        return tempString.toString();
    }
}