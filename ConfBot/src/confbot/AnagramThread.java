/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package confbot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;

/**
 *
 * @author Aman
 */
public class AnagramThread extends Thread {

    Vector dictionary;
    Random random; 
    String WORD ="";
    String ANAGRAM ="";
    Main main;
    int SCORE;

    public AnagramThread(Main main) {
        this.main = main;
        dictionary = new Vector();
        try {
            String filename = "dict";

            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            while (in.ready()) {
                String s = in.readLine().trim();
                if (s.charAt(0) >= 'A' && s.charAt(0) <= 'Z') {
                    continue;
                }
                dictionary.addElement(s.toUpperCase());
            }
        } catch (IOException e) {
            System.out.println("Dictionary file operation errors.");
        }
        
        random = new Random();
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (main.quit) break;
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(AnagramThread.class.getName()).log(Level.SEVERE, null, ex);
            }
//            String choosenWord = getRandomWord();
//            String anagram = makeAnagram(choosenWord);
//            synchronized(WORD) {
//                synchronized(ANAGRAM) {
//                    this.WORD = choosenWord;
//                    this.ANAGRAM = anagram;
//                }
//            }
//            System.out.println("DEBUG 312 " + choosenWord + " " + anagram);
//            try {
//                sendToAll();
//            }
//            catch (XMPPException ex) {
//                ex.printStackTrace();
//            }
            
        }
    }

    void checkWord(Chat chat, String string) throws XMPPException {
        if (string.toUpperCase().equals(this.WORD)) {
            
            synchronized(this.main.scores) {
                if (this.main.scores.containsKey(chat.getParticipant())) {
                    int sc = this.SCORE + ((Integer) this.main.scores.remove(chat.getParticipant())).intValue();
                    this.main.scores.put(chat.getParticipant(), new Integer(sc));
                }
                else {
                    this.main.scores.put(chat.getParticipant(), new Integer (this.SCORE));
                }
            }
            sendToAll("_" + this.main.getName(chat.getParticipant()) + " answers *" + this.WORD + "* correctly and scores " + this.SCORE + " points._");
            
            getNewQuestion();
            sendToAll();
        }
        else if (string.toLowerCase().equals("exit")) {
            chat.sendMessage("_Thank you for playing with us._");
            if (this.main.scores.size() == 0 ) return;
            synchronized(this.main.anagramChats) {
                this.main.anagramChats.remove(chat.getParticipant());
            }
            synchronized(this.main.scores) {
                Enumeration<String> en = this.main.scores.keys();
                String retstr = "_*Scores for Anagrams*_\n";
                while (en.hasMoreElements()) {
                    String user = en.nextElement();
                    int sc = ((Integer)this.main.scores.get(user)).intValue();
                    retstr = retstr.concat(this.main.getScreenName(user) + ": " + sc + "\n");
                }
                chat.sendMessage(retstr);
            }
        }
        else {
            chat.sendMessage("*Anagrams:* _That is incorrect!_");
            this.sendToAll("*Anagrams:* " + this.main.getScreenName(chat.getParticipant()) + " guessed *" + string + "*");
        }
        
    }

    void newlyJoined(Chat chat) throws XMPPException {
        main.debug_log(chat.getParticipant() + " has joined anagrams");
        chat.sendMessage("_Welcome to Anagrams with " + this.dictionary.size() + " words. Type */exit* to exit the game, */scores* to view overall scores and */hint* to get a hint._\n_(You will not be able to send any messages while you are playing)._");
        sendLastQuestion(chat);
    }

    String makeAnagram(String choosenWord) {
        int len = choosenWord.length();
        int anagramIndex[] = new int[len];
        int c = 0;
        while(c<len) {
            int randInt = random.nextInt(len);
            if (anagramIndex[randInt]==0) {
                anagramIndex[randInt] = c+1;
                c++;
            }
        }
        char[] chararray = new char[len];
        for (int i = 0; i < chararray.length; i++) {
            chararray[i] = choosenWord.charAt(anagramIndex[i]-1);
        }
        String retstr = new String(chararray);
        return retstr.equals(choosenWord)?makeAnagram(choosenWord):retstr;
    }

    String getRandomWord() {
        return ((String) dictionary.get(random.nextInt(dictionary.size()))).toUpperCase();
    }

    void sendAHint(Chat chat) throws XMPPException {
        String hint = getHint();
        this.sendToAll("_" + this.main.getName(chat.getParticipant()) + " asked for a hint: *" + hint + "*_");
    }

    void sendLastQuestion(Chat chat) throws XMPPException {
        if (this.ANAGRAM.equals("")) {
            getNewQuestion();
            sendToAll();
        } else {
            chat.sendMessage("*Anagram:* " + this.ANAGRAM);
        }
    }
    
    void getNewQuestion() {
        String choosenWord = getRandomWord();
        String anagram = makeAnagram(choosenWord);
        synchronized(WORD) {
            synchronized(ANAGRAM) {
                this.WORD = choosenWord;
                this.ANAGRAM = anagram;
                this.SCORE = choosenWord.length();
            }
        }
    }

    void sendToAll() throws XMPPException {
        synchronized(this.main.anagramChats) {
            //Iterator<Chat> iter = this.main.anagramChats.keySet().iterator();
            Enumeration iter = this.main.anagramChats.elements();
            while(iter.hasMoreElements()) {
                Chat chat = (Chat) iter.nextElement();
                String user = chat.getParticipant();
                if (this.main.roster.getPresence(user).isAvailable()) {
                    sendLastQuestion(chat);
                } else {
                    this.main.anagramChats.remove(chat.getParticipant());
                }
            }
        }
    }

    private String getHint() {
        int n = this.WORD.length();
        char[] array = new char[n];
        for (int i = 0; i < n; i++) {
            array[i] = (random.nextInt(n)>n/2?this.WORD.charAt(i):'?');
        }
        int c1=0, c2=0;
        for (int i = 0; i < array.length; i++) {
            if (array[i]=='?')
                c1++;
            else 
                c2++;
        }
        if (c1==0 || c2 == 0) {
            return getHint();
        }
        this.SCORE = this.SCORE>c2?this.SCORE-c2:1;
        return new String (array);
    }

    private void sendToAll(String string) throws XMPPException {
        synchronized(this.main.anagramChats) {
            //Iterator<Chat> iter = this.main.anagramChats.keySet().iterator();
            Enumeration iter = this.main.anagramChats.elements();
            while(iter.hasMoreElements()) {
                Chat chat = (Chat) iter.nextElement();
                String user = chat.getParticipant();
                if (this.main.roster.getPresence(user).isAvailable()) {
                    chat.sendMessage(string);
                } else {
                    this.main.anagramChats.remove(chat.getParticipant());
                }
            }
        }
    }
}
