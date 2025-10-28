package me.haxx0r.lodevator;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerPrefs {
    private final Lodevator pl;
    private final Config cfg;
    private HashMap<UUID, Trigger> prefs;

    public PlayerPrefs(Lodevator pl, Config cfg) {
        this.pl = pl;
        this.cfg = cfg;
        reload();
    }

    public void reload() {
        pl.saveResource("playerprefs.yml", false);
        File file = new File(pl.getDataFolder(), "playerprefs.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        prefs = new HashMap<>();
        for (String key : config.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            Trigger trigger = Trigger.fromString(config.getString(key));
            if (trigger != null) {
                prefs.put(uuid, trigger);
            }
        }
    }

    public Trigger get(UUID uuid) {
        return prefs.containsKey(uuid) ? prefs.get(uuid) : cfg.getDefaultTrigger();
    }

    public Trigger toggle(UUID uuid) {
        Trigger trigger = get(uuid) == Trigger.CLICK ? Trigger.MOVE : Trigger.CLICK;
        prefs.put(uuid, trigger);
        save(uuid, trigger);
        return trigger;
    }

    private void save(UUID uuid, Trigger trigger) {
        File file = new File(pl.getDataFolder(), "playerprefs.yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set(uuid.toString(), trigger.toString());
        // for (UUID uuid : prefs.keySet()) {
        // config.set(uuid.toString(), prefs.get(uuid).toString());
        // }

        try {
            config.save(file);
        } catch (Exception e) {
            pl.getComponentLogger().error("Failed to save playerprefs.yml", e);
        }
    }
}
