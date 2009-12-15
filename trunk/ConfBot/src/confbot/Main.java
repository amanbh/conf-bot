/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package confbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;

/**
 * Main Class for ConfBot Application.
 * @author Aman
 */
public class Main implements Runnable {

    String[] args;
    String topic = "";
    XMPPConnection connection = null;
    boolean[] available = null;
    Chat[] relayTo = null;
    Roster roster = null;
    Hashtable usernames = null;
    Hashtable userID = null;
    Hashtable dndList = null;
    Hashtable banList = null;
    String mode = "default";
    String poll = "";
    boolean pollRunning = false;
    Hashtable pollresult = null;
    boolean running = true;
    boolean quit = false;
    AnagramThread anagramThread;
    Hashtable anagramChats = new Hashtable();
    Hashtable scores = new Hashtable();

    ArrayList<String> messageLog;
    static final int MAX_MESSAGE_LOG_SIZE = 60;
    static final String VERSION = "0.2.1";
    
    String BOTUSERNAME;
    String BOTPASSWORD;
    String ADMINPASS;
    String SERVERADDRESS;
    static final String DEFAULT_SERVERADDRESS = "talk.google.com";
    String SERVERPORT;
    static final String DEFAULT_SERVERPORT = "5222";
    String PROXY_SERVERADDRESS;
    String PROXY_SERVERPORT;
    String PROXY_USER;
    String PROXY_PASS;
    private FileWriter fstream;
    private BufferedWriter out;
    Launcher launcher;
                                                                                                                    private static String specialWords[] = {"fuck", "sex", "rape", "ass", "asshole", "fcuk", "slut", "chutiye", "chutiya", "madar", "boob", "porn", "choot", "behenchod", "incest"};
    static final String DEFAULT_PRESENCE = "Running a gtalkbot! *Now with anagrams! Start the game by \'/anagarms\'. Try them now!* Type your message and it will be forwarded to everyone. For help type \"/help\"";

//    static boolean SHOULD_ACCPET_FILES = true;
//    static FileTransferManager manager;
    /**
     * The main() method for ConfBot Creates a new Main object with specified arguments.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Main(args);
    }

   /**
    * Constructor for Main class.
    * Starts a new thread for this Main object.
    * @param args the commandline arguments passed to the constructor
    */
    public Main(String[] args) {
        this.args = args;
        Thread t = new Thread(this);
        t.start();
    }

