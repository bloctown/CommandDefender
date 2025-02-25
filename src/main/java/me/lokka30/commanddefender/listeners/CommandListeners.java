package me.lokka30.commanddefender.listeners;

import java.util.List;
import me.lokka30.commanddefender.CommandDefender;
import me.lokka30.commanddefender.managers.CommandManager;
import me.lokka30.commanddefender.utils.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

public class CommandListeners implements Listener {

    private final CommandDefender instance;

    public CommandListeners(CommandDefender instance) {
        this.instance = instance;
    }

    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(this, instance);

        //Check if Minecraft 1.13+ is installed.
        if (Utils.classExists("org.bukkit.event.player.PlayerCommandSendEvent")) {
            Bukkit.getPluginManager().registerEvents(new NewCommandListeners(), instance);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(final PlayerCommandPreprocessEvent event) {
        final String command = event.getMessage();

        final CommandManager.BlockedStatus blockedStatus = instance.commandManager.getBlockedStatus(event.getPlayer(), command.split(" "));

        if (blockedStatus.isBlocked) {
            event.setCancelled(true);

            List<String> denyMessage;
            if (blockedStatus.denyMessage == null || blockedStatus.denyMessage.isEmpty()) {
                denyMessage = instance.messagesFile.getConfig().getStringList("cancelled-blocked");
            } else {
                denyMessage = blockedStatus.denyMessage;
            }

            denyMessage.forEach(message -> event.getPlayer().sendMessage(MessageUtils.colorizeAll(message
                    .replace("%prefix%", instance.getPrefix())
                    .replace("%command%", command)
            )));

        } else if (instance.settingsFile.getConfig().getBoolean("block-colons", true)) {
            // check if colon is present in the first arg
            if(command.split(" ")[0].contains(":")) {

                // check bypass permission: return.
                if(event.getPlayer().hasPermission("commanddefender.bypass-colon-blocker")) return;

                event.setCancelled(true);

                instance.messagesFile.getConfig().getStringList("cancelled-colon").forEach(message -> event.getPlayer().sendMessage(MessageUtils.colorizeAll(message
                        .replace("%prefix%", instance.getPrefix())
                )));
            }
        }
    }

    private class NewCommandListeners implements Listener {
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onCommandSend(final PlayerCommandSendEvent event) {
            // Remove commands with colons, if enabled, such as /bukkit:help.
            if (instance.settingsFile.getConfig().getBoolean("block-colons")) {
                event.getCommands().removeIf(command -> command.contains(":"));
            }

            // Make sure filter tab completion is enabled to continue with the next operation.
            if(!instance.settingsFile.getConfig().getBoolean("priorities.enable-command-suggestion-filtering", true)) return;

            // Remove blocked commands from the suggestions list.
            event.getCommands().removeIf(command -> instance.commandManager.getBlockedStatus(event.getPlayer(), ("/" + command).split(" ")).isBlocked);
        }
    }
}
