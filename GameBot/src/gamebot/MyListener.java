package gamebot;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

/**
 * Purpose: This class implements a ListenerAdapter for a discord bot. It checks
 * for all the non-game related commands and outputs a command specific 
 * message to the channel it received it in.
 * 
 * @author Colin Keys
 * 
 * Variables            Description
 * 
 * private
 * 
 * dbOps                    DataboseOps object for database operations
 * 
 */
public class MyListener extends ListenerAdapter {
    private final DatabaseOps dbOps = new DatabaseOps();
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        try {
            //Get the readable contents of the message
            String message = event.getMessage().getContentDisplay();
            //Dont respond to other bots, this bot, and only handle basic commands
            if (event.getAuthor().isBot() || !message.startsWith(GameBot.config.getProperty("prefix"))
                    || message.startsWith(GameBot.config.getProperty("prefix") + "lol")
                    || message.startsWith(GameBot.config.getProperty("prefix") + "fn")) return;
            //Split the args on a space to get them all
            ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
            //Get the command without the prefix
            String command = args.get(0).substring(1);
            //Remove the command from the argument list
            args.remove(0);
            //Geth the amount of arguments given
            int numArgs = args.size();
            StringBuilder outputString = new StringBuilder();
            //Switch used to process the command given
            switch(command){
                //Outputs all the basic commands and help commands for other listeners
                case "help":
                    outputString.setLength(0);
                    outputString.append("__**Commands**__\n");
                    outputString.append("**!ping:** responds with pong!\n");
                    outputString.append("**!buddy:** responds with guy!\n");
                    outputString.append("**!roll [number]:** Randomly gets a value "
                            + "up to the selected ***number*** (Max 9 digits | default is 100)\n");
                    outputString.append("**!gimme <noun>:** Inputs ***item*** and "
                            + "***noun*** into a static sentence\n");
                    outputString.append("**!reverse <sentence>:** Responds with the given ***sentence*** reversed\n");
                    outputString.append("**!lolHelp:** Outputs info about the available League of Legends commands\n");
                    outputString.append("**!fnHelp:** Outputs info about the available Fortnite commands\n");
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "help");
                    break;
                    //Outputs pong!
                case "ping":
                    //Send message in channel it was received
                    event.getChannel().sendMessage("pong!").queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "ping");
                    break;
                    //Outputs guy!
                case "buddy":
                    //Send message in channel it was received
                    event.getChannel().sendMessage("guy!").queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "buddy");
                    break;
                    //Outputs a random number between 1-100, or 1-given number
                case "roll":
                    //Set max value and output string
                    int max = 100;
                    outputString.setLength(0);
                    //if a value is given change max value to that
                    if(numArgs >= 1){
                        String maxRange = args.get(0);
                        //Incase they type a negative number
                        maxRange = maxRange.replace("-", "");
                        //Have a max number
                        if(StringUtils.isNumericSpace(maxRange) && maxRange.length() < 10)
                            max = Integer.parseInt(maxRange);
                    }
                    //Pick random number between 1 (inclusive) and max (exclusive)
                    String randomNum = String.valueOf(ThreadLocalRandom.current().nextInt(1, max + 1));
                    outputString.append(event.getAuthor().getName()).append(" rolls ").append(randomNum);
                    //Send message in channel it was received
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "roll");
                    break;
                    //Outputs a siple sentence with the given argument
                case "gimme":
                    //Must have at least 1 args
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !gimme <noun>**").queue();
                        break;
                    }
                    //Build output
                    outputString.setLength(0);
                    //Join the arguments
                    outputString.append("Gimme dat ").append(StringUtils.join(args, ' ')).append("").append("!");
                    //Send message in channel it was received
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "gimme");
                    break;
                    //Outputs the given argument backwards
                case "reverse":
                    //Must have something to reverse
                    if(numArgs < 1){
                        event.getChannel().sendMessage("**Usage: !reverse <sentence>**").queue();
                        break;
                    }
                    //Join the split args into a sentence with spaces
                    String sentence = StringUtils.join(args, ' ');
                    //Set up output string, size and stack
                    outputString.setLength(0);
                    Stack back = new Stack();
                    int size;
                    //Add all letters to the stack
                    for(int i = 0; i < sentence.length(); i++){
                        back.add(sentence.charAt(i));
                    }
                    //Stack size, must be grabbed here or size will be dynamic
                    size = back.size();
                    //Pop all letters off the stack of the sentence to reverse it
                    for(int i = 0; i < size; i++){
                        outputString.append((back.pop()));
                    }
                    //Send message in channel it was received
                    event.getChannel().sendMessage(outputString.toString()).queue();
                    //Add use to db
                    dbOps.dbUpdate(event, "reverse");
                    break;
            }
        } catch (SQLException | IllegalAccessException ex) {
            Logger.getLogger(MyListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