   /**
    * Run method for Main class.
    */
    public void run() {
        try {
            if (args.length < 5) {
                System.out.println("Missing arguments.");
                try {
                    BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

                    System.out.println("Username for Bot (including @gmail.com): ");
                    BOTUSERNAME = console.readLine();

                    System.out.println("Password for Bot: ");
                    BOTPASSWORD = console.readLine();

                    System.out.println("Choose an admin password: ");
                    ADMINPASS = console.readLine();
                    System.out.println("Repeat admin password: ");
                    if (!console.readLine().equals(ADMINPASS)) {
                        System.out.println("ERROR: Admin Passwords don't match! \nExiting now.");
                        return;
                    }

                    System.out.println("Server Address to connect to: (leave blank for default server)");
                    String temp_response = console.readLine();
                    if (temp_response.length() == 0 ) {
                        SERVERADDRESS = Main.DEFAULT_SERVERADDRESS;
                        SERVERPORT = Main.DEFAULT_SERVERPORT;
                    } else {
                        SERVERADDRESS = temp_response;
                        System.out.println("Server Port to connect to: (leave blank for default port)");
                        temp_response = console.readLine();
                        if (temp_response.length() == 0) {
                            SERVERPORT = Main.DEFAULT_SERVERPORT;
                        } else {
                            SERVERPORT = temp_response;
                        }

                    }
                    
                    System.out.println("Proxy Server Address: (leave blank for no proxy)");
                    temp_response = console.readLine();
                    if (temp_response.length() == 0) {
                        PROXY_SERVERADDRESS = "";
                        PROXY_SERVERPORT = "";
                        PROXY_USER = "";
                        PROXY_PASS = "";
                    } else {
                        PROXY_SERVERADDRESS = temp_response;
                        System.out.println("Proxy Server Port: ");
                        PROXY_SERVERPORT = console.readLine();
                        System.out.println("Username for Proxy: ");
                        PROXY_USER = console.readLine();
                        System.out.println("Password for Proxy: ");
                        PROXY_PASS = console.readLine();
                    }
                } catch (IOException ex) {
                    error_log(ex.toString());
                    error_log(ex.getStackTrace().toString());
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            } else {
                BOTUSERNAME = args[0];
                BOTPASSWORD = args[1];
                ADMINPASS = args[2];
                SERVERADDRESS = args[3];
                SERVERPORT = args[4];

                if (args.length >= 9) {
                    PROXY_SERVERADDRESS = args[5];
                    PROXY_SERVERPORT = args[6];
                    PROXY_USER = args[7];
                    PROXY_PASS = args[8];
                }

            }

            // Open Log File Writer
            try {
                fstream = new FileWriter(BOTUSERNAME + ".log");
                out = new BufferedWriter(fstream);
                System.out.println("DEBUG ----- LOG FILE READY");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            initializeConnection();
            login();
            setPresence(Presence.Type.available, DEFAULT_PRESENCE);
            // create new Packet Listener - unused?
            PacketListener echoPacket = new PacketListener() {

                public void processPacket(Packet packet) {
                    if (packet instanceof Message) {
                        Message message = (Message) packet;
                        // print message body
                        debug_log("Received Message as Packet: " + message.getBody());
                    }
                }
            };
            getRoster();
            Thread.sleep(2000);

            this.messageLog = new ArrayList<String>();
            this.messageLog.ensureCapacity(Main.MAX_MESSAGE_LOG_SIZE / 2);

            this.getSettings();

            debug_log("Main: Printing Roster\n*********************");
            int noEntries = roster.getEntryCount();
            Collection<RosterEntry> entries = roster.getEntries();
            relayTo = new Chat[noEntries];
            available = new boolean[noEntries];
            int x = 0;
            int j = 0;
            for (RosterEntry entry : entries) {
                debug_log("#" + entry.getType());
                debug_log(entry + ": " + roster.getPresence(entry.getUser()).toString());
                if (roster.getPresence(entry.getUser()).isAvailable()) {
                    available[x++] = true;
                } else {
                    available[x++] = false;
                }
                relayTo[j++] = connection.getChatManager().createChat(entry.getUser(), new EchoMessageListener(this));
            }
            debug_log("*********************");

            // TODO check if needed - connection.addPacketListener(echoPacket, (PacketFilter)(new AndFilter( new PacketTypeFilter(Message.class) ) ) );
            //DEBUG Chat chat = connection.getChatManager().createChat("amanatiit@gmail.com", echoMessage );
            //DEBUG chat.sendMessage("lava heloo!");
            //sendToAll("A Conference Bot is up and running. Send a message to this bot to relay to all. Try it now!");
            PresenceUpdater pUpdater = new PresenceUpdater(connection, this);
            pUpdater.start();

            this.setFileTransferManager();

            anagramThread = new AnagramThread(this);
            anagramThread.start();


            while (!this.quit) {
                Thread.sleep(30 * 1000);
                this.saveSettings();
                // DEBUG if (i%5000==0) chat.sendMessage( i + " message.");
            }
            debug_log("Exiting bot...");
            connection.disconnect();
            pUpdater.join();
            debug_log("Bye! Have a good day.");

            // Close debug log file
            try {
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // System.exit(0);
        } catch (XMPPException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

    }

    /* Method to format the message body according to the content mode */
    String getFormattedMessage(String body) {
        if (this.mode.equals("clean")) {
            String split[] = body.split(" ", 100);
            for (int i = 0; i < split.length; i++) {
                for (int j = 0; j < Main.specialWords.length; j++) {
                    if (split[i].startsWith(Main.specialWords[j])) {
                        body = body.replaceAll(Main.specialWords[j], Main.getMask(Main.specialWords[j]));
                        j = Main.specialWords.length; // to continue with next i
                    }
                }
            }
        } else if (this.mode.equals("dirty")) {
            String split[] = body.split(" ", 100);
            for (int i = 0; i < split.length; i++) {
                for (int j = 0; j < Main.specialWords.length; j++) {
                    if (split[i].startsWith(Main.specialWords[j])) {
                        body = body.replaceAll(split[i], "*" + split[i] + "*");
                        j = Main.specialWords.length; // to continue with next i
                    }
                }
            }
        }
        return body;
    }

    /** Returns a Mask of length equal to length of string argument 
     *  Mask is %'s.
     * @param word
     * @return mask of length same as word
     */
    static String getMask(String word) {
        String retstr = "";
        for (int i = 0; i < word.length(); i++) {
            retstr = retstr.concat("%");
        }
        return retstr;
    }

    /* Method to get roster information */
    private int getRoster() {
        // print roster
        // debug_log("Main: Printing Roster\n*********************");
        roster = connection.getRoster();

        Collection<RosterEntry> entries = roster.getEntries();
        usernames = new Hashtable();
        userID = new Hashtable();
        dndList = new Hashtable();
        banList = new Hashtable();
        for (RosterEntry entry : entries) {
            // debug_log(entry.getName());
            if (entry.getName() != null) {
                usernames.put(entry.getUser(), entry.getName());
                userID.put(entry.getName().toLowerCase(), entry.getUser());
            }
            dndList.put(entry.getUser(), new Long(0));
            banList.put(entry.getUser(), new Long(0));
        }

        return roster.getEntryCount();

    }

    /* Method to initialize coonnection to XMPP server */
    private void initializeConnection() throws XMPPException {
        //ProxyInfo proxyInfo = new ProxyInfo(ProxyInfo.ProxyType.HTTP, "ernetproxy.iitk.ac.in", 3128, "amanbh", "{P;;osa");
        //ConnectionConfiguration connConfig = new ConnectionConfiguration("talk.google.com", 443, proxyInfo);

        //ConnectionConfiguration connConfig = new ConnectionConfiguration("talk.google.com", 443, "gmail.com");
        //ConnectionConfiguration connConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");

        String resource = "";
        if (SERVERADDRESS.equals("talk.google.com")) resource = "gmail.com";
        ConnectionConfiguration connConfig = new ConnectionConfiguration(SERVERADDRESS, Integer.parseInt(SERVERPORT), resource);
        
        if (PROXY_SERVERADDRESS !=null && !PROXY_SERVERADDRESS.equals("")) {
            connConfig.setSocketFactory(new SSLTunnelSocketFactory(PROXY_SERVERADDRESS, PROXY_SERVERPORT, this.PROXY_USER, this.PROXY_PASS, this));
        }

        // TURN ON DEBUGGER XMPPConnection.DEBUG_ENABLED = true;
        connection = new XMPPConnection(connConfig);
        debug_log("Establishing Connection...");

        connection.connect();

        debug_log("Connected Successfully!");

    }

    /** Method for logging into the server **/
    private void login() throws XMPPException {
        // http://www.igniterealtime.org/community/thread/35976?tstart=0
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);

        // Username and Password for Login : BOTUSERNAME, BOTPASSWORD
        connection.login(BOTUSERNAME, BOTPASSWORD, "resource");
        debug_log("Logged in.");
    }

    /** Method to restore saved settings **/
    private void getSettings() {
        try {
            // Read from disk using FileInputStream.
            FileInputStream f_in = new FileInputStream("dndUsers-" + this.BOTUSERNAME + ".data");
            // Read object using ObjectInputStream.
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            // Read an object.
            Object obj = obj_in.readObject();

            // Is the object that you read in, say, an instance
            // of the Vector class?
            if (obj instanceof Hashtable) {
                // Cast object to a Hashtable
                this.dndList = (Hashtable) obj;
                debug_log("Restored DND List \n" + this.dndList.toString());
            }
        } catch (Exception ex) {
            // ex.printStackTrace();
            error_log(ex.toString());
            error_log(ex.getStackTrace().toString());

        }

        try {
            FileInputStream f_in = new FileInputStream("bannedUsers-" + this.BOTUSERNAME + ".data");
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            Object obj = obj_in.readObject();

            if (obj instanceof Hashtable) {
                this.banList = (Hashtable) obj;
                debug_log("Restored BAN List \n" + this.banList.toString());
            }
        } catch (Exception ex) {
            error_log(ex.toString());
            error_log(ex.getStackTrace().toString());
        }

        try {
            FileInputStream f_in = new FileInputStream("scores-" + this.BOTUSERNAME + ".data");
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            Object obj = obj_in.readObject();

            if (obj instanceof Hashtable) {
                this.scores = (Hashtable) obj;
                debug_log("Restored SCORE List \n" + this.scores.toString());
            }
        } catch (Exception ex) {
            error_log(ex.toString());
            error_log(ex.getStackTrace().toString());
        }
    }

    /** Method to store settings **/
    private void saveSettings() {
        //debug_log("Saving settings ..");
        try {
            // Use a FileOutputStream to send data to a file called myobject.data.
            FileOutputStream f_out = new FileOutputStream("dndUsers-" + this.BOTUSERNAME + ".data");
            // Use an ObjectOutputStream to send object data to the FileOutputStream for writing to disk.
            ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
            // Pass our object to the ObjectOutputStream's writeObject() method to cause it to be written out to disk.
            obj_out.writeObject(this.dndList);
        } catch (Exception ex) {
            error_log(ex.toString());
            error_log(ex.getStackTrace().toString());
        }

        try {
            FileOutputStream f_out = new FileOutputStream("bannedUsers-" + this.BOTUSERNAME + ".data");
            ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
            obj_out.writeObject(this.banList);
        } catch (Exception ex) {
            error_log(ex.toString());
            error_log(ex.getStackTrace().toString());
        }

        try {
            FileOutputStream f_out = new FileOutputStream("scores-" + this.BOTUSERNAME + ".data");
            ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
            obj_out.writeObject(this.scores);
        } catch (Exception ex) {
            error_log(ex.toString());
            error_log(ex.getStackTrace().toString());
        }
    }

    /** Method to send a message to everyone **/
    public void sendToAll(String message) throws XMPPException {
        long currenttime = System.currentTimeMillis();
        for (int i = 0; i < relayTo.length; i++) {
            if (!available[i]) {
                continue;
            }
            Chat chat = relayTo[i];
            Long dndexpiry = (Long) this.dndList.get(chat.getParticipant());
            Long banexpiry = (Long) this.banList.get(chat.getParticipant());
            // debug_log("DEBUG " + dndexpiry + " " + banexpiry + "  " + currenttime);
            if (dndexpiry.longValue() < currenttime) {
                chat.sendMessage(message);
            }
        }
    }

    /** General Method to set presence **/
    public void setPresence(Type presence_type, String text) {
        // Create a new presence. 
        Presence presence = new Presence(presence_type);
        presence.setStatus(text);
        // Send the packet (assume we have a XMPPConnection instance called "con").
        connection.sendPacket(presence);
    }

    /** 
     * @return Returns a random entry from the array to be used as a prefix
     */
    static String getRandomPrefix() {
                                                                                                                    String prefixSet[] = {"Madar.", "Gay.", "Bajar.", "Champu.", "Magoda.", "Chussu.", "Besharam.", "Lallu.", "Sust.", "Chaman.", "Bajresh.", "Chutiya.", "MalluLover.", "3inch.", "4inch.", "Hairy.Ass.", "Muthafucka.", "Hagoda.", "Dast.Ka.Mara.", "Sucker.", "Fucker.", "Jagat.Randi.", "Angrez.ka.Chuda.", "Bhains.ki.Tang.", "Choot.ka.Poojari.", "Chamiya.", "Lauda.", "Madarchod.", "Choot.ka.Pyasa.", "Bajar.Lund.", "Bad.Ass.Motherfucker.", "Doodwala.", "Sadhaundh.", "Phaphoond."};
        Random random = new Random();
        return prefixSet[random.nextInt(prefixSet.length)];
    }

    /**
     * 
     * @param participant String like userid[AT]example.com
     * @return A screen name for the participant - Usually name, otherwise same as participant. Has prefix if mode is dirty.
     */
    String getScreenName(String participant) {
        String name = (String) this.usernames.get(participant);
        if (name == null) {
            name = participant;
        }
        if (this.mode.equals("dirty")) {
            name = getRandomPrefix().concat(name);
        }
        return name;
    }

    String getName(String participant) {
        String name = (String) this.usernames.get(participant);
        if (name == null) {
            name = participant;
        }
        return name;
    }

    String getPollResults() {
        String msg = "_So far " + this.pollresult.size() + " people have voted._\n";
        String yeslist = "";
        String nolist = "";
        String maybelist = "";
        int yes = 0, no = 0, maybe = 0;
        Enumeration keys = this.pollresult.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            // DEBUG main.debug_log("debug " + name + " " + this.pollresult.get(name));
            if (this.pollresult.get(name).equals("yes")) {
                yeslist = yeslist.concat(name + ", ");
                yes++;
            } else if (this.pollresult.get(name).equals("no")) {
                nolist = nolist.concat(name + ", ");
                no++;
            } else if (this.pollresult.get(name).equals("maybe")) {
                maybelist = maybelist.concat(name + ", ");
                maybe++;
            }
        }
        if (yes != 0) {
            msg = msg.concat("_*Yes [" + yes + "]*: " + yeslist.substring(0, yeslist.length() - 2) + "_\n");
        }
        if (no != 0) {
            msg = msg.concat("_*No [" + no + "]*: " + nolist.substring(0, nolist.length() - 2) + "_\n");
        }
        if (maybe != 0) {
            msg = msg.concat("_*Maybe [" + maybe + "]*: " + maybelist.substring(0, maybelist.length() - 2) + "_\n");
        }
        return msg;
    }

    public void debug_log(String str) {
        // print to console
        System.out.println(str);

        // if running launcher then add to its log text area
        if (this.launcher != null) {
            this.launcher.addToLog("[Debug] " + str);
        }

        // log in file
        if (out == null) {
            System.err.println("Missing log file");
            return;
        } else {
            try {
                out.write(str + "\n");
                out.flush();
            } catch (IOException ex) {
                error_log(ex.toString());
                error_log(ex.getStackTrace().toString());
            }
        }
    }

    public void chat_log(String str) {
        System.out.println(str);
        if (this.launcher != null) {
            this.launcher.addToLog("[Chat] " + str);
        }
        if (out == null) {
            return;
        } else {
            try {
                out.write("CHAT: " + str + "\n");
                out.flush();
            } catch (IOException ex) {
                error_log(ex.toString());
                error_log(ex.getStackTrace().toString());
            }
        }
    }

    public void error_log(String str) {
        System.err.println(str);
        if (this.launcher != null) {
            this.launcher.addToLog("[Error] " + str);
        }
        if (out == null) {
            return;
        } else {
            try {
                out.write("ERROR: " + str + "\n");
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /** Method creates transfer manager and adds a basic listener to it.
     *  Does Not Work 
     **/
    private void setFileTransferManager() {
//        manager = new FileTransferManager(connection);
//        manager.addFileTransferListener(new FileTransferListener() {
//            public void fileTransferRequest(FileTransferRequest request) {
//                // Check to see if the request should be accepted
//                if (SHOULD_ACCPET_FILES) {
//                    // Accept it
//                    try { 
//                        Chat chat = connection.getChatManager().createChat(request.getRequestor(), new EchoMessageListener());
//                        chat.sendMessage("AcceptING.");
//                        IncomingFileTransfer transfer = request.accept();
//                        transfer.recieveFile(new File(request.getFileName()));
//                        chat.sendMessage("AcceptED.");
//                    } catch (XMPPException ex ) {
//                        error_log(ex.toString()); error_log(ex.getStackTrace().toString());
//                    }
//                } else {
//                    // Reject it
//                    request.reject();
//                }
//            }
//        });
    }
//        // create new Message Listener
//        MessageListener echoMessage = new MessageListener() {
//            public void processMessage(Chat chat, Message message) {
//                try {
//                    // print message body
//                    System.out.println("Received message: " + message.getBody() + " - from " + chat.getParticipant());
//                    
//                    for (int i = 0; i < relayTo.length; i++) {
//                        Chat chat1 = relayTo[i];
//                        if (! chat1.getParticipant().equals(chat.getParticipant()) ) {
//                            chat1.sendMessage(chat.getParticipant()+ ": " + message.getBody());
//                        }
//                    }
//                    
//                    // echo message body
//                    chat.sendMessage("Message relayed: " + message.getBody());
//                    
//
//                    // FIXME: print message props - not working! 
//                    //String props[] = (String[]) message.getPropertyNames().toArray();
//                    //for (int i = 0; i < props.length; i++) {
//                    //    System.out.println(props[i] + " - " + message.getProperty(props[i]));
//                    //}
//
//                } catch (XMPPException ex) {
//                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        };
} // Main class ends here

class EchoMessageListener implements MessageListener {

    Main main;
    

    public EchoMessageListener(Main main) {
        this.main = main;
        
    }

    public void processMessage(Chat chat, Message message) {
        try {

            if (message.getType() == Message.Type.error) {
                main.error_log("Received ERROR!: " + message.getBody() + " - from " + chat.getParticipant());
                return;
            }

            // print message body
            main.chat_log("Received message: " + message.getBody() + " - from " + chat.getParticipant());
            

//            if (message.getBody().startsWith("get_file")) {
//                chat.sendMessage("Sending file now");
//                
//                // Create the outgoing file transfer
//                OutgoingFileTransfer transfer = Main.manager.createOutgoingFileTransfer(chat.getParticipant());
//                // Send the file
//                transfer.sendFile(new File("read.txt"), "You won't believe this!");
//                chat.sendMessage("File Sent started");
//                return;
//            }

            // pause
            if (message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " pause") && this.main.running) {
                main.debug_log("*** ADMIN MESSAGE RECEIVED ***\nBot paused by " + chat.getParticipant() + " : " + message.getBody());
                this.main.running = false;
                this.main.sendToAll("_Bot paused by " + this.main.getScreenName(chat.getParticipant()) + "_");
                this.main.setPresence(Presence.Type.available, "_[PAUSED]_ " + this.main.DEFAULT_PRESENCE);
                return;
            }

            // start
            if (message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " start") && !this.main.running) {
                main.debug_log("*** ADMIN MESSAGE RECEIVED ***\nBot started by " + chat.getParticipant() + " : " + message.getBody());
                this.main.running = true;
                this.main.sendToAll("_Bot started by " + this.main.getScreenName(chat.getParticipant()) + "_");
                this.main.setPresence(Presence.Type.available, Main.DEFAULT_PRESENCE);
                return;
            }

            // quit
            if (message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " quit")) {
                main.debug_log("*** ADMIN MESSAGE RECEIVED ***\nBot quit by " + chat.getParticipant() + " : " + message.getBody());
                if (this.main.running) {
                    if (!message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " quit forced")) {
                        main.debug_log("Bot Status was running.");
                        chat.sendMessage("_Could not perform action. Please pause bot first._");
                        return;
                    }
                }
                main.debug_log("Bot shut down by " + chat.getParticipant());
                this.main.running = false;
                this.main.quit = true;
                //Main.sendToAll("Bot started by " + chat.getParticipant() );
                return;
            }

            // sendtoall
            if (message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " sendtoall")) {
                main.debug_log("*** ADMIN MESSAGE RECEIVED ***\nBroadcast message by " + chat.getParticipant() + " : " + message.getBody());
                this.main.sendToAll("*Bot:* " + message.getBody().substring(("/admin " + this.main.ADMINPASS + " sendtoall").length() + 1));
                return;
            }

            // warn
            if (message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " warn")) {
                String[] split = message.getBody().split(" ", 6);
                //String warned = message.getBody().substring(("/admin " + this.ADMINPASS + " warn ").length(), (message.getBody().toLowerCase()).indexOf(' ', ("/admin " + this.ADMINPASS + " warn ").length()));
                String warned = split[3];
                String warnedUser = (String) this.main.userID.get(warned.toLowerCase());
                main.debug_log("DEBUG warned " + warned + "-> " + warnedUser);
                if (warnedUser == null) {
                    chat.sendMessage("_No such user on this conference. (You sent warning for " + warned + ")_");
                    return;
                } else {
                    String senderName = (String) this.main.usernames.get(chat.getParticipant());
                    String privateMessage = "*" + senderName + " has warned you* : " + message.getBody().substring(message.getBody().indexOf(warned) + warned.length());
                    this.main.connection.getChatManager().createChat(warnedUser, this).sendMessage(privateMessage);
                }
                main.debug_log("*** ADMIN MESSAGE RECEIVED ***\nIssued warning to " + warned + " by " + chat.getParticipant() + " : " + message.getBody());
                this.main.sendToAll("_*Bot:* Issued warning to " + warned + " : " + message.getBody().substring(message.getBody().indexOf(warned) + warned.length()) + "_");
                return;
            }

            // ban
            if (message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " ban")) {
                String[] split = message.getBody().split(" ", 6);
                //String banned = message.getBody().substring(("/admin " + this.ADMINPASS + " ban ").length(), ("/admin " + this.ADMINPASS + " ban ").indexOf(' ', ("/admin " + this.ADMINPASS + " ban ").length()));
                String banned = split[3];
                //String bantime = message.getBody().substring(message.getBody().indexOf(banned)+banned.length()+1);
                String bantime = "";
                if (split.length < 5) {
                    bantime = "10";
                } else {
                    bantime = split[4];
                }
                String reason = "";
                for (int i = 5; i < split.length; i++) {
                    reason = reason.concat(split[i] + " ");
                }
                long banexpireson = Long.parseLong(bantime) * 60 * 1000 + System.currentTimeMillis();
                // DEBUG main.debug_log(banexpireson);
                String bannedUser = (String) this.main.userID.get(banned.toLowerCase());
                if (bannedUser == null) {
                    chat.sendMessage("_No such user on this conference. (You sent ban for " + banned + ")_");
                    return;
                } else {
                    String senderName = (String) this.main.usernames.get(chat.getParticipant());
                    String privateMessage = "*" + senderName + " has banned you* : " + reason + "\n*You will not be able to receive or send any messages for the next " + bantime + " minutes.*";
                    this.main.connection.getChatManager().createChat(bannedUser, this).sendMessage(privateMessage);

                }
                main.debug_log("*** ADMIN MESSAGE RECEIVED ***\nBanned " + banned + " by " + chat.getParticipant() + " for " + bantime + " reason: " + reason.trim() + "\n" + message.getBody());
                this.main.banList.remove(bannedUser);
                this.main.banList.put(bannedUser, new Long(banexpireson));
                this.main.sendToAll("_*Bot:* Banned " + banned + " for the next " + bantime + " minutes_ Reason:" + reason.trim() + "");
                return;
            }

