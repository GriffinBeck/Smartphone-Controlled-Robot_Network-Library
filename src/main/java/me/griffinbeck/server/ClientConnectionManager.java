package me.griffinbeck.server;

import me.griffinbeck.server.cmdresponses.CommandArguments;
import me.griffinbeck.server.cmdresponses.Commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientConnectionManager {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String currentInput;
    private int readTimeOut;
    private ClientSocketConnector clientSocketConnector;
    private long timeOfLastSocketInteration;
    private boolean heatbeatSent;

    public ClientConnectionManager(Socket socket, ClientSocketConnector clientSocketConnector) {
        this.socket = socket;
        this.readTimeOut = 10000000;
        currentInput = "";
        this.clientSocketConnector = clientSocketConnector;
        clientSocketConnector.setClientConnectionManager(this);
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();

        }
        timeOfLastSocketInteration = System.currentTimeMillis();
        heatbeatSent = false;
    }

    public ClientConnectionManager(Socket socket) {
        this.socket = socket;
        this.readTimeOut = 10000000;
        currentInput = "";
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();

        }
        timeOfLastSocketInteration = System.currentTimeMillis();
        heatbeatSent = false;
    }

    public void sendPacket(Commands cmd, String... data) throws IOException {
        StringBuilder packet = new StringBuilder("<" + cmd.toString());
        for (String word : data) {
            packet.append(",").append(word);
        }
        packet.append(">");
        //out.writeChars(packet);
        out.writeUTF(packet.toString());
        out.flush();
        socket.getOutputStream().flush(); //Re-add if packets are not sent
        timeOfLastSocketInteration = System.currentTimeMillis();
    }

    public void sendPacket(CommandPacket commandPacket) throws IOException {
        String packet = commandPacket.getPacket();
        //out.writeChars(packet);
        out.writeUTF(packet);
        if (commandPacket instanceof BackLoadedCommandPacket) {
            out.write(( (BackLoadedCommandPacket) commandPacket ).getBackLoad());
        }
        out.flush();
        socket.getOutputStream().flush(); //Re-add if packets are not sent
        timeOfLastSocketInteration = System.currentTimeMillis();
    }

    public void sendRawBytes(byte[] arr) throws IOException {
        out.write(arr);
        out.flush();
        timeOfLastSocketInteration = System.currentTimeMillis();
    }

    public CommandPacket getPacket(boolean waitForMessage) throws IOException {
        return waitForMessage ? this.getPacket(0L) : this.getPacket(this.readTimeOut);
    }

    public CommandPacket getPacket(long waitTime) throws IOException {
        boolean found = false;
        if (waitTime > 0) {
            long time = System.nanoTime();
            //byte[] arr;
            while (!found && System.nanoTime() - time < waitTime) {
                //arr = new byte[socketIn.available()];
                //socketIn.readFully(arr);
                //currentInput += new String(arr,"UTF-8");
                if (containsPacket())
                    found = true;
                else if (in.available() > 0) {
                    currentInput += in.readUTF();
                    //System.out.println("currentInput: " + currentInput);
                }
            }
        } else {
            //byte[] arr;
            while (!found) {
                //arr = new byte[socketIn.available()];
                //socketIn.readFully(arr);
                //currentInput += new String(arr,"UTF-8");
                if (containsPacket())
                    found = true;
                else if (( System.currentTimeMillis() - timeOfLastSocketInteration > 2500 )) {
                    if (heatbeatSent) {
                        closeConnection();
                        throw new IOException("Heatbeat Failed");
                    } else {
                        sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_REQUEST));
                        heatbeatSent = true;
                    }
                } else if (System.currentTimeMillis() - timeOfLastSocketInteration > 1500) {
                    sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_REQUEST));
                    heatbeatSent = true;
                } else if (in.available() > 0)
                    currentInput += in.readUTF();
            }
        }
        if (found) {
            int start = currentInput.indexOf("<");
            int end = currentInput.indexOf(">");
            CommandPacket toReturn = CommandPacket.fromPacket(currentInput.substring(start, end + 1));
            currentInput = currentInput.substring(end + 1);
            if (Commands.HEARTBEAT.equalTo(toReturn.getCmd())) {
                if (CommandArguments.HEARTBEAT_REQUEST.equals(toReturn.getArg(0))) {
                    sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_RESPONSE));
                } else {
                    heatbeatSent = false;
                    timeOfLastSocketInteration = System.currentTimeMillis();
                }
                return getPacket(waitTime);
            } else if (Commands.BACKLOADED_PACKET.equalTo(toReturn.getCmd())) {
                byte[] bufferFull = currentInput.getBytes();
                int bufferLength = Integer.parseInt(toReturn.getArg(0));
                byte[] toSendBuffer = new byte[bufferLength];
                if (bufferFull.length > bufferLength) {
                    byte[] currInput = new byte[bufferFull.length - bufferLength];
                    System.arraycopy(bufferFull, bufferLength - 1, currInput, 0, currInput.length);
                    currentInput = new String(currInput);
                    System.arraycopy(bufferFull, 0, toSendBuffer, 0, toSendBuffer.length);
                }
                if (bufferFull.length < bufferLength) {
                    //System.arraycopy(bufferFull, 0, toSendBuffer,0,toSendBuffer.length);//This is commended because UTF wont read raw bytes
                    byte[] input = readRawBytes(bufferLength - bufferFull.length);
                    for (int i = 0; i < input.length; i++) {
                        toSendBuffer[i + bufferFull.length] = input[i];
                    }
                }
                return new BackLoadedCommandPacket(toReturn, toSendBuffer);
            } else {
                timeOfLastSocketInteration = System.currentTimeMillis();
                return toReturn;
            }
        }
        if (System.currentTimeMillis() - timeOfLastSocketInteration > 2500) {
            if (heatbeatSent) {
                closeConnection();
                throw new IOException("Heatbeat Failed");
            } else {
                sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_REQUEST));
                heatbeatSent = true;
            }
        } else if (System.currentTimeMillis() - timeOfLastSocketInteration > 1500) {
            sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_REQUEST));
            heatbeatSent = true;
        }
        return null;
    }

    /*private CommandPacket getPacket(long waitForMessage) throws IOException {
        boolean found = false;
        long time = System.nanoTime();
        byte[] arr;
        while (!found && System.nanoTime() - time < waitForMessage) {
            //arr = new byte[socketIn.available()];
            //socketIn.readFully(arr);
            //currentInput += new String(arr,"UTF-8");
            if (socketIn.available() > 0) {
                currentInput += socketIn.readUTF();
                //System.out.println("currentInput: " + currentInput);
            }
            if (containsPacket())
                found = true;
        }
        if (found) {
            int start = currentInput.indexOf("<");
            int end = currentInput.indexOf(">");
            CommandPacket toReturn = CommandPacket.fromPacket(currentInput.substring(start, end + 1));
            currentInput = currentInput.substring(end + 1);
            return toReturn;
        }
        return null;
    }*/


    public byte[] readRawBytes(int numOfBytes) throws IOException {
        byte[] buffer = new byte[numOfBytes];
        //int imgLength = is.readInt();
        //System.out.println("getLength:" + imgLength);
        //System.out.println("back-token" + is.readUTF());
        int len = 0;
        while (len < numOfBytes) {
            len += in.read(buffer, len, numOfBytes - len);
        }
        return buffer;
    }

    public boolean tryRecconnect() {
        //Verify connection is lost
        closeConnection();
        timeOfLastSocketInteration = System.currentTimeMillis() + 10000;
        socket = clientSocketConnector.connectToServer();
        return socket != null;
    }

    public boolean isConnected() {
        return socket.isConnected() && !socket.isClosed();
    }

    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean containsPacket() {
        int i = currentInput.indexOf("<");
        int x = currentInput.indexOf(">");
        if (i > -1 && x > -1) {
            if (i < x) {
                cleanExcessPacket();
                //System.out.println("contains packet");
                return true;
            } else {
                cleanExcessPacket();
            }
        }
        return false;
    }

    private void cleanExcessPacket() {
        if (currentInput.length() > 60) {
            currentInput = currentInput.substring(40);
        }
        int i = currentInput.indexOf("<");
        int x = currentInput.indexOf(">");
        if (i > -1 && x > -1) {
            if (i > x) {
                currentInput = currentInput.substring(i + 1);
            }
        }
    }

    public void setSocket(Socket socket) {
        if (socket != null && socket.isConnected()) {
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                this.socket = socket;
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }

    public void addTimeToHeartbeat(long val) {
        timeOfLastSocketInteration = System.currentTimeMillis() + val;
    }
}
