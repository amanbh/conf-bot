/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package confbot;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;

/**
 *
 * @author Aman
 */
public class PresenceUpdater extends Thread {
    Roster roster;
    Chat[] relayTo;
    boolean[] available;
    private XMPPConnection connection;
    Main main;
    
    public PresenceUpdater(XMPPConnection connection, Main main) {
        this.connection = connection;
        this.roster = connection.getRoster();
        
        int noEntries = roster.getEntryCount();
        this.relayTo = new Chat[noEntries];
        this.available = new boolean[noEntries];
        
        this.main = main;
    }
    
    @Override
    public void run() {
        try {
            PresenceUpdater.sleep(5*1000);
            while (!this.main.quit) {
                //DEBUG main.debug_log("PresenceUpdater: Updating availability..");
                int noEntries = roster.getEntryCount();
                Collection<RosterEntry> entries = roster.getEntries();
                relayTo = new Chat[noEntries];
                available = new boolean[noEntries];
                int x = 0, j =0;
                for (RosterEntry entry : entries) {
                    // DEBUG main.debug_log(entry + ": " + roster.getPresence(entry.getUser()).toString());
                    if (roster.getPresence(entry.getUser()).isAvailable() ) {
                        available[x++] = true;
                    }
                    else {
                        available[x++] = false;
                    }
                    relayTo[j++] =  connection.getChatManager().createChat(entry.getUser(), new EchoMessageListener(this.main));
                }
                synchronized (this.main.relayTo) {
                    synchronized (this.main.available) {
                        this.main.relayTo = this.relayTo;
                        this.main.available = this.available;
                    }
                }
                // DEBUG main.debug_log("PresenceUpdater: Done updating!");
                PresenceUpdater.sleep(1000);
                if (!this.main.running) PresenceUpdater.sleep(1000);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(PresenceUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
}