            // adminhelp
            // covers pause, start, quit, sendtoall, warn, ban, 
            if (message.getBody().toLowerCase().startsWith("/admin " + this.main.ADMINPASS + " help") && this.main.running) {
                main.debug_log("*** ADMIN MESSAGE RECEIVED ***\nAdmin help by " + chat.getParticipant() + " : " + message.getBody());
                chat.sendMessage("*Admin Commands and their description*\n\"/admin [admin_pass] pause\" - Pauses the conference bot.\n\"/admin [admin_pass] start\" - Unpauses the conference bot\n\"/admin [admin_pass] sendtoall [Message here]\" - Sends a message to all connected users.\n\"/admin [admin_pass] quit\" - Stops the conference bot permanently.\n\"/admin [admin_pass] warn [username] {Reason_Optional}\" - Sends a warning to the user and notifies everybody else. Look up username using /who command.\n\"/admin [admin_pass] ban [username] {Time in minutes {Reason}}\" - Bans the specified user for given time (default 10 minutes), reason may be specified. Notifies everybody.\n_All commands are case insensitive. For non-admin commands, type /help._");
                return;
            }

            // help
            if (message.getBody().toLowerCase().startsWith("/help")) {
                this.enterMessageToLog(chat, message.getBody());
                //chat.sendMessage("Sorry help not implemented yet. Talk to amanatiit@gmail.com and suggest features that you would like to see implemented.");
                chat.sendMessage("*Commands and their description*\n\"/start poll [Question Here]\" - Starts new poll\n\"/stop poll\" - Stops running poll\n\"/vote [response]\" - Record your vote on the running poll. Allowed Responses: yes/no/maybe\n\"/DND [x]\" - Do Not Disturb for specified time (x in minutes). By default x is 60. \n\"/wakeup\" - Join conference after DND.\n\"/who\" - See who is online.\n\"/set topic [topic of discussion]\" - Sets topic of discussion\n\"@username [message]\" - PM to user (username is case insensitive, use \"/who\" to find out who is online).\n\"/status\" - Prints status of bot, content filter mode and poll results\n\"/ping [test message]\" - Send and Receive a Ping to the Bot\n\"/log\" - See log of past messages\n_All commands are case insensitive._");
                return;
            }

