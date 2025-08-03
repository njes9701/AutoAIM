package NJ.autoaim.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoAimController {
    private static boolean enabled = false;
    private static boolean wasKeyPressed = false;

    // 配置管理
    private static AutoAimConfig config;

    // 網路延遲追蹤
    private static int currentPing = 0;
    private static long lastPingUpdate = 0;

    // 自動補償設定
    private static boolean autoCompensationEnabled = false;

    // 用於計算玩家速度的變數
    private static final Map<PlayerEntity, Vec3d> lastPlayerPositions = new HashMap<>();
    private static final Map<PlayerEntity, Vec3d> playerVelocities = new HashMap<>();

    public static void onClientTick(MinecraftClient client) {
        handleKeyInput(client);

        if (client.player != null && client.world != null) {
            // 更新所有玩家的速度
            updatePlayerVelocities(client);

            // 每60 tick（3秒）檢測一次延遲
            if (client.world.getTime() % 60 == 0) {
                checkNetworkLatency(client);
            }

            // 當自動瞄準開啟時，每tick更新延遲（為了HUD顯示）
            if (enabled) {
                updateRealtimeLatency(client);
                performAutoAim(client);
            }
        }
    }

    private static void handleKeyInput(MinecraftClient client) {
        boolean isCurrentlyPressed = AutoAimClient.toggleAutoAimKey.isPressed();

        if (isCurrentlyPressed && !wasKeyPressed) {
            enabled = !enabled;
            if (client.player != null) {
                String status = enabled ? "§aON" : "§cOFF";
                client.player.sendMessage(Text.literal("AutoAim: " + status), true);
            }
        }

        wasKeyPressed = isCurrentlyPressed;
    }

    private static void performAutoAim(MinecraftClient client) {
        // 檢查數據是否載入成功
        if (!CalibrationData.isDataLoaded()) {
            return; // 沒有校準數據就不執行
        }

        ClientPlayerEntity localPlayer = client.player;
        // 使用配置文件中的弓箭位置
        Vec3d arrowPos = config.arrowPosition.toVec3d();

        // 找最近目標
        PlayerEntity target = findNearestTarget(client, arrowPos);
        if (target == null) return;

        // 計算瞄準
        Vec3d targetPos = target.getPos().add(0, 0.3, 0); // 瞄準胸部
        Vec3d targetDis = targetPos.subtract(arrowPos);

        double targetMag = targetDis.length();
        double targetXZ = Math.sqrt(targetDis.x * targetDis.x + targetDis.z * targetDis.z);
        double targetPitch = Math.toDegrees(Math.atan2(-targetDis.y, targetXZ)); // 轉換成度數


        if (targetPitch > -18) {
            return; // 目標角度太低放棄追蹤
        }

        // 第一次速度校正
        double correctedSpeed = CalibrationData.interpolate(
                CalibrationData.getPitchOutputInitial(),
                CalibrationData.getArrowOutput(),
                targetPitch
        );
        int targetTime = CalibrationData.coverDistance(correctedSpeed, targetMag);

        // 預測目標未來位置
        Vec3d targetVel = getPlayerVelocity(target); // 使用計算出的速度
        Vec3d targetFuture = targetDis.add(targetVel.multiply(targetTime));

        // 加上網路延遲補償
        int totalCompensation = getTotalCompensation();
        Vec3d compensatedFuture = targetFuture.add(targetVel.multiply(totalCompensation));

        double futureXZ = Math.sqrt(compensatedFuture.x * compensatedFuture.x + compensatedFuture.z * compensatedFuture.z);
        double futurePitch = Math.toDegrees(Math.atan2(-compensatedFuture.y, futureXZ)); // 轉換成度數
        double futureYaw = Math.atan2(-compensatedFuture.x, compensatedFuture.z); // yaw 保持弧度，稍後轉換



        // 最終角度校正
        double[] timeCompensatedOutput = CalibrationData.getTimeCompensatedOutput(targetTime);
        double correctedPitch = CalibrationData.interpolate(
                timeCompensatedOutput,
                CalibrationData.getPitchInput(),
                futurePitch // 現在是度數
        );

        // 應用瞄準 (轉換回弧度給 Minecraft)
        localPlayer.setYaw((float) Math.toDegrees(futureYaw));
        localPlayer.setPitch((float) correctedPitch); // correctedPitch 已經是度數
    }

    private static PlayerEntity findNearestTarget(MinecraftClient client, Vec3d arrowPos) {
        List<AbstractClientPlayerEntity> players = client.world.getPlayers();
        PlayerEntity nearestTarget = null;
        double minDistance = 1000.0; // 1000格限制

        for (AbstractClientPlayerEntity player : players) {
            if (player == client.player) continue; // 跳過自己

            Vec3d playerPos = player.getPos();
            double distance = arrowPos.distanceTo(playerPos);

            if (distance < minDistance) {
                // 在選擇目標前先檢查角度是否可射擊
                Vec3d targetPos = playerPos.add(0, 0.3, 0); // 瞄準胸部
                Vec3d targetDis = targetPos.subtract(arrowPos);
                double targetXZ = Math.sqrt(targetDis.x * targetDis.x + targetDis.z * targetDis.z);
                double targetPitch = Math.toDegrees(Math.atan2(-targetDis.y, targetXZ));

                // 只有在角度範圍內才考慮這個目標
                if (targetPitch <= -18) { // 可射擊範圍
                    nearestTarget = player;
                    minDistance = distance;
                }
            }
        }

        return nearestTarget;
    }

    private static void updatePlayerVelocities(MinecraftClient client) {
        List<AbstractClientPlayerEntity> players = client.world.getPlayers();

        for (AbstractClientPlayerEntity player : players) {
            Vec3d currentPos = player.getPos();
            Vec3d lastPos = lastPlayerPositions.get(player);

            if (lastPos != null) {
                // 計算速度 (position差 / 時間差，這裡是1 tick = 0.05秒)
                Vec3d velocity = currentPos.subtract(lastPos);
                playerVelocities.put(player, velocity);
            } else {
                // 第一次見到這個玩家，速度設為0
                playerVelocities.put(player, Vec3d.ZERO);
            }

            // 更新位置記錄
            lastPlayerPositions.put(player, currentPos);
        }

        // 清理已離線的玩家
        lastPlayerPositions.keySet().removeIf(player -> !players.contains(player));
        playerVelocities.keySet().removeIf(player -> !players.contains(player));
    }

    private static Vec3d getPlayerVelocity(PlayerEntity player) {
        Vec3d velocity = playerVelocities.get(player);
        return velocity != null ? velocity : Vec3d.ZERO;
    }

    // 命令接口：設定時間補償
    public static void setTimeCompensation(int ticks) {
        if (config == null) return;
        config.timeCompensation = ticks;
        config.save(); // 自動保存
        System.out.println("AutoAim: Time compensation set to " + ticks + " ticks");
    }

    // 獲取當前補償值
    public static int getTimeCompensation() {
        return config != null ? config.timeCompensation : 2;
    }

    // 命令接口：設定弓箭位置
    public static void setArrowPosition(double x, double y, double z) {
        if (config == null) return;
        config.arrowPosition.fromVec3d(new Vec3d(x, y, z));
        config.save(); // 自動保存
        System.out.println(String.format("AutoAim: Arrow position set to %.3f %.3f %.3f", x, y, z));
    }

    // 獲取當前弓箭位置
    public static Vec3d getArrowPosition() {
        return config != null ? config.arrowPosition.toVec3d() : new Vec3d(0, 0, 0);
    }

    // 初始化配置
    public static void initializeConfig() {
        config = AutoAimConfig.load();
        System.out.println("AutoAim: Config initialized");
        System.out.println("  Time compensation: " + config.timeCompensation + " ticks");
        System.out.println("  Arrow position: " + config.arrowPosition.x + " " + config.arrowPosition.y + " " + config.arrowPosition.z);
    }

    // 檢測網路延遲
    private static void checkNetworkLatency(MinecraftClient client) {
        try {
            if (client.getNetworkHandler() != null && client.player != null) {
                net.minecraft.client.network.PlayerListEntry playerEntry =
                        client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());

                if (playerEntry != null) {
                    int ping = playerEntry.getLatency(); // 延遲，單位：毫秒
                    int pingTicks = ping / 50; // 轉換為 tick（1 tick = 50ms）

                    // 只在延遲變化時更新
                    if (Math.abs(ping - currentPing) > 10) { // 變化超過10ms才更新
                        currentPing = ping;
                        lastPingUpdate = System.currentTimeMillis();

                        System.out.println("AutoAim: Network latency: " + ping + "ms (" + pingTicks + " ticks)");
                    }
                }
            }
        } catch (Exception e) {
            // 靜默處理錯誤，避免控制台垃圾訊息
        }
    }

    // 實時更新延遲（用於HUD顯示）
    private static void updateRealtimeLatency(MinecraftClient client) {
        try {
            if (client.getNetworkHandler() != null && client.player != null) {
                net.minecraft.client.network.PlayerListEntry playerEntry =
                        client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());

                if (playerEntry != null) {
                    currentPing = playerEntry.getLatency();
                }
            }
        } catch (Exception e) {
            // 靜默處理錯誤
        }
    }

    // 獲取當前延遲
    public static int getCurrentPing() {
        return currentPing;
    }

    // 獲取當前延遲（以tick為單位）
    public static int getCurrentPingTicks() {
        return currentPing / 50;
    }

    // 切換自動補償
    public static void toggleAutoCompensation() {
        autoCompensationEnabled = !autoCompensationEnabled;
        System.out.println("AutoAim: Auto compensation " + (autoCompensationEnabled ? "enabled" : "disabled"));
        if (autoCompensationEnabled) {
            System.out.println("AutoAim: Total compensation: " + getTotalCompensation() + " ticks");
        }
    }

    // 獲取自動補償狀態
    public static boolean isAutoCompensationEnabled() {
        return autoCompensationEnabled;
    }

    // 獲取總補償值（手動設定 + 網路延遲）
    public static int getTotalCompensation() {
        if (autoCompensationEnabled) {
            int manualCompensation = config != null ? config.timeCompensation : 2;
            int networkCompensation = getCurrentPingTicks();
            return manualCompensation + networkCompensation;
        } else {
            return config != null ? config.timeCompensation : 2;
        }
    }

    // 獲取自動瞄準狀態（供HUD使用）
    public static boolean isEnabled() {
        return enabled;
    }
}