package NJ.autoaim.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class AutoAimClient implements ClientModInitializer {
    public static KeyBinding toggleAutoAimKey;

    @Override
    public void onInitializeClient() {
        System.out.println("=== AutoAim Client Initializing ===");

        // 註冊按鍵綁定
        toggleAutoAimKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.autoaim.toggle",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_G,
                        "category.autoaim"
                )
        );
        System.out.println("AutoAim: Key binding registered");

        // 註冊命令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("autoaim")
                    .then(literal("compensation")
                            .then(argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-10, 10))
                                    .executes(context -> {
                                        int value = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value");
                                        AutoAimController.setTimeCompensation(value);
                                        context.getSource().sendFeedback(net.minecraft.text.Text.literal(
                                                "§a[AutoAim] Time compensation set to " + value + " ticks"));
                                        return 1;
                                    }))
                            .then(literal("auto")
                                    .executes(context -> {
                                        AutoAimController.toggleAutoCompensation();
                                        boolean enabled = AutoAimController.isAutoCompensationEnabled();
                                        int total = AutoAimController.getTotalCompensation();
                                        context.getSource().sendFeedback(net.minecraft.text.Text.literal(
                                                "§a[AutoAim] Auto compensation: " + (enabled ? "ON" : "OFF") +
                                                        (enabled ? " (Total: " + total + " ticks)" : "")));
                                        return 1;
                                    })))
                    .then(literal("position")
                            .then(argument("x", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                    .then(argument("y", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                            .then(argument("z", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                                    .executes(context -> {
                                                        double x = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "x");
                                                        double y = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "y");
                                                        double z = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "z");
                                                        AutoAimController.setArrowPosition(x, y, z);
                                                        context.getSource().sendFeedback(net.minecraft.text.Text.literal(
                                                                String.format("§a[AutoAim] Arrow position set to %.3f %.3f %.3f", x, y, z)));
                                                        return 1;
                                                    })))))
                    .then(literal("ping")
                            .executes(context -> {
                                int ping = AutoAimController.getCurrentPing();
                                int pingTicks = AutoAimController.getCurrentPingTicks();
                                context.getSource().sendFeedback(net.minecraft.text.Text.literal(
                                        "§a[AutoAim] Current ping: " + ping + "ms (" + pingTicks + " ticks)"));
                                return 1;
                            })));
        });
        System.out.println("AutoAim: Command registered");

        // 載入校準數據
        CalibrationData.loadCalibrationData();

        // 初始化配置
        AutoAimController.initializeConfig();

        // 註冊客戶端事件
        ClientTickEvents.END_CLIENT_TICK.register(AutoAimController::onClientTick);
        System.out.println("AutoAim: Client tick event registered");

        // 註冊HUD渲染
        HudRenderCallback.EVENT.register(AutoAimHUD::renderHUD);
        System.out.println("AutoAim: HUD render event registered");

        System.out.println("=== AutoAim Client Initialization Complete ===");
    }
}