            // mode 
            if (message.getBody().toLowerCase().startsWith("/mode ")) {
                this.enterMessageToLog(chat, message.getBody());
                String newmode = message.getBody().toLowerCase().substring("/mode ".length());
                if (this.main.mode.equals(newmode)) {
                    chat.sendMessage("_Mode is already " + newmode + "._");
                    return;
                }
                if (message.getBody().toLowerCase().startsWith("/mode dirty")) {
                    this.main.mode = "dirty";
                    this.main.sendToAll("_Mode set to dirty by " + this.main.getScreenName(chat.getParticipant()) + "._");
                    return;
                } else if (message.getBody().toLowerCase().startsWith("/mode clean")) {
                    this.main.mode = "clean";
                    this.main.sendToAll("_Mode set to clean by " + this.main.getScreenName(chat.getParticipant()) + "._");
                    return;
                } else if (message.getBody().toLowerCase().startsWith("/mode default")) {
                    this.main.mode = "default";
                    this.main.sendToAll("_Mode set to default by " + this.main.getScreenName(chat.getParticipant()) + "._");
                    return;
                } else {
                    chat.sendMessage("_Unknown Mode. [You tried to set mode to " + newmode + "].\nModes available currently: default, dirty, clean._");
                    return;
                }
            }

            // DND 
            if (message.getBody().toLowerCase().startsWith("/dnd")) {
                this.enterMessageToLog(chat, message.getBody());
                String dndtime = null;
                if (message.getBody().toLowerCase().equals("/dnd") || message.getBody().toLowerCase().equals("/dnd ")) {
                    dndtime = "60";
                } else {
                    dndtime = message.getBody().substring(5);
                }
                long dndexpireson = Long.parseLong(dndtime) * 60 * 1000 + System.currentTimeMillis();
                // DEBUG 
                main.debug_log("Added " + chat.getParticipant() + " to DND list for " + dndtime + " minutes. Wake up on " + dndexpireson + ".");
                this.main.dndList.remove(chat.getParticipant());
                this.main.dndList.put(chat.getParticipant(), new Long(dndexpireson));
                chat.sendMessage("_You will not be disturbed for the next " + dndtime + " minutes. Type \"/wakeup\" to wake up and join the conference._");
                return;
            }

