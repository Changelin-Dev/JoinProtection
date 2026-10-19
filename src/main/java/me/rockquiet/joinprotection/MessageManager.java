package me.rockquiet.joinprotection;

import me.rockquiet.joinprotection.configuration.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class MessageManager {

    private final ConfigManager configManager;
    private final MiniMessage msg;

    public MessageManager(JoinProtection plugin) {
        this.configManager = plugin.configManager();
        this.msg = MiniMessage.miniMessage();
    }

    private TagResolver getPrefixPlaceholder() {
        return Placeholder.parsed("prefix", configManager.get().messages.prefix);
    }

    /** Parses a MiniMessage string with the prefix placeholder and the given resolvers. */
    public Component parse(String message, TagResolver... tagResolvers) {
        return msg.deserialize(message, getPrefixPlaceholder(), TagResolver.resolver(tagResolvers));
    }

    public void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, message, TagResolver.empty());
    }

    public void sendMessage(CommandSender sender, String message, TagResolver... tagResolvers) {
        if (message.isBlank()) return;

        sender.sendMessage(msg.deserialize(
                message,
                getPrefixPlaceholder(), TagResolver.resolver(tagResolvers)
        ));
    }

    public void sendActionbar(Player player, String message, TagResolver... tagResolvers) {
        if (message.isBlank()) return;

        player.sendActionBar(msg.deserialize(
                message,
                getPrefixPlaceholder(), TagResolver.resolver(tagResolvers)
        ));
    }

    private Title.Times getTitleTimes() {
        return configManager.get().display.title.toTimes();
    }

    public void sendTitle(Player player, String message, TagResolver... tagResolvers) {
        if (message.isBlank()) return;

        player.showTitle(Title.title(
                msg.deserialize(message, getPrefixPlaceholder(), TagResolver.resolver(tagResolvers)),
                Component.empty(),
                getTitleTimes()
        ));
    }

    public void sendSubTitle(Player player, String message, TagResolver... tagResolvers) {
        if (message.isBlank()) return;

        player.showTitle(Title.title(
                Component.empty(),
                msg.deserialize(message, getPrefixPlaceholder(), TagResolver.resolver(tagResolvers)),
                getTitleTimes()
        ));
    }

    public void sendProtectionInfo(Player player, String message, TagResolver... tagResolvers) {
        switch (configManager.get().display.location.toLowerCase(Locale.ROOT)) {
            case "title" -> sendTitle(player, message, tagResolvers);
            case "subtitle" -> sendSubTitle(player, message, tagResolvers);
            case "chat" -> sendMessage(player, message, tagResolvers);
            default -> sendActionbar(player, message, tagResolvers);
        }
    }
}
