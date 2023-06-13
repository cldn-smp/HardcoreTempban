package pro.cloudnode.smp.hardcoretempban;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

public final class DeadPlayer {
    public final UUID uuid;
    public Date lastDeath;
    public int deathCount;
    public String deathMessage;

    public DeadPlayer(UUID uuid, Date lastDeath, int deathCount, String deathMessage) {
        this.uuid = uuid;
        this.lastDeath = lastDeath;
        this.deathCount = deathCount;
        this.deathMessage = deathMessage;
    }

    public DeadPlayer(UUID uuid) {
        this(uuid, null, 0, null);
    }


    /**
     * Load list of dead players from deadPlayers.json using GSON. Ignore any missing libraries.
     */
    public static List<DeadPlayer> load() {
        final File file = new File("deadPlayers.json");
        if (!file.exists()) createFileIfNotExist(file);
        else {
            try {
                final Scanner scanner = new Scanner(file);
                final StringBuilder builder = new StringBuilder();
                while (scanner.hasNextLine()) builder.append(scanner.nextLine());
                JsonArray array = new Gson().fromJson(builder.toString(), JsonArray.class);

                final List<DeadPlayer> deadPlayers = new ArrayList<>();
                for (JsonElement element : array) {
                    final UUID uuid = UUID.fromString(element.getAsJsonObject().get("uuid").getAsString());
                    final Date lastDeath = new Date(element.getAsJsonObject().get("lastDeath").getAsLong());
                    final int deathCount = element.getAsJsonObject().get("deathCount").getAsInt();
                    final String deathMessage = element.getAsJsonObject().get("deathMessage").getAsString();
                    deadPlayers.add(new DeadPlayer(uuid, lastDeath, deathCount, deathMessage));
                }
                return deadPlayers;
            }
            catch (FileNotFoundException e) {
                HardcoreTempban.getInstance().getLogger().warning("Failed to read deadPlayers.json");
            }
        }
        return new ArrayList<>();
    }

    /**
     * Save list of dead players to deadPlayers.json using GSON. Ignore any missing libraries.
     */
    public static void save(List<DeadPlayer> deadPlayers) {
        final File file = new File("deadPlayers.json");
        if (!file.exists()) createFileIfNotExist(file);
        else {
            try (FileWriter writer = new FileWriter(file)) {
                final JsonArray array = new JsonArray();
                for (DeadPlayer deadPlayer : deadPlayers) {
                    final Map<String, Object> map = new HashMap<>();
                    map.put("uuid", deadPlayer.uuid.toString());
                    map.put("lastDeath", deadPlayer.lastDeath.getTime());
                    map.put("deathCount", deadPlayer.deathCount);
                    map.put("deathMessage", deadPlayer.deathMessage);
                    array.add(new Gson().toJsonTree(map));
                }
                writer.write(array.toString());
            }
            catch (Exception e) {
                HardcoreTempban.getInstance().getLogger().warning("Failed to write deadPlayers.json");
            }
        }
    }

    private static void createFileIfNotExist(File file) {
        try {
            final boolean created = file.createNewFile();
            if (!created) HardcoreTempban.getInstance().getLogger().warning("Failed to create deadPlayers.json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("[]");
            }
        }
        catch (Exception e) {
            HardcoreTempban.getInstance().getLogger().warning("Failed to create deadPlayers.json");
        }
    }

    /**
     * Get ban duration in seconds
     * @param deathCount Number of deaths
     */
    public static int getBanDuration(int deathCount) throws Exception {
        final ConfigurationSection section = HardcoreTempban.getInstance().getConfig().getConfigurationSection("ladder");
        if (section == null) throw new Exception("Property 'ladder' not found in config.yml");
        final int[] keys = section.getKeys(false).stream().mapToInt(Integer::parseInt).toArray();
        if (keys.length == 0) throw new Exception("No durations specified in `ladder` in config.yml");
        final HashMap<Integer, Integer> ladder = new HashMap<>();
        for (int key : keys) ladder.put(key, section.getInt(String.valueOf(key)));
        Arrays.sort(keys);
        final int maxKey = ladder.keySet().stream().sorted().filter(k -> deathCount >= k).max(Integer::compareTo).orElse(-1);
        if (maxKey == -1) throw new Exception("No duration found for death count " + deathCount);
        return ladder.get(maxKey);
    }

    /**
     * Check if the player's ban is still active
     */
    public boolean isBanned() throws Exception {
        return this.getBanExpiry().after(Calendar.getInstance().getTime());
    }

    /**
     * Get ban expiry date
     */
    public Date getBanExpiry() throws Exception {
        final int banDuration = getBanDuration(deathCount);
        return new Date(lastDeath.getTime() + banDuration * 1000L);
    }
}