            // wakeup
            if (message.getBody().toLowerCase().startsWith("/wakeup")) {
                this.enterMessageToLog(chat, message.getBody());
                this.main.dndList.remove(chat.getParticipant());
                this.main.dndList.put(chat.getParticipant(), new Long(0));
                chat.sendMessage("_Welcome back. You are no longer on the DND list._");
                return;
            }

            // who
            if (message.getBody().toLowerCase().startsWith("/who")) {
                this.enterMessageToLog(chat, message.getBody());
                String reply = "_Following people are online right now._\n";
                long currenttime = System.currentTimeMillis();
                for (int i = 0; i < this.main.relayTo.length; i++) {
                    if (!this.main.available[i]) {
                        continue;
                    }
                    Chat chat1 = this.main.relayTo[i];
                    Long dndexpiry = (Long) this.main.dndList.get(chat1.getParticipant());
                    Long banexpiry = (Long) this.main.banList.get(chat1.getParticipant());
                    String name = (String) this.main.usernames.get(chat1.getParticipant());
                    if (name == null) {
                        reply = reply.concat("_" + chat1.getParticipant() + (dndexpiry.longValue() < currenttime ? "" : " (DND)") + (banexpiry.longValue() < currenttime ? "" : " (BAN)") + (this.main.anagramChats.containsKey(chat1.getParticipant()) ? " (ANAGRAMS)" : "") + "_\n");
                    } else {
                        reply = reply.concat("_" + name + (dndexpiry.longValue() < currenttime ? "" : " (DND)") + (banexpiry.longValue() < currenttime ? "" : " (BAN)") + (this.main.anagramChats.containsKey(chat1.getParticipant()) ? " (ANAGRAMS)" : "") + "_\n");
                    }
                }
                chat.sendMessage(reply);
                return;
            }

