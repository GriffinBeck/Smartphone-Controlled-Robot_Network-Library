package me.griffinbeck.server.cmdresponses;

public enum CommandArguments {
    EXIT_CONNECTIONLOST(Commands.EXIT, "CONNECTIONLOST"),
    EXIT_AUTHENTICATIONFAILED(Commands.EXIT, "AUTHENTICATIONFAILED"),
    EXIT_CONNECTIONFILLED(Commands.EXIT, "CONNECTIONFILLED"),
    EXIT_CONNECTIONTERMINATED(Commands.EXIT, "CONNECTIONTERMINATED"),
    PAUSE_CONNECTIONLOST(Commands.PAUSE, "CONNECTIONLOST"),
    PAUSE_PAUSECONNECTION(Commands.PAUSE, "ON"),
    PAUSE_UNPAUSECONNECTION(Commands.PAUSE, "OFF"),
    PAUSE_ESTABLISHLINK(Commands.PAUSE, "ESABLISHLINK"),
    LINK_REQUESTLINK(Commands.LINK, "REQUEST"),
    LINK_LINKOPENED(Commands.LINK, "OPEN"),
    LINK_PREPARE(Commands.LINK, "PREPARE"),
    REQUEST_PROGRAMCONFIG(Commands.REQUEST, "PROGCONFIG"),
    RESPONSE_AUTORECCONNECT(Commands.RESPONSE, "RECONNECT"),
    REQUEST_IMG(Commands.REQUEST, "IMG"),
    RESPONSE_IMG(Commands.RESPONSE, "IMGSTART"),
    HEARTBEAT_REQUEST(Commands.HEARTBEAT, "REQ"),
    HEARTBEAT_RESPONSE(Commands.HEARTBEAT, "RES"),
    BACKLOADED_PACKET_IMG(Commands.BACKLOADED_PACKET, "IMG");

    private final Commands linkedCommand;
    private String subCommand;

    CommandArguments(Commands linkedCommand, String subCommand) {
        this.linkedCommand = linkedCommand;
        this.subCommand = subCommand;
    }

    public Commands getLinkedCommand() {
        return linkedCommand;
    }

    public String toString() {
        return subCommand;
    }

    public boolean equals(String cmdArg) {
        if (cmdArg == null)
            return false;
        return cmdArg.equals(subCommand);
    }
}
