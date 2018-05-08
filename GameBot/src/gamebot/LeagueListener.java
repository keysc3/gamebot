package gamebot;

import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMasteries;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMastery;
import com.merakianalytics.orianna.types.core.league.LeaguePosition;
import com.merakianalytics.orianna.types.core.league.LeaguePositions;
import com.merakianalytics.orianna.types.core.spectator.CurrentMatch;
import com.merakianalytics.orianna.types.core.spectator.Player;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
 * 
 * private
 * 
 * queueWins                integer - wins in the given queue
 * queueLosses              integer - losses in the given queue
 * queuePercent             integer - Win/Loss percent in the given queue
 * summonerName             String - name of the given summoner
 * summoner                 Summoner - summoner object of given summonerName
 * outputString             StringBuilder - used to format output
 * 
 */
public class LeagueListener extends ListenerAdapter{
    
    private static final Map<String, String> REGION_MAP = createRegionMap();
    private static final Map<String, String> REGIONABB_MAP = createRegionAbbMap();
    private static final Map<String, String> QUEUE_MAP = createQueueMap();
    private static final Map<String, String> TIER_MAP = createTierMap();
    private static final Map<String, String> TEAM_MAP = createTeamMap();
    private int queueWins;
    private int queueLosses;
    private int queuePercent;

    private String summonerName;
    private Summoner summoner;
    private final StringBuilder outputString = new StringBuilder();
    
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
                summoner(args, "NA", event);
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
                summoner(args, regionGiven, event);
                break;
            //Outputs info about the Leagues the given summoner is ranked in on the NA server
            case "lolRanks":
                //Must have a summoner to search for
                if(numArgs < 1){
                   event.getChannel().sendMessage("**Usage: !lolRanks <Summoner_Name>**").queue();
                   break;
                }
                //Process the summoner
                summonerRanks(args, "NA", event);
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
                summonerRanks(args, regionGiven, event);
                break;
            //Outputs info about the current game the given summoner is in
            case "lolCurrentGame":
                //Must have a summoner to search for
                if(numArgs < 1){
                   event.getChannel().sendMessage("**Usage: !lolCurrentGame <Summoner_Name>**\n").queue();
                   break;
                }
                //Process the summoner
                summonerCurrentGame(args, "NA", event);
                break;
                
