package gamebot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
/**
 * Purpose: Implements methods to connect to a local database and update/insert
 * specific data.
 * 
 * @author Colin Keys
 * 
 * Variables            Description
 * 
 */
public class DatabaseOps {
    
    /**
     * getConnection - Opens a connection to a local database
     * @return Connection - A connection to the database 
     */
    public static Connection getConnection() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException{
        Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
        String dbUrl = "jdbc:derby://localhost:1527/GameBotStats";
        return DriverManager.getConnection(dbUrl, GameBot.config.getProperty("serverUser"), GameBot.config.getProperty("serverPass"));
    }
    
    /**
     * dbUpdate - Updates the database by incrementing a users command call count, 
     * while also adding a new entry if necessary
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     * @param command - command called by the user
     */
    public void dbUpdate(MessageReceivedEvent event, String command) throws SQLException, IllegalAccessException{
        try {
            Connection conn = DatabaseOps.getConnection();
            PreparedStatement prepstate;
            //If user isnt in the database add them
            if(!checkExists(event, conn)){
                //Get number of entrys so new entry can have proper id
                prepstate = conn.prepareStatement("select count(*) from APP.USERS");
                ResultSet rs = prepstate.executeQuery();
                int count = 0;
                while(rs.next()){
                    count = rs.getInt(1);
                }
                //Add new entry and start the command they used at 1
                prepstate = conn.prepareStatement("insert into APP.USERS (user_id, discordName, discrim, " + command + ") values (?, ?, ?, ?)");
                prepstate.setInt(1, count+1);
                prepstate.setString(2, event.getAuthor().getName());
                prepstate.setString(3, event.getAuthor().getDiscriminator());
                prepstate.setInt(4, 1);
                prepstate.executeUpdate();
                
            }
            else{
                //Update the used command by one
                String strang = "update APP.USERS set " + command + " = " + command + " + 1 where discordName = ? and discrim = ?";
                prepstate = conn.prepareStatement(strang);
                prepstate.setString(1, event.getAuthor().getName());
                prepstate.setString(2, event.getAuthor().getDiscriminator());
                prepstate.executeUpdate();
            }
            conn.close();
        } catch (ClassNotFoundException | InstantiationException ex) {
            Logger.getLogger(MyListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * dbUpdate - Checks to see if a user is already an entry in the database
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     * @param conn- connection to the wanted database
     */
    public Boolean checkExists(MessageReceivedEvent event, Connection conn) throws SQLException{
        //Select the user that sent the message to see if they exist
        PreparedStatement prepstate = conn.prepareStatement("select discordName, discrim from APP.USERS where discordName = ? and discrim = ?");
        prepstate.setString(1, event.getAuthor().getName());
        prepstate.setString(2, event.getAuthor().getDiscriminator());
        ResultSet rs = prepstate.executeQuery();
        //If select found something this will return true.
        return rs.next();
    }
}
