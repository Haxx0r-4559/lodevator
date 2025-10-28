package me.haxx0r.lodevator;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

public class Config {
    private final Lodevator pl;

    private Material material;
    private Sound triggerSound;
    private Trigger defaultTrigger;
    private String defaultMsg = "Tell your server admin to delete Lodevator's config.yml and restart the server.";

    private String checkMsg;
    private String checkOtherMsg;
    private String toggleMsg;
    private String toggleOtherMsg;
    private String upFailMsg;
    private String downFailMsg;

    public Config(Lodevator pl) {
        this.pl = pl;
        reload();
    }

    public void reload() {
        pl.saveDefaultConfig();
        pl.reloadConfig();

        FileConfiguration cfg = pl.getConfig();
        ComponentLogger logger = pl.getComponentLogger();

        material = Material.getMaterial(cfg.getString("material"));
        if (material == null || !material.isBlock()) {
            logger.error(
                    "Plugin disabled due to invalid material. Delete Lodevator's config.yml and restart the server.");
            pl.getServer().getPluginManager().disablePlugin(pl);
        }

        triggerSound = Sound.sound(Key.key(cfg.getString("trigger-sound", "entity.enderman.teleport")),
                Sound.Source.PLAYER, 1.0f, 0.8f);

        defaultTrigger = Trigger.fromString(cfg.getString("default-trigger"));
        if (defaultTrigger == null) {
            logger.warn(
                    "Invalid default-trigger in config.yml, defaulting to CLICK.");
            defaultTrigger = Trigger.CLICK;
        }

        checkMsg = cfg.getString("messages.check", defaultMsg);
        checkOtherMsg = cfg.getString("messages.check-other", defaultMsg);
        toggleMsg = cfg.getString("messages.toggle", defaultMsg);
        toggleOtherMsg = cfg.getString("messages.toggle-other", defaultMsg);
        upFailMsg = cfg.getString("messages.up-fail", defaultMsg);
        downFailMsg = cfg.getString("messages.down-fail", defaultMsg);

        logger.info("Config successfully reloaded.");
    }

    public Material getMaterial() {
        return material;
    }

    public Sound getTriggerSound() {
        return triggerSound;
    }

    public Trigger getDefaultTrigger() {
        return defaultTrigger;
    }

    public String getCheckMsg() {
        return checkMsg;
    }

    public String getCheckOtherMsg() {
        return checkOtherMsg;
    }

    public String getToggleMsg() {
        return toggleMsg;
    }

    public String getToggleOtherMsg() {
        return toggleOtherMsg;
    }

    public String getUpFailMsg() {
        return upFailMsg;
    }

    public String getDownFailMsg() {
        return downFailMsg;
    }
}