            /*
            case "summonerQueueRank":
                //Must have a summoner to search for
                if(numArgs < 1){
                   event.getChannel().sendMessage("**Usage: !summonerRanks <Summoner_Name>**").queue();
                   break;
                }
                //Set up output string, Orianna, and summoner name
                setUpOrianna();
                outputString.setLength(0);
                //Build proper name
                summonerName = getSummonerName(args);
                //Get the summoner
                summoner = getSummoner(summonerName);
                //Get position summoner is in for the leagues they are ranked in
                final LeaguePosition position1 = summoner.getLeaguePosition(Queue.RANKED_SOLO_5x5);
                //Make sure the summoner is ranked
                if(position1 != null){
                    //Get the summoners league name, tier, division, and LP
                    int rankedFiveWins = position1.getWins();
                    int rankedFiveLosses = position1.getLosses();
                    int rankedFivesPercent = (rankedFiveWins * 100)/(rankedFiveWins+rankedFiveLosses);

                    outputString.append("**Solo/Duo League:** ").append(position1.getName()).append("\n");
                    outputString.append("**Tier:** ").append(tierMap.get(position1.getTier().toString())).append("\n");
                    outputString.append("**Division:** ").append(position1.getDivision().toString()).append("\n");
                    outputString.append("**LP:** ").append(String.valueOf(position1.getLeaguePoints())).append("\n");
                    outputString.append("**Win/Loss:** ").append(String.valueOf(rankedFiveWins))
                            .append("/").append(String.valueOf(rankedFiveLosses))
                            .append(" (").append(String.valueOf(rankedFivesPercent))
                            .append("%)\n");
                    if(position1.getPromos() != null) {
                        // If the summoner is in their promos show progress
                        outputString.append("**Promos progress: **")
                                .append(position1.getPromos().getProgess().replace('N', '-'))
                                .append("\n");
                    }
                }
                //Summoner is not ranked
                else{
                    outputString.append("**Solo/Duo League:** Summoner is not ranked in the Solo/Duo 5v5 queue\n");
                }*/
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
     * @return tierMap - HashMap of ENUM Queue names to user friendly tier names
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
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     */
    private void summonerRanks(ArrayList args, String region, MessageReceivedEvent event){
        //Reset output string, set up Orianna, and summoner name
        setUpOrianna();
        outputString.setLength(0);
        //Build proper name
        summonerName = getSummonerName(args);
        //Get the summoner, NA if no region is given
        if(region.equals("NA"))
            summoner = getSummoner(summonerName);
        else{
            summoner = getSummonerRegionSpecific(summonerName, REGIONABB_MAP.get(region));
        }
        
        //Make sure summoner exists
        if(!checkSummonerExists(summoner)){
            summonerDoesNotExist(summonerName, event, REGIONABB_MAP.get(region));
            return;
        }
        
        //Get the positions summoner is in for the leagues they are ranked in
        final LeaguePositions positions = summoner.getLeaguePositions();
        //If they are not ranked in any leagues output results and return
        if(positions.isEmpty()){
            outputString.append("**").append(summonerName).append("** is not ranked in any leagues!");
            event.getChannel().sendMessage(outputString.toString()).queue();
            return;
        }
        
        outputString.append("__**Leagues ").append(summonerName).append(" is ranked in:**__\n");
        //Go through each league and get the desired data from each one
        for(final LeaguePosition leaguePosition : positions) {
            //Queue name, tier name, division number, LP amount, W/L, and W/L ratio
            outputString.append("**").append(QUEUE_MAP.get(leaguePosition.getQueue().toString())).append(":** ");
            outputString.append(TIER_MAP.get(leaguePosition.getTier().toString())).append(" ");
            outputString.append(leaguePosition.getDivision().toString()).append(" ");
            outputString.append(String.valueOf(leaguePosition.getLeaguePoints())).append("LP ");
            queueWins = leaguePosition.getWins();
            queueLosses = leaguePosition.getLosses();
            queuePercent = (queueWins * 100)/(queueWins+queueLosses);
            outputString.append("(W/L: ").append(String.valueOf(queueWins))
                    .append("/").append(String.valueOf(queueLosses))
                    .append(" ").append(String.valueOf(queuePercent))
                    .append("%)\n");
        }
        //Send message in channel it was received in
        event.getChannel().sendMessage(outputString.toString()).queue();
    }
    
    /**
     * summonerRanks - Outputs info about the given summoner on the NA server 
     * by default, otherwise the given server.
     * @param args - ArrayList of the given summoner name split on spaces
     * @param region - String of the given abbreviated region to search on
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     */
    private void summoner(ArrayList args, String region, MessageReceivedEvent event){
        //Reset output string, set up Orianna, and summoner name
        setUpOrianna();
        outputString.setLength(0);
        //Build proper name
        summonerName = getSummonerName(args);
        //Get the summoner, NA if no region is given
        if(region.equals("NA"))
            summoner = getSummoner(summonerName);
        else{
            summoner = getSummonerRegionSpecific(summonerName, REGIONABB_MAP.get(region));
        }

        //Make sure summoner exists
        System.out.println(checkSummonerExists(summoner));
        if(!checkSummonerExists(summoner)){
            summonerDoesNotExist(summonerName, event, REGIONABB_MAP.get(region));
            return;
        }

        //Build summoner level and region output
        outputString.append("__**").append(summoner.getName()).append("**__").append("\n");
        outputString.append("**Level:** ").append(summoner.getLevel()).append("\n");
        outputString.append("**Region:** ").append(REGION_MAP.get(summoner.getRegion().toString())).append("\n");

        outputString.append("\n__**Top 3 Champs By Mastery:**__\n");
        //Get the champion mastery stats on all champs they have a point on
        ChampionMasteries champMasts = summoner.getChampionMasteries();
        //Only want to display their top three, which are the first three
        for(int i = 0; i < 3; i++){
            //Get the champ
            ChampionMastery singleChampMast = champMasts.get(i);
            //Build the Rank, champion name, and champion points output
            outputString.append(String.valueOf(i+1)).append(". ")
                    .append(singleChampMast.getChampion().getName()).append(" - ")
                    .append(String.valueOf(singleChampMast.getPoints()))
                    .append(" pts\n");
        }
        System.out.println(summoner.isInGame());
        //Send message in channel it was received in
        event.getChannel().sendMessage(outputString.toString()).queue();
    }
    
