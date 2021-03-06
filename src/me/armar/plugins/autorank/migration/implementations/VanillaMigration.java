package me.armar.plugins.autorank.migration.implementations;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.migration.MigrationablePlugin;
import me.armar.plugins.autorank.storage.TimeType;
import org.bukkit.World;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VanillaMigration extends MigrationablePlugin {

    public VanillaMigration(Autorank instance) {
        super(instance);
    }

    @Override
    public boolean isReady() {
        return true; // We can always import since the server should just be running.
    }

    @Override
    public CompletableFuture<Integer> migratePlayTime(List<UUID> uuids) {
        if (!this.isReady()) {
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> {

            getPlugin().debugMessage("Migrating player data from Minecraft's statistics!");

            int playersImported = 0;

            // Loop over each world and sum the total number of playtime over each player.
            for (World world : getPlugin().getServer().getWorlds()) {

                File worldFolder = new File(world.getWorldFolder(), "stats");

                // We don't care about the given players. Instead, we want to find all players that are stored on disk.

                // Get all files in this folder (each file is a player)
                File[] playerFiles = worldFolder.listFiles();

                if (playerFiles == null) continue;

                for (File playerStatistics : playerFiles) {

                    UUID uuid = null;

                    if (!playerStatistics.exists()) continue;

                    try {
                        uuid = UUID.fromString(
                                playerStatistics.getName()
                                        .replace("[Conflict]", "")
                                        .replace(".json", "")
                        );
                    } catch (IllegalArgumentException e) {
                        getPlugin().debugMessage("Couldn't read statistics file '" + playerStatistics.getName() + "' " +
                                "on world " + world.getName());
                        continue;
                    }

                    if (uuid == null) continue;

                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = null;

                    try {
                        jsonObject = (JSONObject) parser.parse(new FileReader(playerStatistics));
                    } catch (IOException | ParseException e) {
                        getPlugin().debugMessage("Couldn't read statistics file of player '" + uuid.toString() + "' " +
                                "on " +
                                "world " + world.getName());
                        continue;
                    }

                    if (jsonObject == null) continue;

                    long ticksPlayed = 0;

                    if (jsonObject.containsKey("stats")) {
                        JSONObject statsSection = (JSONObject) jsonObject.get("stats");

                        if (statsSection.containsKey("minecraft:custom")) {
                            JSONObject customSection = (JSONObject) statsSection.get("minecraft:custom");

                            if (customSection.containsKey("minecraft:play_one_minute")) {
                                ticksPlayed = (long) customSection.get("minecraft:play_one_minute");
                            }
                        }
                    }

                    if (ticksPlayed <= 0) continue;

                    getPlugin().debugMessage("Migrating vanilla data of '" + uuid.toString() + "' " +
                            "on world " + world.getName());

                    getPlugin().getPlayTimeStorageManager().addPlayerTime(TimeType.TOTAL_TIME, uuid,
                            (int) (ticksPlayed / 1200));

                    playersImported++;
                }


            }

            return playersImported;
        });
    }
}
