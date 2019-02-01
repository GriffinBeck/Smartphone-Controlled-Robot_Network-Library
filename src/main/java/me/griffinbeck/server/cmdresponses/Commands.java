package me.griffinbeck.server.cmdresponses;

public enum Commands {
    RESPONSE("res"),
    REQUEST("req"),
    CONNECTIONESABLISHED("connected"),
    EXIT("exit"),
    PAUSE("pause"),
    LINK("link"),
    HEARTBEAT("heartbeat"),
    BACKLOADED_PACKET("backload");//Backloaded Packets will always take the form of <CMD,LengthOfBackload,Args...>

    private final String command;

    Commands(String cmd) {
        command = cmd;
    }

    public String toString() {
        return command;
    }

    public boolean equalTo(String cmd) {
        if (cmd == null)
            return false;
        return this.command.equals(cmd);
    }
}
