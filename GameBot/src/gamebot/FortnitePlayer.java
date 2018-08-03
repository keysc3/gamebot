package gamebot;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author colin
 */
public class FortnitePlayer {
    private final String name;
    private final String platform;
    
    private String lifetimeGp = "0";
    private String lifetimeWins = "0";
    private String lifetimeWp = "0";
    private String lifetimeKills = "0";
    private String lifetimeKd = "0";
    
    private String soloLifetimeGp = "0";
    private String soloLifetimeWins = "0";
    private String soloLifetimeWp = "0";
    private String soloLifetimeKills = "0";
    private String soloLifetimeKd = "0";
    
    private String duoLifetimeGp = "0";
    private String duoLifetimeWins = "0";
    private String duoLifetimeWp = "0";
    private String duoLifetimeKills = "0";
    private String duoLifetimeKd = "0";
    
    private String squadLifetimeGp = "0";
    private String squadLifetimeWins = "0";
    private String squadLifetimeWp = "0";
    private String squadLifetimeKills = "0";
    private String squadLifetimeKd = "0";
    private final String Md = "**";
    
    public FortnitePlayer(String nm, String pForm){
        name = nm;
        platform = pForm;
    }
    //Lifetime setters
    //Games Played
    public void setLifetimeGp(String gamesPlayed, String category){
        gamesPlayed = gamesPlayed.replace(",", "");
        switch(category){
            case "totalLifetime":
                lifetimeGp = gamesPlayed;
                break;
            case "soloLifetime":
                soloLifetimeGp = gamesPlayed;
                break;
            case "duoLifetime":
                duoLifetimeGp = gamesPlayed;
                break;
            default:
                squadLifetimeGp = gamesPlayed;
                
        }
    }
    //Wins
    public void setLifetimeWins(String gamesWon, String category){
        gamesWon = gamesWon.replace(",", "");
        switch(category){
            case "totalLifetime":
                lifetimeWins = gamesWon;
                break;
            case "soloLifetime":
                soloLifetimeWins = gamesWon;
                break;
            case "duoLifetime":
                duoLifetimeWins = gamesWon;
                break;
            default:
                squadLifetimeWins = gamesWon;
                
        }
    }
    //Win Percent
    public void setLifetimeWp(String winPercent, String category){
        switch(category){
            case "totalLifetime":
                lifetimeWp = winPercent;
                break;
            case "soloLifetime":
                soloLifetimeWp = winPercent;
                break;
            case "duoLifetime":
                duoLifetimeWp = winPercent;
                break;
            default:
                squadLifetimeWp = winPercent;
                
        }
    }
    //Kills
    public void setLifetimeKills(String kills, String category){
        kills = kills.replace(",", "");
        switch(category){
            case "totalLifetime":
                lifetimeKills = kills;
                break;
            case "soloLifetime":
                soloLifetimeKills = kills;
                break;
            case "duoLifetime":
                duoLifetimeKills = kills;
                break;
            default:
                squadLifetimeKills = kills;
                
        }
    }
    //Kill death ratio
    public void setLifetimeKd(String killDeath, String category){
        switch(category){
            case "totalLifetime":
                lifetimeKd = killDeath;
                break;
            case "soloLifetime":
                soloLifetimeKd = killDeath;
                break;
            case "duoLifetime":
                duoLifetimeKd = killDeath;
                break;
            default:
                squadLifetimeKd = killDeath;
                
        }
    }
    //Lifetime getters
    //Name
    public String getPlayerName(){
        return name;
    }
    //Platform
    public String getPlatform(){
        return platform;
    }
    //Games played
    public String getLifetimeGp(String category){
        switch(category){
            case "totalLifetime":
                return lifetimeGp;
            case "soloLifetime":
                return soloLifetimeGp;
            case "duoLifetime":
                return duoLifetimeGp;
            default:
                return squadLifetimeGp;    
        }
    }
    //Wins
    public String getLifetimeWins(String category){
        switch(category){
            case "totalLifetime":
                return lifetimeWins;
            case "soloLifetime":
                return soloLifetimeWins;
            case "duoLifetime":
                return duoLifetimeWins;
            default:
                return squadLifetimeWins;      
        }
    }
    //Win Percent
    public String getLifetimeWp(String category){
        switch(category){
            case "totalLifetime":
                return lifetimeWp;
            case "soloLifetime":
                return soloLifetimeWp;
            case "duoLifetime":
                return duoLifetimeWp;
            default:
                return squadLifetimeWp;      
        }
    }
    //Kills
    public String getLifetimeKills(String category){
        switch(category){
            case "totalLifetime":
                return lifetimeKills;
            case "soloLifetime":
                return soloLifetimeKills;
            case "duoLifetime":
                return duoLifetimeKills;
            default:
                return squadLifetimeKills;      
        }
    }
    //Kill death
    public String getLifetimeKd(String category){
        switch(category){
            case "totalLifetime":
                return lifetimeKd;
            case "soloLifetime":
                return soloLifetimeKd;
            case "duoLifetime":
                return duoLifetimeKd;
            default:
                return squadLifetimeKd;      
        }
    }
    //Comparers
    //Games Played
    public void compareLifetimeGp(FortnitePlayer otherPlayer, String category){
        String otherGp = otherPlayer.getLifetimeGp(category);
        String thisGp = this.getLifetimeGp(category);
        if(Integer.parseInt(thisGp) > Integer.parseInt(otherGp))
            this.setLifetimeGp(Md + thisGp + Md, category);
        else
            otherPlayer.setLifetimeGp(Md + otherGp + Md, category);
    }
    
