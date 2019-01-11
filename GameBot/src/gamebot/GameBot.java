package gamebot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

/**
 * Purpose: Implements a discord bot that has basic commands as well as commands
 * to check user statistics for different games through their respective API's using
 * the JDA discord library.
 * 
 * @author Colin Keys
 * 
 * Variables            Description
 * 
 */
public class GameBot {
    
    public static final Properties config = new Properties();
    /**
     * Purpose: Start and configure the discord bot
     * @param args the command line arguments
     * @throws net.dv8tion.jda.core.exceptions.RateLimitedException
     */
    public static void main(String[] args) throws RateLimitedException {
        //Try and catch for exceptions
        try{
            InputStream input = new FileInputStream("src/props/gamebot-config.properties");
            config.load(input);
            //Start the bot, set it to my bots token, attach wanted listeners.
            JDA api = new JDABuilder(AccountType.BOT)
                    .setToken(config.getProperty("botToken"))
                    .addEventListener(new MyListener())
                    .addEventListener(new LeagueListener())
                    .addEventListener(new FortniteListener())
                    .buildBlocking();
            System.out.println("I'm Online!\nI'm Online!");
        }
        catch (LoginException | InterruptedException e){
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GameBot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GameBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