    /**
     * summonerCurrentGame - Outputs info about the current game the given summoner
     * is in on the NA server
     * @param args - ArrayList of the given summoner name split on spaces
     * @param region - String of the given abbreviated region to search on
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     */
    @SuppressWarnings("empty-statement")
    private void summonerCurrentGame(ArrayList args, String region, MessageReceivedEvent event){
        //Reset output string, set up Orianna, and summoner name
        setUpOrianna();
        outputString.setLength(0);
        //Build proper name
        summonerName = getSummonerName(args);
        //Get the summoner, NA if no region is given
        if(region.equals("NA"))
            summoner = getSummoner(summonerName);
        else{
            summoner = getSummonerRegionSpecific(summonerName, REGIONABB_MAP.get(region));
        }
        
        //Make sure summoner exists
        if(!checkSummonerExists(summoner)){
            summonerDoesNotExist(summonerName, event, REGIONABB_MAP.get(region));
            return;
        }
        
        //Get the positions summoner is in for the leagues they are ranked in
        final CurrentMatch currentGame = summoner.getCurrentMatch();
        //summoner.isInGame does not work, using try and catch to check if they are in game
        try{
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
            
            outputString.append("**").append(summonerName).append("** is in a **")
                    .append(queueName).append("** game!\n")
                    .append("**Server:** ").append(REGION_MAP.get(REGIONABB_MAP.get(region))).append("\n")
                    .append("**Champion: **").append(player.getChampion().getName()).append("\n")
                    .append("**Duration: **").append(gameDuration).append("\n")
                    .append("**Team Side: **").append(TEAM_MAP.get(player.getTeam().getSide().name())).append("\n");
            if(queueName.equals("Solo/Duo 5v5") ||queueName.equals("Summoners Rift Draft 5v5")){
                outputString.append("__**Bans:**__\n**Blue:** ");
                //Get all blue teams bans and output them
                currentGame.getBlueTeam().getBans().forEach((blueBan) -> {
                    outputString.append(blueBan.getName()).append(" | ");
                });
                outputString.append("\n**Red:** ");
                //Get all blue teams bans and output them
                currentGame.getRedTeam().getBans().forEach((redBan) -> {
                    outputString.append(redBan.getName()).append(" | ");
                });
            }
            //Send message in channel it was received in
            event.getChannel().sendMessage(outputString.toString()).queue();
        }
        catch(NullPointerException e){
            outputString.append("**").append(summonerName).append("** is not in game on the **")
                    .append(REGION_MAP.get(REGIONABB_MAP.get(region))).append("** server");
            event.getChannel().sendMessage(outputString.toString()).queue();
        }
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
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     */
    private void summonerDoesNotExist(String summonerName, MessageReceivedEvent event, String region){
        outputString.append("**").append(summonerName).append("** does not exist on the **")
                .append(REGION_MAP.get(region)).append("** server");
        event.getChannel().sendMessage(outputString.toString()).queue();
    }
}