    public void compareLifetimeWins(FortnitePlayer otherPlayer, String category){
        String otherWins = otherPlayer.getLifetimeWins(category);
        String thisWins = this.getLifetimeWins(category);
        if(Integer.parseInt(thisWins) > Integer.parseInt(otherWins))
            this.setLifetimeWins(Md + thisWins + Md, category);
        else
            otherPlayer.setLifetimeWins(Md + otherWins + Md, category);
    }
    
    public void compareLifetimeWp(FortnitePlayer otherPlayer, String category){
        String otherWp = otherPlayer.getLifetimeWp(category);
        String thisWp = this.getLifetimeWp(category);
        if(Float.parseFloat(thisWp.replace("%", "")) > Float.parseFloat(otherWp.replace("%", "")))
            this.setLifetimeWp(Md + thisWp + Md, category);
        else
            otherPlayer.setLifetimeWp(Md + otherWp + Md, category);
    }
    
    public void compareLifetimeKills(FortnitePlayer otherPlayer, String category){
        String otherKills = otherPlayer.getLifetimeKills(category);
        String thisKills = this.getLifetimeKills(category);
        if(Integer.parseInt(thisKills) > Integer.parseInt(otherKills))
            this.setLifetimeKills(Md + thisKills + Md, category);
        else
            otherPlayer.setLifetimeKills(Md + otherKills + Md, category);
    }
    
    public void compareLifetimeKd(FortnitePlayer otherPlayer, String category){
        String otherKd = otherPlayer.getLifetimeKd(category);
        String thisKd = this.getLifetimeKd(category);
        if(Float.parseFloat(thisKd) > Float.parseFloat(otherKd))
            this.setLifetimeKd(Md + thisKd + Md, category);
        else
            otherPlayer.setLifetimeKd(Md + otherKd + Md, category);
    }
    
    public void compareAllLifetime(FortnitePlayer otherPlayer, String category){
        this.compareLifetimeGp(otherPlayer, category);
        this.compareLifetimeWins(otherPlayer, category);
        this.compareLifetimeWp(otherPlayer, category);
        this.compareLifetimeKills(otherPlayer, category);
        this.compareLifetimeKd(otherPlayer, category);
    }
}
