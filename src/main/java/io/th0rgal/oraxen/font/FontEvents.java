package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.config.Message;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;

public class FontEvents implements Listener {

    private final FontManager manager;

    public FontEvents(FontManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        for (Character character : manager.getReverseMap().keySet()) {
            if (!message.contains(String.valueOf(character)))
                continue;
            Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
            if (!glyph.hasPermission(event.getPlayer())) {
                Message.NO_PERMISSION.send(event.getPlayer(), Template.of("permission", glyph.getPermission()));
                event.setCancelled(true);
            }
        }
        for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet())
            if (entry.getValue().hasPermission(event.getPlayer()))
                message = message.replace(entry.getKey(), String.valueOf(entry.getValue().getCharacter()));
        event.setMessage(message);
    }

}
