package me.griffinbeck.server;

import me.griffinbeck.server.cmdresponses.Commands;

public class BackLoadedCommandPacket extends CommandPacket {
    private byte[] backload;

    /**
     * @param args     Do not included length of byte array
     * @param backload byte array to backload
     */
    public BackLoadedCommandPacket(String[] args, byte[] backload) {
        super(Commands.BACKLOADED_PACKET, addLengthToStartOfArray(backload.length, args));
        this.backload = backload;

    }

    public BackLoadedCommandPacket(CommandPacket commandPacket, byte[] backload) {
        super(commandPacket);
        this.backload = backload;
    }

    private static String[] addLengthToStartOfArray(int length, String[] arr) {
        String[] arrayNew = new String[arr.length + 1];
        arrayNew[0] = "" + length;
        for (int i = 0; i < arr.length; i++) {
            arrayNew[i + 1] = arr[i];
        }
        return arrayNew;
    }

    public byte[] getBackLoad() {
        return backload;
    }
}