            // start poll
            if (message.getBody().toLowerCase().startsWith("/start poll ")) {
                this.enterMessageToLog(chat, message.getBody());
                this.main.poll = message.getBody().substring("/start poll ".length()).trim();
                this.main.sendToAll("_" + this.main.getScreenName(chat.getParticipant()) + " has started a poll: *" + this.main.poll + "*_\n_To vote, type \"/vote yes\", \"/vote no\" or \"/vote maybe\"._\n_Use \"/status\" to view results._");
                this.main.pollRunning = true;
                this.main.pollresult = new Hashtable();
                this.main.setPresence(Presence.Type.available, (!this.main.topic.equals("") ? ("_Topic: *" + this.main.topic + "*_  \n") : "") + (this.main.pollRunning ? ("_Poll: *" + this.main.poll + "*_ \n") : "") + Main.DEFAULT_PRESENCE);
                return;
            }

            // stop poll
            if (message.getBody().toLowerCase().startsWith("/stop poll")) {
                this.enterMessageToLog(chat, message.getBody());
                if (!this.main.pollRunning) {
                    chat.sendMessage("_Error. No Poll is Running._");
                    return;
                }
                String msg = "_" + this.main.getScreenName(chat.getParticipant()) + " has stopped the poll: *" + this.main.poll + "*_\n_The results are:_\n";
                msg = msg.concat(this.main.getPollResults());
                this.main.sendToAll(msg);
                this.main.pollRunning = false;
                this.main.setPresence(Presence.Type.available, (!this.main.topic.equals("") ? ("_Topic: *" + this.main.topic + "*_  \n") : "") + (this.main.pollRunning ? ("_Poll: *" + this.main.poll + "*_ \n") : "") + Main.DEFAULT_PRESENCE);
                return;
            }

