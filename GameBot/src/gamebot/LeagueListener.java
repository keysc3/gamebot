package gamebot;

import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.common.Season;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMasteries;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMastery;
import com.merakianalytics.orianna.types.core.league.LeaguePosition;
import com.merakianalytics.orianna.types.core.league.LeaguePositions;
import com.merakianalytics.orianna.types.core.spectator.CurrentMatch;
import com.merakianalytics.orianna.types.core.spectator.CurrentMatchTeam;
import com.merakianalytics.orianna.types.core.spectator.Player;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;


/**
 *Purpose: This class implements a ListenerAdapter for a discord bot. It checks
 * for all the League of Legends related commands. The listener outputs data from
 * the official League of Legends API through the use of the Orianna library.
 * 
 * @author Colin Keys
 * 
 * Variables                Description
 * 
 * private static final
 * 
 * REGION_MAP               HashMap - keys are the API format of regions, values are readable format of regions
 * REGIONABB_MAP            HashMap - keys are the region abbreviations, values are the API format of regions
 * QUEUE_MAP                HashMap - keys are the API format of queues, values are the readable format of queues
 * TIER_MAP                 HashMap - keys are the API format of tiers, values are the readable format of tiers
 * TEAM_MAP                 HashMap - keys are the API format of teams, values are the readable format of teams
 * DIVISION_MAP             ashMap - keys are the API format of divisions, values are the integer format of divisions
 * 
 * private
 * 
 * dbOps                    DataboseOps object for database operations
 * 
 */
public class LeagueListener extends ListenerAdapter{
    
    private static final Map<String, String> REGION_MAP = createRegionMap();
    private static final Map<String, String> REGIONABB_MAP = createRegionAbbMap();
    private static final Map<String, String> QUEUE_MAP = createQueueMap();
    private static final Map<String, String> TIER_MAP = createTierMap();
    private static final Map<String, String> TEAM_MAP = createTeamMap();
    private static final Map<String, String> DIVISION_MAP = createDivisionMap();
    private final DatabaseOps dbOps = new DatabaseOps();
    
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
            if (event.getAuthor().isBot() || !message.startsWith(GameBot.config.getProperty("prefix") + "lol")) return;
            //Split the args on a space to get them all
            ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
            //Get the command without the prefix
            String command = args.get(0).substring(1);
            //Remove the command from the argument list
            args.remove(0);
            
