/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gamebot;

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
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Keeeeys
 */
public class FortniteListener extends ListenerAdapter {
    private final StringBuilder outputString = new StringBuilder();
    
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
        
        //Switch used to process the command given
        switch(command){
            //Outputs all the League of Legends related commands
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
        {
            try {
                makeRequest("pc", args.get(0));
            } catch (ProtocolException ex) {
                Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FortniteListener.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
                break;
        }
    }
    
    private void makeRequest(String platform, String epicName) throws MalformedURLException, ProtocolException, IOException{
        //StringBuilder result = new StringBuilder();
        String urlString = "https://api.fortnitetracker.com/v1/profile/" + platform + "/" + epicName;
        URL url = new URL(urlString);
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
        System.out.println(obj.getJSONObject("stats").getJSONObject("p2").getJSONObject("kd").getString("displayValue"));
        String test = obj.getString("accountId");
        //JSONObject soloOverall = stats.getJSONObject("p2");
        //String score = soloOverall.getJSONObject("score").getString("displayValue");
        //print in String
        System.out.println(test);
        
    }
}
