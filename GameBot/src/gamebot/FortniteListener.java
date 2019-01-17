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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang.StringUtils;
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
 * MODE_MAP                 HashMap of FortniteTrackers API keys for game modes to more user friendly strings
 * platformList             List of all the API platforms
 * totalModes               List of all the API lifetime game modes
 * currModes                List of all the API current season game modes
 * dbOps                    DataboseOps object for database operations
 * throttler                RateLimiter to throttle request to the API
 * playerJson               JSONObject to hold the currently requested players statistics
 * 
 */
public class FortniteListener extends ListenerAdapter {
    private static final Map<String, String> MODE_MAP = createModeMap();
    private final List<String> platformList = Arrays.asList("pc", "psn", "xbl");
    private final List<String> totalModes = Arrays.asList("p2", "p10", "p9");
    private final List<String> currModes = Arrays.asList("curr_p2", "curr_p10", "curr_p9");
    private final DatabaseOps dbOps = new DatabaseOps();
    //1 request per 2 seconds
    private final RateLimiter throttle = RateLimiter.create(0.5);
    
    /**
     * onMessageReceived - Handles the Listeners actions when a message is received
     * in a channel the bot has read access to.
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        try {
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
            //Array for the players jsons
            ArrayList<JSONObject> jsonArray = new ArrayList<>();
            //Get number of args
            int numArgs = args.size();
            //For output formatting
            StringBuilder outputString = new StringBuilder();
            //Hold current json
            JSONObject playerJson;
            //For proper username
            String epicName;
            //Switch used to process the command given
            switch(command){
                //Outputs all the Fortnite related commands
                case "fnHelp":
                    outputString.setLength(0);
                    outputString.append("__**Fortnite Commands**__\n");
                    outputString.append("**All Fortnite commands default to PC!** If you would like to check xbox or ps4 "
                            + "stats please put a **colon ( : )** after your name then the console **PSN or XBL** (case insensitive). Ex: StankBank Frank:xbl or nickmercs:psn\n");
                    outputString.append("**!fnLifetime <epicgames_name>:** Outputs stats for lifetime"
                            + " solos, duos, and squads about given ***epicgames_name*** on pc or the specified platform played platform\n");
                    outputString.append("**!fnCurrent <epicgames_name>:** Outputs stats for the current seasons"
                            + " solos, duos, and squads about given ***epicgames_name*** on pc or the specified platform platform\n");
                    outputString.append("**!fnCompare <epicgames_name>;<epicgames_name>:** Compares and outputs lifetime"
                            + " solos, duos, and squads about given ***epicgames_name***'s on pc or the specified platform (Split player names with a semicolon)\n");
                    //Send message in channel it was received
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "fnHelp");
                    break;
                    //Outputs a players lifetime totals and lifetime solos,duos, and squads totals
                case "fnLifetime":
                    //Must have a name to search for
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !fnLifetime <Epic_Name>[:console]**").queue();
                        break;
                    }
                    //Incase they had a space in their epic name
                    epicName = StringUtils.join(args, ' ');
                    if(epicName.contains(":"))
                        playerJson = makeRequest(epicName.substring(epicName.indexOf(":")+1), epicName.substring(0, epicName.indexOf(":")));
                    else
                        playerJson = makeRequest("pc", epicName);
                    
                    if(playerJson.has("error")){
                        if(epicName.contains(":")){
                            playerNotFound(epicName.substring(0, epicName.indexOf(":")), event);
                            //dbOps.dbUpdate(event, "fnLifetime");
                        }
                        else{
                            playerNotFound(epicName, event);
                            //dbOps.dbUpdate(event, "fnLifetime");
                        }
                        break;
                    }
                    //Format the players header
                    outputString.append(playerHeader(playerJson));
                    //Get the players lifetime totals
                    outputString.append(getLifeTimeStats(playerJson)).append("\n\n");
                    //For each game mode get and display the players stats
                    for(String mode : totalModes)
                        outputString.append(getGameModeStats(playerJson, mode)).append("\n\n");
                    //Send a message in the channel it was recieved
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    //dbOps.dbUpdate(event, "fnLifetime");
                    break;
                    //Outputs a players current season totals and current season solos,duos, and squads totals
                case "fnCurrent":
                    //Must have a name to search for
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !fnCurrent <Epic_Name>[:console]**").queue();
                        break;
                    }
                    //Incase they had a space in their epic name
                    epicName = StringUtils.join(args, ' ');
                   if(epicName.contains(":"))
                        playerJson = makeRequest(epicName.substring(epicName.indexOf(":")+1), epicName.substring(0, epicName.indexOf(":")));
                    else
                        playerJson = makeRequest("pc", epicName);
                    
                    if(playerJson.has("error")){
                        if(epicName.contains(":")){
                            playerNotFound(epicName.substring(0, epicName.indexOf(":")), event);
                            //dbOps.dbUpdate(event, "fnCurrent");
                        }
                        else{
                            playerNotFound(epicName, event);
                            //dbOps.dbUpdate(event, "fnCurrent");
                        }
                        break;
                    }
                    //Format the players header
                    outputString.append(playerHeader(playerJson));
                    //Get the players current season totals
                    outputString.append(getCurrentTotalStats(playerJson)).append("\n\n");
                    //For each game mode get and display the players stats
                    for(String mode : currModes)
                        outputString.append(getGameModeStats(playerJson, mode)).append("\n\n");
                    //Send a message in the channel it was recieved
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    //dbOps.dbUpdate(event, "fnCurrent");
                    break;
                case "fnCompare":
                     //Must have a name to search for
                    if(numArgs < 1 || !StringUtils.join(args, ' ').contains(";")){
                        event.getChannel().sendMessage("**Usage: !fnCompare <Epic_Name>[:console];<Epic_Name>[:console]\nMake "
                                + "sure there is a semicolon ( ; ) seperating the player names**").queue();
                        break;
                    }
                    //Get player names
                    ArrayList<String> playerNames = new ArrayList<>(Arrays.asList(StringUtils.join(args, ' ').split(";")));
                    for(String player : playerNames){
                        if(player.contains(":"))
                            playerJson = makeRequest(player.substring(player.indexOf(":")+1), player.substring(0, player.indexOf(":")));
                        else
                            playerJson = makeRequest("pc", player);
                    
                        if(playerJson.has("error")){
                            playerNotFound("One of the given players", event);
                            //dbOps.dbUpdate(event, "fnCompare");
                            return;
                        }
                        jsonArray.add(playerJson);
                    }
                    //Set each players lifetime stats
                    ArrayList<FortnitePlayer> fnPlayers = new ArrayList<>();
                    for(JSONObject player : jsonArray)
                        fnPlayers.add(setPlayerStats(player));
                    outputString.append(comparePlayerStats(fnPlayers));
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    //dbOps.dbUpdate(event, "fnCompare");
                    break;
            }
        } catch (SQLException | IllegalAccessException ex) {
            Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ProtocolException ex) {
            Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
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
        String urlString;
        platform = platform.toLowerCase();
        if(platform.equals("xbl"))
            urlString = "https://api.fortnitetracker.com/v1/profile/xbox/" + platform + "(" + epicName + ")";
        else
            urlString = "https://api.fortnitetracker.com/v1/profile/" + platform + "/" + epicName;
        URL url = new URL(urlString);
        //Make sure to throttle if needed
        throttle.acquire();
        //Create Connection
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        con.setRequestProperty("TRN-Api-Key", GameBot.config.getProperty("fnKey"));
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
     * seetPlayerStats - Sets the lifetime statistics from the given playerStats JSONobject
     * on a new FortnitePlayer
     * @param playerStats - A JSONObject of the requested players statistics
     * @return player - The created FortnitePlayer object
     */
    private FortnitePlayer setPlayerStats(JSONObject playerStats){
        //Initiate output stringbuilder and header
        FortnitePlayer player = new FortnitePlayer(playerStats.getString("epicUserHandle"), playerStats.getString("platformName").toUpperCase());
        JSONObject mode;
        //Set lifetime totals
        JSONArray lifetimeArray = playerStats.getJSONArray("lifeTimeStats");
        player.setLifetimeGp(lifetimeArray.getJSONObject(7).getString("value"), "totalLifetime");
        player.setLifetimeWins(lifetimeArray.getJSONObject(8).getString("value"), "totalLifetime");
        player.setLifetimeWp(lifetimeArray.getJSONObject(9).getString("value"), "totalLifetime");
        player.setLifetimeKills(lifetimeArray.getJSONObject(10).getString("value"), "totalLifetime");
        player.setLifetimeKd(lifetimeArray.getJSONObject(11).getString("value"), "totalLifetime");
        //Solo totals
        if(playerStats.getJSONObject("stats").has("p2")){
            mode = playerStats.getJSONObject("stats").getJSONObject("p2");
            player.setLifetimeGp(mode.getJSONObject("matches").getString("displayValue"), "soloLifetime");
            player.setLifetimeWins(mode.getJSONObject("top1").getString("displayValue"), "soloLifetime");
            player.setLifetimeWp(mode.getJSONObject("winRatio").getString("displayValue") + "%", "soloLifetime");
            player.setLifetimeKills(mode.getJSONObject("kills").getString("displayValue"), "soloLifetime");
            player.setLifetimeKd(mode.getJSONObject("kd").getString("displayValue"), "soloLifetime");
        }
        //Duo totals
        if(playerStats.getJSONObject("stats").has("p10")){
            mode = playerStats.getJSONObject("stats").getJSONObject("p10");
            player.setLifetimeGp(mode.getJSONObject("matches").getString("displayValue"), "duoLifetime");
            player.setLifetimeWins(mode.getJSONObject("top1").getString("displayValue"), "duoLifetime");
            player.setLifetimeWp(mode.getJSONObject("winRatio").getString("displayValue") + "%", "duoLifetime");
            player.setLifetimeKills(mode.getJSONObject("kills").getString("displayValue"), "duoLifetime");
            player.setLifetimeKd(mode.getJSONObject("kd").getString("displayValue"), "duoLifetime");
        }
        //Squad totals
        if(playerStats.getJSONObject("stats").has("p9")){
            mode = playerStats.getJSONObject("stats").getJSONObject("p9");
            player.setLifetimeGp(mode.getJSONObject("matches").getString("displayValue"), "squadLifetime");
            player.setLifetimeWins(mode.getJSONObject("top1").getString("displayValue"), "squadLifetime");
            player.setLifetimeWp(mode.getJSONObject("winRatio").getString("displayValue") + "%", "squadLifetime");
            player.setLifetimeKills(mode.getJSONObject("kills").getString("displayValue"), "squadLifetime");
            player.setLifetimeKd(mode.getJSONObject("kd").getString("displayValue"), "squadLifetime");
        }
        
        return player;
    }
    
