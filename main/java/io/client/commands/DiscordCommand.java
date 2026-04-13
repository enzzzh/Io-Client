package io.client.commands;

public class DiscordCommand implements Command {
    private static final String DISCORD_LINK = "https://discord.gg/KyXxzgycxb";

    @Override
    public void execute(String[] args) throws Exception {
        CommandManager.INSTANCE.sendMessage("§9IO Client Discord: §b" + DISCORD_LINK);
    }
}
