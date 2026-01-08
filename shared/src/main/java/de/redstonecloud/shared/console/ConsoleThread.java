package de.redstonecloud.shared.console;

public class ConsoleThread extends Thread {
    public ConsoleThread() {
        setName("Console Thread");
    }

    @Override
    public void run() {
        Console.getInstance().start();
    }
}
