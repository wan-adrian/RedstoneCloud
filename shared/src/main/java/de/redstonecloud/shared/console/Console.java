package de.redstonecloud.shared.console;

import de.redstonecloud.shared.commands.CommandManager;
import de.redstonecloud.shared.utils.SharedUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

@Log4j2
public class Console extends SimpleTerminalConsole {
    private static Console instance;

    public static Console getInstance() {
        if(instance == null) instance = new Console();
        return instance;
    }

    private Console() {

    }

    public static boolean running = true;

    @Override
    protected boolean isRunning() {
        return running;
    }

    @Override
    protected void runCommand(String command) {
        //boolean hasLogServer = server.getCurrentLogServer() != null;
        //if (!hasLogServer) {
        String cmd = command.split(" ")[0];
        String[] args = SharedUtils.dropFirstString(command.split(" "));
        CommandManager.getInstance().executeCommand(cmd, args);
        //} else {
            /*if (command.equalsIgnoreCase("_exit")) {
                server.getCurrentLogServer().disableConsoleLogging();
                server.setCurrentLogServer(null);
                log.info("Exited console");
            } else {
                server.getCurrentLogServer().getServer().writeConsole(command);
            }*/
        //}
    }

    @Override
    protected void shutdown() {
        ;
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        builder.completer(new ConsoleCompleter());
        builder.appName("RedstoneCloud");
        builder.option(LineReader.Option.HISTORY_BEEP, false);
        builder.option(LineReader.Option.HISTORY_IGNORE_DUPS, true);
        builder.option(LineReader.Option.HISTORY_IGNORE_SPACE, true);
        return super.buildReader(builder);
    }
}