    /**
     * comparePlayerStats - Compares all lifetime statistics between 2 FortniePlayer objects
     * and creates an output string with both players statistics that highlights whose are better
     * @param fnPlayers - An ArrayList of the FortnitPlayer objects
     * @return tempString - A String of the wanted lifetime statistics
     */
    private String comparePlayerStats(ArrayList<FortnitePlayer> fnPlayers){
        //Create output string and variables to hold array objects
        StringBuilder tempString = new StringBuilder();
        FortnitePlayer playerOne = fnPlayers.get(0);
        FortnitePlayer playerTwo = fnPlayers.get(1);
        //Compare stats
        playerOne.compareAllLifetime(playerTwo, "totalLifetime");
        playerOne.compareAllLifetime(playerTwo, "soloLifetime");
        playerOne.compareAllLifetime(playerTwo, "duoLifetime");
        playerOne.compareAllLifetime(playerTwo, "squadLifetime");
        //Make headers
        tempString.append("__**~ ").append(playerOne.getPlayerName()).append(" (").append(playerOne.getPlatform())
                .append(")").append(" | ").append(playerTwo.getPlayerName()).append(" (")
                .append(playerTwo.getPlatform()).append(") ~**__\n\n");
        //Get the lifetime total games played, wins, win%, kills, and kd values
        tempString.append("__***Lifetime***__\n");
        tempString.append("**Games Played:** ").append(playerOne.getLifetimeGp("totalLifetime")).append(" | ").append(playerTwo.getLifetimeGp("totalLifetime")).append("\n");
        tempString.append("**Wins:** ").append(playerOne.getLifetimeWins("totalLifetime")).append(" | ").append(playerTwo.getLifetimeWins("totalLifetime")).append("\n");
        tempString.append("**Win %:** ").append(playerOne.getLifetimeWp("totalLifetime")).append(" | ").append(playerTwo.getLifetimeWp("totalLifetime")).append("\n");
        tempString.append("**Kills:** ").append(playerOne.getLifetimeKills("totalLifetime")).append(" | ").append(playerTwo.getLifetimeKills("totalLifetime")).append("\n");
        tempString.append("**K/D:** ").append(playerOne.getLifetimeKd("totalLifetime")).append(" | ").append(playerTwo.getLifetimeKd("totalLifetime")).append("\n\n");
        //Get the solo lifetime games played, wins, win%, kills, and kd values
        tempString.append("__***Overall Solos***__\n");
        tempString.append("**Games Played:** ").append(playerOne.getLifetimeGp("soloLifetime")).append(" | ").append(playerTwo.getLifetimeGp("soloLifetime")).append("\n");
        tempString.append("**Wins:** ").append(playerOne.getLifetimeWins("soloLifetime")).append(" | ").append(playerTwo.getLifetimeWins("soloLifetime")).append("\n");
        tempString.append("**Win %:** ").append(playerOne.getLifetimeWp("soloLifetime")).append(" | ").append(playerTwo.getLifetimeWp("soloLifetime")).append("\n");
        tempString.append("**Kills:** ").append(playerOne.getLifetimeKills("soloLifetime")).append(" | ").append(playerTwo.getLifetimeKills("soloLifetime")).append("\n");
        tempString.append("**K/D:** ").append(playerOne.getLifetimeKd("soloLifetime")).append(" | ").append(playerTwo.getLifetimeKd("soloLifetime")).append("\n\n");
        //Get the duo lifetime games played, wins, win%, kills, and kd values
        tempString.append("__***Overall Duos***__\n");
        tempString.append("**Games Played:** ").append(playerOne.getLifetimeGp("duoLifetime")).append(" | ").append(playerTwo.getLifetimeGp("duoLifetime")).append("\n");
        tempString.append("**Wins:** ").append(playerOne.getLifetimeWins("duoLifetime")).append(" | ").append(playerTwo.getLifetimeWins("duoLifetime")).append("\n");
        tempString.append("**Win %:** ").append(playerOne.getLifetimeWp("duoLifetime")).append(" | ").append(playerTwo.getLifetimeWp("duoLifetime")).append("\n");
        tempString.append("**Kills:** ").append(playerOne.getLifetimeKills("duoLifetime")).append(" | ").append(playerTwo.getLifetimeKills("duoLifetime")).append("\n");
        tempString.append("**K/D:** ").append(playerOne.getLifetimeKd("duoLifetime")).append(" | ").append(playerTwo.getLifetimeKd("duoLifetime")).append("\n\n");
        //Get the squad lifetime games played, wins, win%, kills, and kd values
        tempString.append("__***Overall Squads***__\n");
        tempString.append("**Games Played:** ").append(playerOne.getLifetimeGp("squadLifetime")).append(" | ").append(playerTwo.getLifetimeGp("squadLifetime")).append("\n");
        tempString.append("**Wins:** ").append(playerOne.getLifetimeWins("squadLifetime")).append(" | ").append(playerTwo.getLifetimeWins("squadLifetime")).append("\n");
        tempString.append("**Win %:** ").append(playerOne.getLifetimeWp("squadLifetime")).append(" | ").append(playerTwo.getLifetimeWp("squadLifetime")).append("\n");
        tempString.append("**Kills:** ").append(playerOne.getLifetimeKills("squadLifetime")).append(" | ").append(playerTwo.getLifetimeKills("squadLifetime")).append("\n");
        tempString.append("**K/D:** ").append(playerOne.getLifetimeKd("squadLifetime")).append(" | ").append(playerTwo.getLifetimeKd("squadLifetime")).append("\n");
        
        return tempString.toString();
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
        tempString.append("__***").append(MODE_MAP.get(gameMode)).append("***__\n");
        //Get the mode requesteds JSONObject
        if(!playerStats.getJSONObject("stats").has(gameMode)){
            tempString.append("No stats for this playlist. Play some matches first!");
            return tempString.toString();
        }
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
        //Initiate counters
        int gamesPlayed = 0;
        int totalWins = 0;
        int totalKills = 0;
        double killDeath;
        //Initiate json
        JSONObject mode;
        //Mode names for for each
        List<String> modeList = Arrays.asList("curr_p2", "curr_p10", "curr_p9");
        //Initiate output stringbuilder
        StringBuilder tempString = new StringBuilder();
        tempString.append("__***Current Season Totals***__\n");
        //For each to grab wanted stats from each game mode
        for(String modeName : modeList){
            if(playerStats.getJSONObject("stats").has(modeName)){
                mode = playerStats.getJSONObject("stats").getJSONObject(modeName);
                gamesPlayed += mode.getJSONObject("matches").getInt("valueInt");
                totalWins += mode.getJSONObject("top1").getInt("valueInt");
                totalKills += mode.getJSONObject("kills").getInt("valueInt");
            }
        }
        //Format output
        tempString.append("**Games Played:** ").append(String.valueOf(gamesPlayed)).append("\n");
        tempString.append("**Wins:** ").append(String.valueOf(totalWins)).append("\n");
        //Format 2 nearest whole number
        tempString.append("**Win %:** ").append(String.valueOf(Math.round(((float)totalWins/(float)gamesPlayed)*100))).append("%\n");
        tempString.append("**Kills:** ").append(String.valueOf(totalKills)).append("\n");
        killDeath = ((double)totalKills)/((double)gamesPlayed-(double)totalWins);
        //Format to 2 decimal places
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
    
    /**
     * getFavePlatform - Finds the platform the player has the most played games on
     * @param platformJsons - an ArrayList of JSONObjects to search through
     * @return favePlatform - the JSONObject of the platform the user has played the most games on
     */
    private static JSONObject getFavePlatform(ArrayList<JSONObject> platformJsons){
        int tempMost = 0;
        int tempPlayed;
        JSONObject favePlatform = platformJsons.get(0);
        //Loop through each object
        for(JSONObject platform : platformJsons){
            //Get amount of matches played
            JSONArray lifetimeArray = platform.getJSONArray("lifeTimeStats");
            tempPlayed = Integer.parseInt(lifetimeArray.getJSONObject(7).getString("value"));
            //If more played on latest platform, change fave platform and games played
            if(tempPlayed > tempMost){
                tempMost = tempPlayed;
                favePlatform = platform;
            }
        }
        return favePlatform;
    }
    
    /**
     * getAllJsons - Gets a JSONs for each platform the player has played on
     * @param epicName - epic games name
     * @return jsonArray - ArrayList of JSONs
     */
    private ArrayList<JSONObject> getAllJsons(String epicName) throws ProtocolException, IOException{
        //Array for the players jsons
        ArrayList<JSONObject> jsonArray = new ArrayList<>();
        JSONObject playerJson;
        //Get JSON for each platform the user has played on
        for(String platform : platformList){
            playerJson = makeRequest(platform, epicName);
            if(!playerJson.has("error"))
                jsonArray.add(playerJson);
        }
        
        return jsonArray;
    }
    
    /**
     * playerNotFound - Formats and sends a message for when a player is not found
     * @param epicName - epic games name
     * @param event - MessageReceivedEvent instance generated when the bot
     */
    private void playerNotFound(String epicName, MessageReceivedEvent event){
        StringBuilder invalidName = new StringBuilder();
        invalidName.append("Invalid player name! **").append(epicName)
                .append("** could not be found");
        event.getChannel().sendMessage(invalidName.toString()).queue(); 
    }
    
    private String playerHeader(JSONObject playerJson){
        StringBuilder header = new StringBuilder();
        //Format the players header
        header.append("__**~ ").append(playerJson.getString("epicUserHandle"))
                .append(" (").append(playerJson.getString("platformName").toUpperCase())
                .append(") ~**__\n\n");
        return header.toString();
    }
}
