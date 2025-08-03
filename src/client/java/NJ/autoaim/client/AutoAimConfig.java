package NJ.autoaim.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AutoAimConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "autoaim-settings.json";

    public int timeCompensation = 2;
    public ArrowPosition arrowPosition = new ArrowPosition();

    public static class ArrowPosition {
        public double x = 0.01;
        public double y = -1.5840298512464805;
        public double z = 0.0066421353727;

        public Vec3d toVec3d() {
            return new Vec3d(x, y, z);
        }

        public void fromVec3d(Vec3d vec) {
            this.x = vec.x;
            this.y = vec.y;
            this.z = vec.z;
        }
    }

    public static AutoAimConfig load() {
        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            System.out.println("AutoAim: Creating default config file");
            AutoAimConfig defaultConfig = new AutoAimConfig();
            defaultConfig.save();
            return defaultConfig;
        }

        try {
            String json = Files.readString(configPath);
            AutoAimConfig config = GSON.fromJson(json, AutoAimConfig.class);
            System.out.println("AutoAim: Config loaded successfully");
            return config;
        } catch (Exception e) {
            System.err.println("AutoAim: Failed to load config, using defaults: " + e.getMessage());
            return new AutoAimConfig();
        }
    }

    public void save() {
        try {
            Path configPath = getConfigPath();
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
            System.out.println("AutoAim: Config saved successfully");
        } catch (IOException e) {
            System.err.println("AutoAim: Failed to save config: " + e.getMessage());
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }
}