            //Get number of args
            int numArgs = args.size();
            //Region abbreviation given
            String regionGiven;
            //For output formatting
            StringBuilder outputString = new StringBuilder();
            //Switch used to process the command given
            switch(command){
                //Outputs all the League of Legends related commands
                case "lolHelp":
                    outputString.setLength(0);
                    outputString.append("__**League of Legends Commands**__\n");
                    outputString.append("**!lol <summoner_name>:** Outputs info about given ***summoner_name*** in NA\n");
                    outputString.append("**!lolRegion <region> <summoner_name>:** "
                            + "Outputs info about given ***summoner_name*** in given ***region***\n");
                    outputString.append("**!lolRanks <summoner_name>:** Outputs "
                            + "given ***summoner_name***'s rank in each queue they are ranked in on the NA server\n");
                    outputString.append("**!lolRanksRegion <region> <summoner_name>:** "
                            + "Outputs given ***summoner_name***'s rank in each queue they are ranked in on the given ***region***\n");
                    outputString.append("**!lolLive <summoner_name>:** Outputs info about given ***summoner_name***'s live game\n");
                    outputString.append("**!lolLiveRegion <region> <summoner_name>:** Outputs info about given ***summoner_name***'s live "
                            + "game on the given ***region***\n");
                    //Add use to db
                    dbOps.dbUpdate(event, "lolHelp");
                    //Send message in channel it was received
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    break;
                    //Outputs info about the given summoner if they are on the NA server
                case "lol":
                    //Must have a summoner to search for
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !lol <Summoner_Name>**").queue();
                        break;
                    }
                    //Process the summoner
                    event.getChannel().sendMessage(summoner(args, "NA")).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "lol");
                    break;
                    //Outputs info about the given summoner if they are on the given server
                case "lolRegion":
                    /*Must have a region to search on and a summoner to search for.
                    The region given must be a key in the abbreviation hashmap*/
                    if(numArgs < 2 || REGIONABB_MAP.get(args.get(0)) == null){
                        event.getChannel().sendMessage("**Usage: !lolRegion <Region> <Summoner_Name>**\n" + regionOptions()).queue();
                        break;
                    }
                    //Get the region abbreviation given and remove it from the args
                    regionGiven = args.get(0);
                    args.remove(0);
                    //Process the summoner
                    event.getChannel().sendMessage(summoner(args, regionGiven)).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "lolRegion");
                    break;
                    //Outputs info about the Leagues the given summoner is ranked in on the NA server
                case "lolRanks":
                    //Must have a summoner to search for
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !lolRanks <Summoner_Name>**").queue();
                        break;
                    }
                    //Process the summoner
                    event.getChannel().sendMessage(summonerRanks(args, "NA")).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "lolRanks");
                    break;
                    //Outputs info about the Leagues the given summoner is ranked in on the given server
                case "lolRanksRegion":
                    /*Must have a region to search on and a summoner to search for.
                    The region given must be a key in the abbreviation hashmap*/
                    if(numArgs < 2 || REGIONABB_MAP.get(args.get(0)) == null){
                        event.getChannel().sendMessage("**Usage: !lolRanksRegion <Region> <Summoner_Name>**\n" + regionOptions()).queue();
                        break;
                    }
                    //Get the region abbreviation given and remove it from the args
                    regionGiven = args.get(0);
                    args.remove(0);
                    //Process the summoner
                    event.getChannel().sendMessage(summonerRanks(args, regionGiven)).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "lolRanksRegion");
                    break;
                    //Outputs info about the current game the given summoner is in on NA
                case "lolLive":
                    //Must have a summoner to search for
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !lolCurrentGame <Summoner_Name>**\n").queue();
                        break;
                    }
                    //Process the summoner
                    event.getChannel().sendMessage(summonerLiveGame(args, "NA")).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "lolLive");
                    break;
                    //Outputs info about the current game the given summoner is in on given region
                case "lolLiveRegion":
                    /*Must have a region to search on and a summoner to search for.
                    The region given must be a key in the abbreviation hashmap*/
                    if(numArgs < 2 || REGIONABB_MAP.get(args.get(0)) == null){
                        event.getChannel().sendMessage("**Usage: !lolCurrentGameRegion <Region> <Summoner_Name>**\n" + regionOptions()).queue();
                        break;
                    }
                    //Get the region abbreviation given and remove it from the args
                    regionGiven = args.get(0);
                    args.remove(0);
                    //Process the summoner
                    event.getChannel().sendMessage(summonerLiveGame(args, regionGiven)).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "lolLiveRegion");
                    break;
            }
        } catch (SQLException | IllegalAccessException ex) {
            Logger.getLogger(LeagueListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * setUpOrianna - Sets up Orianna for the NA server
     */
    private void setUpOrianna(){
        //Set up the config, api key, and default regian
        Orianna.loadConfiguration(new File(GameBot.config.getProperty("oriannaConfigPath")));
        Orianna.setRiotAPIKey(GameBot.config.getProperty("riotKey"));
        Orianna.setDefaultRegion(Region.valueOf(GameBot.config.getProperty("riotDefaultRegion")));
    }
    
    /**
     * getSummoner - Gets a summoner object of the given summonerName on 
     * the NA server.
     * @param summonerName - String of the given summoner name to process
     * @return mySummoner - The Summoner object containing info from the API
     */
    private Summoner getSummoner(String summonerName){
        //Get Summoner object to get output data from
        Summoner mySummoner = Orianna.summonerNamed(summonerName).get();
        return mySummoner;
    }
    
    /**
     * getSummoneRegionSpecific - Gets a summoner object of the given summonerName on 
     * the given server.
     * @param summonerName - String of the given summoner name to process
     * @param region - String of the given region abbreviation
     * @return mySummoner - The Summoner object containing info from the API
     */
    private Summoner getSummonerRegionSpecific(String summonerName, String region){
        //Get Summoner object to get output data from
        Summoner mySummoner = Orianna.summonerNamed(summonerName).withRegion(Region.valueOf(region)).get();
        return mySummoner;
    }
    
    /**
     * getSummonerName - Gets a string of the original summoner name format since
     * it is split into arguments when the message is received.
     * @param args - ArrayList of the command arguments (args to join into a name)
     * @return mySummonerName - Summoner name in the originally given format
     */
    private String getSummonerName(ArrayList args){
        //Join the name
        String mySummonerName = StringUtils.join(args, ' ');
        return mySummonerName;
    }
    
    /**
     * createRegionMap - Creates an HashMap with keys being the Orianna Region
     * ENUM's and values being a more user friendly format of the ENUM.
     * @return regionMap - HashMap of ENUM region names to user friendly region names
     */
    private static Map<String, String> createRegionMap(){
        Map<String, String> regionMap = new HashMap<>();
        regionMap.put("NORTH_AMERICA", "North America");
        regionMap.put("BRAZIL", "Brazil");
        regionMap.put("EUROPE_NORTH_EAST", "Europe Nordic & East");
        regionMap.put("EUROPE_WEST", "Europe West");
        regionMap.put("JAPAN", "Japan");
        regionMap.put("KOREA", "Korea");
        regionMap.put("LATIN_AMERICA_NORTH", "Latin America North");
        regionMap.put("LATIN_AMERICA_SOUTH", "Latin America South");
        regionMap.put("OCEANIA", "Oceania");
        regionMap.put("RUSSIA", "Russia");
        regionMap.put("TURKEY", "Turkey");
        return regionMap;
    }
    
    /**
     * createRegionAbbMap - Creates an HashMap with keys being the region abbreviations
     * and values being the Orianna Region ENUM's.
     * @return regionAbbMap - HashMap of region abbreviations to their associated
     * ENUM Region
     */
    private static Map<String, String> createRegionAbbMap(){
        Map<String, String> regionAbbMap = new HashMap<>();
        regionAbbMap.put("NA", "NORTH_AMERICA");
        regionAbbMap.put("BR", "BRAZIL");
        regionAbbMap.put("EUNE", "EUROPE_NORTH_EAST");
        regionAbbMap.put("EUW", "EUROPE_WEST");
        regionAbbMap.put("JP", "JAPAN");
        regionAbbMap.put("KR", "KOREA");
        regionAbbMap.put("LAN", "LATIN_AMERICA_NORTH");
        regionAbbMap.put("LAS", "LATIN_AMERICA_SOUTH");
        regionAbbMap.put("OCE", "OCEANIA");
        regionAbbMap.put("RU", "RUSSIA");
        regionAbbMap.put("TR", "TURKEY");
        return regionAbbMap;
    }
    
    /**
     * createQueueMap - Creates an HashMap with keys being the Orianna Queue
     * ENUM's and values being a more user friendly format of the ENUM
     * @return queueMap - HashMap of ENUM Queue names to user friendly queue names
     */
    private static Map<String, String> createQueueMap(){
        Map<String, String> queueMap = new HashMap<>();
        queueMap.put("RANKED_SOLO_5x5", "Solo/Duo 5v5");
        queueMap.put("RANKED_FLEX_SR", "Flex 5v5");
        queueMap.put("RANKED_FLEX_TT", "Flex 3v3");
        queueMap.put("NORMAL_3X3_BLIND", "Twisted Treeline Blind 3v3");
        queueMap.put("NORMAL_5X5_BLIND", "Summoners Rift Blind 5v5");
        queueMap.put("NORMAL_5X5_DRAFT", "Summoners Rift Draft 5v5");
        queueMap.put("ARAM", "Howling Abyss ARAM 5v5");
        queueMap.put("TEAM_BUILDER_RANKED_SOLO", "Solo/Duo 5v5");
        queueMap.put("TB_BLIND_SUMMONERS_RIFT_5x5", "Summoners Rift Blind 5v5");
        return queueMap;
    }
    
    /**
     * createTierMap - Creates an HashMap with keys being the Orianna Tier
     * ENUM's and values being a more user friendly format of the ENUM
     * @return tierMap - HashMap of ENUM Tier names to user friendly tier names
     */
    private static Map<String, String> createTierMap(){
        Map<String, String> tierMap = new HashMap<>();
        tierMap.put("BRONZE", "Bronze");
        tierMap.put("SILVER", "Silver");
        tierMap.put("GOLD", "Gold");
        tierMap.put("PLATINUM", "Platinum");
        tierMap.put("DIAMOND", "Diamond");
        tierMap.put("MASTER", "Master");
        tierMap.put("CHALLENGER", "Challenger");
        return tierMap;
    }
    
    /**
     * createDivisionMap - Creates an HashMap with keys being the Orianna Division
     * ENUM's and values being a more user friendly format of the ENUM
     * @return divisionMap - HashMap of ENUM Division names to user friendly tier names
     */
    private static Map<String, String> createDivisionMap(){
        Map<String, String> divisionMap = new HashMap<>();
        divisionMap.put("I", "1");
        divisionMap.put("II", "2");
        divisionMap.put("III", "3");
        divisionMap.put("IV", "4");
        divisionMap.put("V", "5");
        return divisionMap;
    }
    
    /**
     * createTeamMap - Creates an HashMap with keys being the Orianna Team
     * ENUM's and values being a more user friendly format of the ENUM.
     * @return teamMap - HashMap of ENUM team names to user friendly team names
     */
    private static Map<String, String> createTeamMap(){
        Map<String, String> teamMap = new HashMap<>();
        teamMap.put("BLUE", "Blue");
        teamMap.put("RED", "Red");
        return teamMap;
    }
    
    /**
     * regionOptions - Creates a StringBuilder of all the available regions 
     * and their abbreviations.
     * @return regionString - String of available regions and their abbreviations
     */
    private String regionOptions(){
        StringBuilder regionString = new StringBuilder();
        regionString.append("__**Region Options**__\n").append("**NA** - North America\n")
                .append("**EUNE** - Europe Nordic & East\n").append("**EUW** - Europe West\n")
                .append("**BR** - Brazil\n").append("**JP** - Japan\n").append("**KR** - Korea\n")
                .append("**LAN** - Latin America North\n").append("**LAS** - Latin America South\n")
                .append("**OCE** - Oceania\n").append("**RU** - Russia\n").append("**TR** - Turkey\n");
        return regionString.toString();
    }
    
    /**
     * summonerRanks - Outputs info about all the Leagues the given summoner is 
     * ranked in on the NA server by default, otherwise the given server.
     * @param args - ArrayList of the given summoner name split on spaces
     * @param region - String of the given abbreviated region to search on
     * @return String - String of the formatted output
     */
    private String summonerRanks(ArrayList args, String region){
        //Reset output string, set up Orianna, and summoner name
        setUpOrianna();
        StringBuilder tempString = new StringBuilder();
        //Build proper name
        String summonerName = getSummonerName(args);
        //Get the summoner, NA if no region is given
        Summoner summoner;
        if(region.equals("NA"))
            summoner = getSummoner(summonerName);
        else{
            summoner = getSummonerRegionSpecific(summonerName, REGIONABB_MAP.get(region));
        }
        
        //Make sure summoner exists
        if(!checkSummonerExists(summoner)){
            return summonerDoesNotExist(summonerName, REGIONABB_MAP.get(region));
        }
        //Get proper profile name
        String properName = summoner.getName();
        //Get the positions summoner is in for the leagues they are ranked in
        final LeaguePositions positions = summoner.getLeaguePositions();
        //If they are not ranked in any leagues output results and return
        if(positions.isEmpty()){
            tempString.append("**").append(properName).append("** is not ranked in any leagues!");
            return tempString.toString();
        }
        
        tempString.append("__**Leagues ").append(properName).append(" is ranked in:**__\n");
        //Go through each league and get the desired data from each one
        for(final LeaguePosition leaguePosition : positions) {
            //Queue name, tier name, division number, LP amount, W/L, and W/L ratio
            tempString.append("**").append(QUEUE_MAP.get(leaguePosition.getQueue().toString())).append(":** ");
            tempString.append(TIER_MAP.get(leaguePosition.getTier().toString())).append(" ");
            tempString.append(leaguePosition.getDivision().toString()).append(" ");
            tempString.append(String.valueOf(leaguePosition.getLeaguePoints())).append("LP ");
            int wins = leaguePosition.getWins();
            int losses = leaguePosition.getLosses();
            double winPercent = (double)wins/((double)wins+(double)losses);
            tempString.append("(W/L: ").append(wins)
                    .append("/").append(losses)
                    .append(" ").append(Math.round(winPercent * 100))
                    .append("%)\n");
            if(leaguePosition.getPromos() != null) {
                // If the summoner is in their promos show progress
                tempString.append("| Promos progress: ")
                        .append(leaguePosition.getPromos().getProgess().replace('N', '-'))
                        .append(" |\n");
            }
        }
        //Send message in channel it was received in
        return tempString.toString();
    }
    
    /**
     * summonerRanks - Outputs info about the given summoner on the NA server 
     * by default, otherwise the given server.
     * @param args - ArrayList of the given summoner name split on spaces
     * @param region - String of the given abbreviated region to search on
     * @return String - String of the formatted output
     */
    private String summoner(ArrayList args, String region){
        //Reset output string, set up Orianna, and summoner name
        setUpOrianna();
        StringBuilder tempString = new StringBuilder();
        //Build proper name
        String summonerName = getSummonerName(args);
        //Get the summoner, NA if no region is given
        Summoner summoner;
        if(region.equals("NA"))
            summoner = getSummoner(summonerName);
        else{
            summoner = getSummonerRegionSpecific(summonerName, REGIONABB_MAP.get(region));
        }

        //Make sure summoner exists
        if(!checkSummonerExists(summoner)){
            return summonerDoesNotExist(summonerName, REGIONABB_MAP.get(region));
        }

        //Build summoner level and region output
        tempString.append("__**").append(summoner.getName()).append("**__").append("\n");
        tempString.append("**Level:** ").append(summoner.getLevel()).append("\n");
        tempString.append("**Region:** ").append(REGION_MAP.get(summoner.getRegion().toString())).append("\n");

        tempString.append("\n__**Top 3 Champs By Mastery:**__\n");
        //Get the champion mastery stats on all champs they have a point on
        ChampionMasteries champMasts = summoner.getChampionMasteries();
        //Only want to display their top three, which are the first three
        for(int i = 0; i < 3; i++){
            //Get the champ
            ChampionMastery singleChampMast = champMasts.get(i);
            //Build the Rank, champion name, and champion points output
            tempString.append(String.valueOf(i+1)).append(". ")
                    .append(singleChampMast.getChampion().getName()).append(" - ")
                    .append(String.valueOf(singleChampMast.getPoints()))
                    .append(" pts\n");
        }
        System.out.println(summoner.isInGame());
        //Send message in channel it was received in
        return tempString.toString();
    }

    /**
     * checkSummonerExists - Return true if summoner exists false otherwise
     * @param summoner - summoner to check if they exist
     */
    private Boolean checkSummonerExists(Summoner summoner){
        //Used to check if summoner is valid for now
        //Dont know a different way yet
        try{
            summoner.getLevel();
        }
        catch(NullPointerException e){
            return false;
        }
        return true;
    }
    
    /**
     * summonerDoesNotExist - Outputs that the summoner does not exist on the NA server
     * @param summonerName - name of given summoner
     * @return String - String of the formatted output
     */
    private String summonerDoesNotExist(String summonerName, String region){
        StringBuilder tempString = new StringBuilder().append("**").append(summonerName).append("** does not exist on the **")
                .append(REGION_MAP.get(region)).append("** server");
        return tempString.toString();
    }
    
    /**
     * summonerLiveGame - Outputs info about the current game the given summoner
     * is in on the given server
     * @param args - ArrayList of the given summoner name split on spaces
     * @param region - String of the given abbreviated region to search on
     * @return String - String of the formatted output
     */
    private String summonerLiveGame(ArrayList args, String region){
        //Setup output string, set up Orianna, and summoner name
        setUpOrianna();
        StringBuilder tempString = new StringBuilder();
        //Build proper name
        String summonerName = getSummonerName(args);
        //Get the summoner, NA if no region is given
        Summoner summoner;
        if(region.equals("NA"))
            summoner = getSummoner(summonerName);
        else{
            summoner = getSummonerRegionSpecific(summonerName, REGIONABB_MAP.get(region));
        }

        //Make sure summoner exists
        if(!checkSummonerExists(summoner)){
            return summonerDoesNotExist(summonerName, REGIONABB_MAP.get(region));
        }
        //Proper summoner name
        String properName = summoner.getName();
        //Get the positions summoner is in for the leagues they are ranked in
        final CurrentMatch currentGame = summoner.getCurrentMatch();
        //Make sure they are in a game
        if(currentGame.exists()){
            //Get game type and duration
            String queueName = QUEUE_MAP.get(currentGame.getQueue().toString());
            final Player player = currentGame.getParticipants().find(summoner);
            Interval interval = new Interval(currentGame.getCreationTime(), DateTime.now());
            //Get proper seconds format
            String seconds = String.valueOf((int)interval.toDuration().getStandardSeconds()%60);
            if(seconds.length() == 1)
                seconds = "0" + seconds;
            //Combine minutes and seconds for game duration
            String gameDuration = String.valueOf((int)interval.toDuration().getStandardSeconds()/60)
                    + ":" + seconds;
            if(gameDuration.length() > 5)
                gameDuration = "Loading In!";
            tempString.append("__**").append(properName).append("**__ is in a __**")
                    .append(queueName).append("**__ game!\n")
                    .append("**Server:** ").append(REGION_MAP.get(REGIONABB_MAP.get(region))).append("\n")
                    .append("**Champion: **").append(player.getChampion().getName()).append("\n")
                    .append("**Spells: **").append(player.getSummonerSpellD().getName()).append("/").append(player.getSummonerSpellF().getName()).append("\n")
                    .append("**Duration: **").append(gameDuration).append("\n")
                    .append("**Team Side: **").append(TEAM_MAP.get(player.getTeam().getSide().name())).append("\n\n");
                List<CurrentMatchTeam> teams = Arrays.asList(currentGame.getBlueTeam(), currentGame.getRedTeam());
                tempString.append("__**Solo/Duo 5v5 Stats**__\n");
                tempString.append("Name | Champ | S8 Rank | Ranked WR | S7 Rank |\n\n");
                for(CurrentMatchTeam team : teams){
                    tempString.append("__**").append(TEAM_MAP.get(team.getSide().name())).append(" Team**__\n");
                    team.getParticipants().forEach((teamPlayer) -> {
                        //Get Summoner and league position
                        Summoner playerProf = teamPlayer.getSummoner();
                        LeaguePosition position = playerProf.getLeaguePosition(Queue.RANKED_SOLO_5x5);
                        //Get Summoner name
                        tempString.append(playerProf.getName()).append(" | ");
                        //Get Champion name
                        tempString.append(teamPlayer.getChampion().getName()).append(" | ");
                        //Get Ranked stats if applicable
                        if(position != null){
                            tempString.append(position.getTier().toString().substring(0,1));
                            //Get the integer value of the players Solo/Duo 5v5 Division
                            tempString.append(DIVISION_MAP.get(position.getDivision().toString()));
                            //Get the amount of LP the player has in the Solo/Duo 5v5 Queue
                            tempString.append("(").append(position.getLeaguePoints()).append("LP) | ");
                            //Get win percent and games played
                            int wins = position.getWins();
                            int losses = position.getLosses();
                            double winPercent = (double)wins/((double)wins+(double)losses);
                            tempString.append(Math.round(winPercent * 100)).append("%(").append(wins+losses).append("GP) | ");
                        }
                        else{
                            tempString.append("Unranked | ");
                            tempString.append("N/A | ");
                        }
                        //Get players highest tier last season
                        tempString.append(playerProf.getHighestTier(Season.SEASON_8).toString().substring(0,1)).append("\n");
                    });
                    tempString.append("\n");
                }
            }
        //}
        //Summoner is not in game
        else{
            tempString.append("**").append(properName).append("** is not in game on the **")
                    .append(REGION_MAP.get(REGIONABB_MAP.get(region))).append("** server");
        }
        return tempString.toString();
    }
}
