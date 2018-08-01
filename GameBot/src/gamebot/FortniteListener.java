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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
 * epicName                 String of the users proper epic name
 * MODE_MAP                 A HashMap of FortniteTrackers API keys for game modes to more user friendly strings
 * 
 */
public class FortniteListener extends ListenerAdapter {
    private static final Map<String, String> MODE_MAP = createModeMap();
    private final StringBuilder outputString = new StringBuilder();
    //1 request per 2 seconds
    private final RateLimiter throttle = RateLimiter.create(0.5);
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
            case "fnLifetime":
                //Must have a name to search for
                if(numArgs < 1){
                   event.getChannel().sendMessage("**Usage: !fnLifetime <Epic_Name>**").queue();
                   break;
                }
                //Incase they had a space in their epic name
                epicName = StringUtils.join(args, ' ');
                {
                    try {
                        //Create lifetime output
                        playerJson = makeRequest("pc", epicName);
                        outputString.append("__**~ ").append(playerJson.getString("epicUserHandle")).append(" ~**__\n\n");
                        outputString.append(getLifeTimeStats(playerJson)).append("\n\n");
                        //Solos output
                        outputString.append(getGameModeStats(playerJson, "p2")).append("\n\n");
                        //Duos output
                        outputString.append(getGameModeStats(playerJson, "p10")).append("\n\n");
                        //Squads output
                        outputString.append(getGameModeStats(playerJson, "p9"));
                        event.getChannel().sendMessage(outputString.toString()).queue();
                    } catch (ProtocolException ex) {
                        Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
            case "fnCurrent":
                //Must have a name to search for
                if(numArgs < 1){
                   event.getChannel().sendMessage("**Usage: !fnCurrent <Epic_Name>**").queue();
                   break;
                }
                //Incase they had a space in their epic name
                epicName = StringUtils.join(args, ' ');
                {
                    try {
                        //Create current season total output
                        playerJson = makeRequest("pc", epicName);
                        outputString.append("__**~ ").append(playerJson.getString("epicUserHandle")).append(" ~**__\n\n");
                        outputString.append(getCurrentTotalStats(playerJson)).append("\n\n");
                        //Solos output
                        outputString.append(getGameModeStats(playerJson, "curr_p2")).append("\n\n");
                        //Duos output
                        outputString.append(getGameModeStats(playerJson, "curr_p10")).append("\n\n");
                        //Squads output
                        outputString.append(getGameModeStats(playerJson, "curr_p9"));
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
        //Create json from outputed lines
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
        //Initiate output stringbuilder and header
        StringBuilder tempString = new StringBuilder();
        tempString.append("__***Lifetime***__\n");
        //Get the lifetime JSONObject
        JSONArray lifetimeArray = playerStats.getJSONArray("lifeTimeStats");
        //Get the lifetime games played  value
        tempString.append("**Games Played:** ").append(lifetimeArray.getJSONObject(7).getString("value")).append("\n");
        //Get the lifetime wins value
        tempString.append("**Wins:** ").append(lifetimeArray.getJSONObject(8).getString("value")).append("\n");
        //Get the lifetime win% value
        tempString.append("**Win %:** ").append(lifetimeArray.getJSONObject(9).getString("value")).append("\n");
        //Get the lifetime kills value
        tempString.append("**Kills:** ").append(lifetimeArray.getJSONObject(10).getString("value")).append("\n");
        //Get the lifetime kd value
        tempString.append("**K/D:** ").append(lifetimeArray.getJSONObject(11).getString("value"));
        return tempString.toString();
    }
    
    /**
     * getGameModeStats - Gets the statistics for the given gameMode&season from the given playerStats JSONobject
     * @param playerStats - A JSONObject of the requested players statistics
     * @param gameMode - A string of the game mode to get statistics for
     * @return tempString - A String of the needed statistics 
     */
    private String getGameModeStats(JSONObject playerStats, String gameMode){
        //Initiate output stringbuilder
        StringBuilder tempString = new StringBuilder();
        outputString.append("__***").append(MODE_MAP.get(gameMode)).append("***__\n");
        //Get the mode requesteds JSONObject
        JSONObject mode = playerStats.getJSONObject("stats").getJSONObject(gameMode);
        //Get the overall gameMode games played value
        tempString.append("**Games Played:** ").append(mode.getJSONObject("matches").getString("displayValue")).append("\n");
        //Get the overall gameMode wins value
        tempString.append("**Wins:** ").append(mode.getJSONObject("top1").getString("displayValue")).append("\n");
        //Get the overall gameMode win% value
        tempString.append("**Win %:** ").append(mode.getJSONObject("winRatio").getString("displayValue")).append("%\n");
        //Get the overall gameMode kills value
        tempString.append("**Kills:** ").append(mode.getJSONObject("kills").getString("displayValue")).append("\n");
        //Get the overall gameMode kd value
        tempString.append("**K/D:** ").append(mode.getJSONObject("kd").getString("displayValue"));
        return tempString.toString();
    }
    
        /**
     * getCurrentTotalStats - Gets the total statistics for the current season from the given playerStats JSONobject
     * @param playerStats - A JSONObject of the requested players statistics
     * @return tempString - A String of the needed statistics 
     */
    private String getCurrentTotalStats(JSONObject playerStats){
        int gamesPlayed = 0;
        int totalWins = 0;
        int totalKills = 0;
        double killDeath;
        //Initiate output stringbuilder
        StringBuilder tempString = new StringBuilder();
        tempString.append("__***Current Season Totals***__\n");
        //Get the current solos JSONObject
        JSONObject solos = playerStats.getJSONObject("stats").getJSONObject("curr_p2");
        //Get the current duos JSONObject
        JSONObject duos = playerStats.getJSONObject("stats").getJSONObject("curr_p10");
        //Get the current squads JSONObject
        JSONObject squads = playerStats.getJSONObject("stats").getJSONObject("curr_p9");
        //Get total games played
        gamesPlayed += solos.getJSONObject("matches").getInt("valueInt");
        gamesPlayed += duos.getJSONObject("matches").getInt("valueInt");
        gamesPlayed += squads.getJSONObject("matches").getInt("valueInt");
        tempString.append("**Games Played:** ").append(String.valueOf(gamesPlayed)).append("\n");
        //Get total wins
        totalWins += solos.getJSONObject("top1").getInt("valueInt");
        totalWins += duos.getJSONObject("top1").getInt("valueInt");
        totalWins += squads.getJSONObject("top1").getInt("valueInt");
        tempString.append("**Wins:** ").append(String.valueOf(totalWins)).append("\n");
        //Get the overall win% from wins/games played * 100, then round to nearest whole number
        tempString.append("**Win %:** ").append(String.valueOf(Math.round(((float)totalWins/(float)gamesPlayed)*100))).append("%\n");
        //Get total kills
        totalKills += solos.getJSONObject("kills").getInt("valueInt");
        totalKills += duos.getJSONObject("kills").getInt("valueInt");
        totalKills += squads.getJSONObject("kills").getInt("valueInt");
        tempString.append("**Kills:** ").append(String.valueOf(totalKills)).append("\n");
        //Get the overall kd from kills/(games played - total wins), then round to 2 decimals.
        killDeath = ((double)totalKills)/((double)gamesPlayed-(double)totalWins);
        tempString.append("**K/D:** ").append(String.valueOf(Math.round(killDeath * 100)/100.0));
        return tempString.toString();
    }
    
    /**
     * createRegionMap - Creates an HashMap with keys being the Fortnite tracker
     * API game mode keys and values being user friendly mode names.
     * @return regionMap - HashMap of API game modes to user friendly game mode names
     */
    private static Map<String, String> createModeMap(){
        Map<String, String> modeMap = new HashMap<>();
        modeMap.put("p2", "Overall Solos");
        modeMap.put("p10", "Overall Duos");
        modeMap.put("p9", "Overall Squads");
        modeMap.put("curr_p2", "Current Season Solos");
        modeMap.put("curr_p10", "Current Season Duos");
        modeMap.put("curr_p9", "Current Season Squads");
        return modeMap;
    }
}
