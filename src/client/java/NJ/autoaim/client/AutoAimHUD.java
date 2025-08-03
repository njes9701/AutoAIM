// AutoAimHUD.java - HUD顯示
package NJ.autoaim.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class AutoAimHUD {

    private static long lastUpdateTime = 0;

    public static void renderHUD(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!AutoAimController.isEnabled() || client.player == null) {
            return;
        }

        // 每 10 tick (0.5秒) 更新一次 actionbar，避免刷屏
        long currentTime = client.world.getTime();
        if (currentTime - lastUpdateTime >= 10) {
            lastUpdateTime = currentTime;

            // 獲取當前數據
            int ping = AutoAimController.getCurrentPing();
            int pingTicks = AutoAimController.getCurrentPingTicks();
            int totalCompensation = AutoAimController.getTotalCompensation();
            boolean autoMode = AutoAimController.isAutoCompensationEnabled();

            // 在 actionbar 顯示信息
            String hudText = String.format("§a[AutoAim] §fPing: %dms (%dt) | Comp: %dt%s",
                    ping, pingTicks, totalCompensation, autoMode ? " (Auto)" : "");

            client.player.sendMessage(Text.literal(hudText), true); // true = actionbar
        }

        // 依然繪製一個小的狀態指示框在左上角
        context.fill(5, 5, 15, 15, 0xFF00FF00); // 綠色小方塊表示 AutoAim 開啟
    }
}