package me.griffinbeck.server;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            String arg = args[0];
            if (arg.equalsIgnoreCase("-c") || arg.equalsIgnoreCase("-client")) {
                ConnectionWrapper.main(args);
            } else {
                Server.main(args);
            }
        } else {
            Server.main(args);
        }
    }
}