            // vote 
            if (message.getBody().toLowerCase().startsWith("/vote ")) {
                this.enterMessageToLog(chat, message.getBody());
                if (!this.main.pollRunning) {
                    chat.sendMessage("_Error. No active poll._");
                    return;
                }

                String[] split = message.getBody().toLowerCase().split(" ", 2);
                if (split[1].equals("yes") || split[1].equals("no") || split[1].equals("maybe")) {
                    String name = this.main.getName(chat.getParticipant());
                    this.main.pollresult.remove(name);
                    this.main.pollresult.put(name, split[1]);
                    chat.sendMessage("_Your vote has been recorded._");
                    return;
                } else {
                    chat.sendMessage("_Error. Invalid option. To vote, type \"/vote yes\", \"/vote no\" or \"/vote maybe\"_");
                    return;
                }
            }


            // ping
            if (message.getBody().toLowerCase().startsWith("/ping")) {
                this.enterMessageToLog(chat, message.getBody());
                chat.sendMessage("_Ping Received._\n" + message.getBody());
                //chat.sendMessage("Time: " + java.util.Calendar.getInstance().toString());
                return;
            }

            // log
            try {if (message.getBody().toLowerCase().startsWith("/log")) {
                if (this.main.messageLog.size() == 0) {
                    chat.sendMessage("No Messages in Log.");
                    return;
                }

                String reply = "*Displaying Message Log*";
                Object[] log =  this.main.messageLog.toArray();
                for (Object string : log) {
                    reply = reply.concat("\n"+string);
                }
//
//                for (Iterator<String> it = messageLog.iterator(); it.hasNext();) {
//                    reply = reply.concat("\n"+it.next());
//                }

                this.enterMessageToLog(chat, message.getBody());
                //chat.sendMessage("Time: " + java.util.Calendar.getInstance().toString());
                chat.sendMessage(reply);
                return;
            }
            } catch (Exception e) {
                System.err.println(e.toString());
            }


            // status
            if (message.getBody().toLowerCase().startsWith("/status")) {
                this.enterMessageToLog(chat, message.getBody());
                long currenttime = System.currentTimeMillis();
                String reply = "_Status: " + (this.main.running ? "Bot running_" : "Bot paused_");
                reply = reply.concat("\n_Version: " + this.main.VERSION + "_");
                reply = reply.concat("\n_Content Filter Mode: " + this.main.mode + "_");
                if (this.main.pollRunning) {
                    reply = reply.concat("\n_Current Poll: " + this.main.poll + "_\n");
                    reply = reply.concat(this.main.getPollResults());
                } else {
                    reply = reply.concat("\n_No poll is running._");
                }
                Long dndexpiry = (Long) this.main.dndList.get(chat.getParticipant());
                Long banexpiry = (Long) this.main.banList.get(chat.getParticipant());
                if (dndexpiry.longValue() > currenttime) {
                    long timeremaining = (dndexpiry.longValue() - currenttime) / (60 * 1000);
                    reply = reply.concat("\n_You are on the DND list and wake up in " + timeremaining + " minutes._");
                }
                if (banexpiry.longValue() > currenttime) {
                    long timeremaining = (banexpiry.longValue() - currenttime) / (60 * 1000);
                    reply = reply.concat("\n_You are on the BAN list and join conference in " + timeremaining + " minutes._");
                }
                reply = reply.concat("\n_" + this.main.messageLog.size() + " past messages in the log._");
                chat.sendMessage(reply);
                return;
            }

            // anagrams
            if (message.getBody().toLowerCase().startsWith("/anagrams") || message.getBody().toLowerCase().startsWith("/games")) {
                this.enterMessageToLog(chat, message.getBody());
                this.main.anagramChats.remove(chat.getParticipant());
                this.main.anagramChats.put(chat.getParticipant(), chat);
                this.main.anagramThread.newlyJoined(chat);
                return;
            }

            // exit (anagrams)
            if (message.getBody().toLowerCase().startsWith("/exit") && this.main.anagramChats.containsKey(chat.getParticipant())) {
                this.enterMessageToLog(chat, message.getBody());
                this.main.anagramChats.remove(chat.getParticipant());
                chat.sendMessage("_Thanks for playing anagrams!_");
                if (this.main.scores.size() == 0) {
                    return;
                }
                synchronized (this.main.scores) {
                    Enumeration<String> en = this.main.scores.keys();
                    String retstr = "_*Scores for Anagrams*_\n";
                    while (en.hasMoreElements()) {
                        String user = en.nextElement();
                        int sc = ((Integer) this.main.scores.get(user)).intValue();
                        retstr = retstr.concat(this.main.getScreenName(user) + ": " + sc + "\n");
                    }
                    chat.sendMessage(retstr);
                }
                return;
            }

            // hint (anagrams)
            if (message.getBody().toLowerCase().startsWith("/hint") && this.main.anagramChats.containsKey(chat.getParticipant())) {
                this.main.anagramThread.sendAHint(chat);
                return;
            }

