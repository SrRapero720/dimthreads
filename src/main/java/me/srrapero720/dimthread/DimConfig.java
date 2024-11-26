package me.srrapero720.dimthread;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DimConfig {
    private static final Gson GSON = new Gson();
    private static final File FILE = FabricLoader.getInstance().getConfigDir().resolve("dimthreads-common.json").toFile();

    public int DEFAULT_GAMERULE_THREADS = 3;
    public boolean IGNORE_TICK_CRASH = false;

    DimConfig(boolean root) {
        if (root) {
            if (FILE.exists()) {
                try (InputStream in = new FileInputStream(FILE)) {
                    DimConfig disposal = GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), DimConfig.class);
                    if (disposal.DEFAULT_GAMERULE_THREADS == 0) throw new RuntimeException("Wrong default gamerule threads value, resaving");
                    this.DEFAULT_GAMERULE_THREADS = disposal.DEFAULT_GAMERULE_THREADS;
                    this.IGNORE_TICK_CRASH = disposal.IGNORE_TICK_CRASH;
                    DimThread.LOGGER.info("Config Loaded");
                } catch (Exception e) {
                    DimThread.LOGGER.error("Cannot read config file", e);
                    this.save();
                }
            } else {
                this.save();
            }
        }
    }

    public DimConfig() {

    }

    public void save() {
        try (OutputStream ou = new BufferedOutputStream(new FileOutputStream(FILE))) {
            ou.write(GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            DimThread.LOGGER.error("Cannot save config file");
        }
    }
}
