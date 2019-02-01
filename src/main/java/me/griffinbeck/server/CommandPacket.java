package me.griffinbeck.server;

import me.griffinbeck.server.cmdresponses.CommandArguments;
import me.griffinbeck.server.cmdresponses.Commands;

public class CommandPacket {
    public static CommandPacket PAUSE_PAUSE_PACKET;
    public static CommandPacket PAUSE_UNPAUSE_PACKET;

    static {
        PAUSE_PAUSE_PACKET = new CommandPacket(Commands.PAUSE, CommandArguments.PAUSE_PAUSECONNECTION);
        PAUSE_UNPAUSE_PACKET = new CommandPacket(Commands.PAUSE, CommandArguments.PAUSE_UNPAUSECONNECTION);
    }

    private String cmd;
    private String[] args;
    private String packet;

    public CommandPacket(CommandPacket commandPacket) {
        this.cmd = commandPacket.cmd;
        this.args = commandPacket.args;
        this.packet = commandPacket.packet;
    }

    public CommandPacket(String cmd, String... args) {
        this.cmd = cmd;
        this.args = args;
        createPacket();
    }

    public CommandPacket(Commands cmd, String... args) {
        this.cmd = cmd.toString();
        this.args = args;
        createPacket();
    }

    public CommandPacket(Commands cmd) {
        this.cmd = cmd.toString();
        this.args = null;
        createPacket();
    }

    public CommandPacket(Commands command, CommandArguments... args) {
        this.cmd = command.toString();
        this.args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            this.args[i] = args[i].toString();
        }
        createPacket();
    }

    private CommandPacket() {

    }


    //TODO set as static method that returns a CommandPacket Instance

    /**
     * initializes packet instance from a received string packet
     *
     * @param packet formatted string packet in the form "\<cmd,args,args2\>"
     */
    public static CommandPacket fromPacket(String packet) {
        if (packet != null && packet.length() > 2) {
            CommandPacket commandPacket = new CommandPacket();
            commandPacket.packet = packet;
            packet = packet.replace("<", "");
            packet = packet.replace(">", "");
            //System.out.println("clean packet: " + packet);
            String[] packetArgs = packet.split(",");
            if (packetArgs.length > 0) {
                commandPacket.cmd = packetArgs[0].trim();
                if (packetArgs.length > 1) {
                    commandPacket.args = new String[packetArgs.length - 1];
                    for (int i = 1; i < packetArgs.length; i++) {
                        commandPacket.args[i - 1] = packetArgs[i].trim();
                    }
                } else {
                    commandPacket.args = null;
                }
            } else {
                commandPacket.cmd = null;
                commandPacket.args = null;
            }
            commandPacket.createPacket();
            return commandPacket;
        } else {
            return null;
        }
    }

    private void createPacket() {
        packet = "<" + cmd;
        if (args != null && args.length > 0) {
            for (String arg : args) {
                packet += "," + arg;
            }
        }
        packet += ">";
    }

    public String getPacket() {
        return packet;
    }

    public String getCmd() {
        return cmd;
    }

    public String[] getArgs() {
        return args;
    }

    public String getArg(int index) {
        try {
            return args[index];
        } catch (Exception e) {
            return "";
        }
    }
}
