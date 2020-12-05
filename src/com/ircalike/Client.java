package com.ircalike;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/** En klient-klass som innehåller information om en användare i form av användarnamn och lösenord.
 * Klienten loggar in på en server utifrån den information som skapats via Login-klassen och börjar lyssna efter meddelanden från servern med ett BufferedReader objekt. Detta pågår tills det inte läses in några strängar från BufferedReader eller ett meddelande från servern att logga ut (/kick) tas emot varpå socketen och chatten stängs.
 * @author Nephixium
 * @version 1.0
 */

public class Client implements Runnable {

    String username;
    String password;
    Socket clientSocket;
    String hostAddress;
    int hostPort;

    /**
     *
     * @param host serverns address
     * @param port serverns port
     * @param loginUsername klienten eller användarens användarnamn
     * @param password klienten eller användarens lösenord
     */
    public Client(String host, int port, String loginUsername, String password) {
        this.username = loginUsername;
        this.password = password;
        this.hostAddress = host;
        this.hostPort = port;
    }

    /** Ansluter socketen till servern och startar en chatt tillsammans med en BufferedReader som sedan inväntar meddelanden från servern, eller avslutar om det inte går att ta emot några meddelanden.
     *
     */
    @Override
    public void run() {

        try {
            this.clientSocket = new Socket(hostAddress, hostPort);

        } catch (IOException e) {
            e.printStackTrace();
            new JOptionPane().showMessageDialog(null, "Could not connect to host, please restart client");
            System.exit(1);
        }

        Chat chat = new Chat(this.clientSocket, this.username, this.password);
        new Thread(chat).start();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            String message;

            while(true) {
                message = reader.readLine();

                if(message == null) { break; }

                if(message.startsWith("/updateuserlist")) {
                    chat.appendMemberList(message);
                }

                else if(message.equals("/kick " + this.username) || message.equals("/deny")) {
                    this.clientSocket.close();
                    chat.closeChat();
                    break;
                }

                else {
                    chat.appendChat(message);
                }
            }

            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
