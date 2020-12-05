package com.ircalike;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;

/** En klienthanterare som appliceras på varje ny klient eller användare som ansluter till servern och lyssnar på samt skickar individuella meddelanden till dessa när de tas emot från servern.
 * Klienthanteraren registrerar varje ny klient eller användare genom Server-objektets metod Server.registerUsername() och formaterar meddelanden som tas emot med hjälp av kännedom om individuella användarnamn som tas emot via mottagna meddelanden.
 * Slutligen ignorerar klienthanteraren vissa meddelanden såvida dessa inte mottagits från en administratör vars namn påbörjas med "++". Meddelanden vidarebefordras till servern som agerar enligt dess innehåll och antingen utför speciella kommandon eller helt enkelt uppdaterar varje inloggad klient eller användares textfält med nya meddelanden.
 * @author Nephixium
 * @version 1.0
 */

public class ClientHandler implements Runnable {

    private Server server;
    private Socket clientSocket;
    private String username;

    /**
     * @param socket klienten eller användarens socket
     * @param server Server-objektet som skapade klienthanteraren
     */
    public ClientHandler(Socket socket, Server server) {
        this.server = server;
        this.clientSocket = socket;
    }

    /** Skapar ett BufferedReader objekt som läser in textmeddelanden enligt UTF_8 format och registrerar användaren på servern med metoden Server.registerUsername() utifrån meddelandet som inledningsvis tagits emot.
     * Första meddelandet måste innehålla /updateuserlist följt av användarnamn annars skickas ett meddelande till klienten eller användaren att avsluta sig och klienthanteraren i sin tur avslutar tråden.
     */
    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream(), UTF_8));
            String message = null;

            if((message = reader.readLine()).startsWith("/updateuserlist") && this.username == null) {
                message = message.replace("/updateuserlist", "");
                int i = message.indexOf('/');
                this.username = message.substring(0,i);
                String password = message.substring(i+1);
                server.registerUsername(this.username, this.clientSocket, password);
            } else {
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                writer.println("/deny");
                return;
            }

            while((message = reader.readLine()) != null) {

                if (message.equals("")) {
                }

                if (message.startsWith("/") && this.username.equals("++Administrator")) {
                    server.processCommand(message);
                }

                if (message.equals("/logout")) {
                    server.processCommand("/kick " + this.username);
                }

                else if (!message.startsWith("/")) {
                    String formattedMessage = this.username + " says: " + message;
                    server.globalSend(formattedMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