            // scores
            if (message.getBody().toLowerCase().startsWith("/scores")) {
                this.enterMessageToLog(chat, message.getBody());

                synchronized (this.main.scores) {
                    Enumeration<String> en = this.main.scores.keys();
                    String retstr = "_*Scores for Anagrams*_\n";
                    while (en.hasMoreElements()) {
                        String user = en.nextElement();
                        int sc = ((Integer) this.main.scores.get(user)).intValue();
                        retstr = retstr.concat(this.main.getScreenName(user) + ": " + sc + "\n");
                    }
                    chat.sendMessage(retstr);
                }
                return;
            }
            if (this.main.anagramChats.containsKey(chat.getParticipant())) {
                this.main.anagramThread.checkWord(chat, message.getBody());
                return;
            }



            // check if on ban list
            if (System.currentTimeMillis() < ((Long) this.main.banList.get(chat.getParticipant())).longValue()) {
                this.enterMessageToLog(chat, message.getBody());
                main.debug_log("Message was from banned user " + chat.getParticipant());
                long timeremaining = (((Long) this.main.banList.get(chat.getParticipant())).longValue() - System.currentTimeMillis()) / (60 * 1000);
                chat.sendMessage("_You are on the ban list for the next " + timeremaining + " minutes. Till then your messages will not be forwarded._");
                return;
            }

            // set topic
            if (message.getBody().toLowerCase().startsWith("/set topic ")) {
                this.enterMessageToLog(chat, message.getBody());
                if (!this.main.running) {
                    chat.sendMessage("_Error: Topic cannot be set while bot has been paused._");
                    return;
                }
                if (message.getBody().toLowerCase().equals("/set topic ")) {
                    chat.sendMessage("_Error: Missing topic!_");
                    return;
                }
                this.main.sendToAll("_Topic changed to " + message.getBody().substring(11) + " by " + chat.getParticipant() + "_");
                this.main.topic = message.getBody().substring(11);
                this.main.setPresence(Presence.Type.available, (!this.main.topic.equals("") ? ("_Topic: *" + this.main.topic + "*_  \n") : "") + (this.main.pollRunning ? ("_Poll: *" + this.main.poll + "*_ \n") : "") + Main.DEFAULT_PRESENCE);
                return;
            }

            // @username for PM
            if (message.getBody().startsWith("@")) {
                // DEBUG main.debug_log(message.getBody().indexOf(' '));
                if (message.getBody().indexOf(' ') < 2) {
                    chat.sendMessage("_Error. Bad message. Missing space to separate username and message._");
                    return;
                }
                String sendToName = message.getBody().substring(1, message.getBody().indexOf(' '));
                String sendToUser = (String) this.main.userID.get(sendToName.toLowerCase());
                if (sendToUser == null) {
                    chat.sendMessage("_No such user on this conference. (You sent PM for " + sendToName + ")_");
                    return;
                } else if (System.currentTimeMillis() < ((Long) this.main.dndList.get(sendToUser)).longValue()) {
                    chat.sendMessage("_User " + sendToName + " has requested not be disturbed._");
                } else {
                    String senderName = this.main.getScreenName(chat.getParticipant());
                    String privateMessage = "*" + senderName + " says in private:* " + message.getBody().substring(message.getBody().indexOf(" ") + 1);
                    this.main.connection.getChatManager().createChat(sendToUser, this).sendMessage(privateMessage);
                    return;
                }
            }

            if (!this.main.running) {
                this.enterMessageToLog(chat, message.getBody());
                chat.sendMessage("_Sorry your message has not been forwarded. The bot is paused right now. Ask your admin to start the conference bot._");
                return;
            }

            if (message.getBody().toLowerCase().startsWith("/admin")) {
                chat.sendMessage("_Sorry your message has not been forwarded. Incorrect password and/or invalid command. If you are trying to run a admin command, try \"/admin <password> help\"._");
                return;
            }

            // set topic
            if (message.getBody().trim().toLowerCase().equals("/set topic")) {
                this.enterMessageToLog(chat, message.getBody());
                chat.sendMessage("_Error: Missing topic!_");
                return;
            }

            synchronized (this.main.relayTo) {
                this.enterMessageToLog(chat, message.getBody());
                long currenttime = System.currentTimeMillis();
                for (int i = 0; i < this.main.relayTo.length; i++) {
                    if (!this.main.available[i]) {
                        continue;
                    }
                    Chat chat1 = this.main.relayTo[i];
                    Long dndexpiry = (Long) this.main.dndList.get(chat1.getParticipant());
                    Long banexpiry = (Long) this.main.banList.get(chat1.getParticipant());
                    if (dndexpiry.longValue() < currenttime && !chat1.getParticipant().equals(chat.getParticipant())) {
                        //String name = (String) this.usernames.get(chat.getParticipant());
                        String name = this.main.getScreenName(chat.getParticipant());
                        String msg = this.main.getFormattedMessage(message.getBody());
                        if (name == null) {
                            chat1.sendMessage("*" + chat.getParticipant() + ":* " + msg);
                        } else {
                            chat1.sendMessage("*" + name + ":* " + msg);
                        }
                    }
                }
            }
            // echo message body
            // DEBUG chat.sendMessage("Message relayed: " + message.getBody());

        } catch (XMPPException ex) {
            main.error_log(ex.toString());
            main.error_log(ex.getStackTrace().toString());
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void enterMessageToLog(Chat chat, String msg) {
        String TIME = this.getTime();
        String name = this.main.getScreenName(chat.getParticipant());
        this.main.messageLog.add("_(" +TIME + ")_ *" + name + "*: " +  msg);
        
        if (this.main.messageLog.size() > Main.MAX_MESSAGE_LOG_SIZE) {
            //main.debug_log("Max Message Log Size Reached.");
            this.main.messageLog.remove(0);
        }

    }

    private String getTime() {
        Calendar calendar = new GregorianCalendar();
        String am_pm;
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        if (calendar.get(Calendar.AM_PM) == 0) {
            am_pm = "AM";
        } else {
            am_pm = "PM";
        }
        return (hour + ":" + minute + ":" + second + " " + am_pm );
    }

} 
