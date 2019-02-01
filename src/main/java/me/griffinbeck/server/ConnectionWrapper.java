package me.griffinbeck.server;


import me.griffinbeck.server.cmdresponses.CommandArguments;
import me.griffinbeck.server.cmdresponses.Commands;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Does not currently function due to added protocols for handling a connection.
 */
public class ConnectionWrapper {
    private static BlockingQueue<CommandPacket> packetOut;
    private static boolean doRun;
    private static ClientConnectionManager clientConnectionManager;

    public static void main(String[] args) {
        packetOut = new LinkedBlockingQueue<>();
        doRun = true;
        ClientSocketConnector connector;
        ExecutorService executor = Executors.newCachedThreadPool();
        String ip;
        String inputString;
        int port;
        boolean isRobot;
        Scanner usrIn = new Scanner(System.in);
        System.out.println("Welcome to Client Socket Connection Wrapper");
        System.out.print("Please enter the IP of the Server: ");
        ip = usrIn.nextLine();
        //ip = "localhost";//Lazy Debug code
        System.out.print("\nPlease enter the port of the Server: ");
        port = getIntInput(usrIn);
        //port = 3068;//Lazy Debug code
        System.out.print("\nIs this connection, a robot? [y/n]: ");
        //inputString = "y";//Lazy Debug code
        inputString = usrIn.nextLine();
        isRobot = inputString.charAt(0) == 'y';
        connector = new ClientSocketConnector(ip, port, isRobot);
        Socket socket = connector.connectToServer();
        if (socket == null) {
            System.out.println("Failed to establish connection now terminating");
            System.exit(-1);
        }
        clientConnectionManager = new ClientConnectionManager(socket, connector);
        Thread thread = new Thread(() -> {
            doRun = true;
            runLoop();
        });
        executor.submit(thread);
        System.out.println("Connected To Server, to send a message just type and press enter");
        System.out.println("To exit, just type exit and press enter");
        while (doRun) {
            inputString = usrIn.nextLine();
            if (inputString.equalsIgnoreCase("exit")) {
                packetOut.clear();
                packetOut.add(new CommandPacket(Commands.EXIT, CommandArguments.EXIT_CONNECTIONTERMINATED));
                doRun = false;
                System.out.println("Exiting");
            } else {
                if (!inputString.trim().equalsIgnoreCase("")) {
                    packetOut.add(new CommandPacket(inputString));
                    System.out.println("Sent packet containing: " + inputString);
                }
            }
        }
        executor.shutdown();
    }

    public static int getIntInput(Scanner in) {
        String input = in.nextLine();
        int out;
        try {
            out = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.print("\n Invalid input please try again: ");
            out = getIntInput(in);
        }
        return out;
    }

    private static void runLoop() {
        //byte[] arr;
        CommandPacket inPacket = null;
        while (doRun) {
            try {
                //arr = new byte[inputStream.available()];
                //inputStream.readFully(arr);
                //currentInput += new String(arr, "ASCII");
                /*if(inputStream.available() > 0)
                    currentInput += inputStream.readUTF();*/
                System.out.println("trying get packet");
                inPacket = clientConnectionManager.getPacket(false);
                System.out.println("finished get packet");
                if (inPacket != null) {
                    System.out.println("In packet: " + inPacket.getCmd() + ", " + Arrays.toString(inPacket.getArgs()));
                    inPacket = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (packetOut.remainingCapacity() > 0) {
                System.out.println("trying to send packet");
                try {
                    clientConnectionManager.sendPacket(packetOut.take());
                    System.out.println("executed send packet");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
