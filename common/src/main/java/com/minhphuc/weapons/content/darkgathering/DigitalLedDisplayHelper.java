package com.minhphuc.weapons.content.darkgathering;

import com.minhphuc.weapons.mixin.DisplayAccessor;
import com.minhphuc.weapons.mixin.TextDisplayAccessor;
import com.mojang.math.Transformation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Quản lý các đồng hồ LED kỹ thuật số 3D hiển thị thời gian nghịch (-0.04, -0.02, 0.00)
 * Bám sát 100% hình ảnh Manga Dark Gathering (Chapter 83):
 * - Phông chữ LED 7 đoạn phát sáng trắng rực rỡ (Pure White Emissive).
 * - Nền hộp chữ nhật đen tuyền (Pitch Black Box 0xFF000000).
 * - Luôn xoay mặt về camera người xem (Billboard CENTER).
 */
public class DigitalLedDisplayHelper {

    public static class ActiveLedText {
        public final ServerLevel level;
        public final Display.TextDisplay display;
        public int ticksRemaining;

        public ActiveLedText(ServerLevel level, Display.TextDisplay display, int ticksRemaining) {
            this.level = level;
            this.display = display;
            this.ticksRemaining = ticksRemaining;
        }

        public void discard() {
            if (display != null && display.isAlive()) {
                display.discard();
            }
        }
    }

    private static final List<ActiveLedText> ACTIVE_LEDS = new ArrayList<>();

    /**
     * Triệu hồi chữ số LED 3D tại tọa độ chỉ định
     */
    public static Display.TextDisplay spawnLedText(ServerLevel level, Vec3 pos, String text, float scale, int lifetimeTicks) {
        Display.TextDisplay textDisplay = EntityType.TEXT_DISPLAY.create(level);
        if (textDisplay == null) return null;

        textDisplay.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);

        DisplayAccessor dispAcc = (DisplayAccessor) textDisplay;
        dispAcc.weapons$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        dispAcc.weapons$setViewRange(64.0F);
        dispAcc.weapons$setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        ));

        TextDisplayAccessor textAcc = (TextDisplayAccessor) textDisplay;
        // Text màu trắng sáng đậm kiểu LED 7-segment
        textAcc.weapons$setText(Component.literal("§f§l" + text));
        // Hộp nền đen tuyền (Black 0xFF000000) như ảnh manga
        textAcc.weapons$setBackgroundColor(0xFF000000);

        level.addFreshEntity(textDisplay);

        ACTIVE_LEDS.add(new ActiveLedText(level, textDisplay, lifetimeTicks));
        return textDisplay;
    }

    /**
     * Cập nhật đếm lùi thời gian tồn tại và dọn dẹp các LED hết hạn (Tối ưu 0 lag khi rỗng)
     */
    public static void tickLeds(ServerLevel level) {
        if (ACTIVE_LEDS.isEmpty()) return;

        Iterator<ActiveLedText> iterator = ACTIVE_LEDS.iterator();
        while (iterator.hasNext()) {
            ActiveLedText led = iterator.next();
            if (led.level == level) {
                led.ticksRemaining--;
                if (led.ticksRemaining <= 0 || !led.display.isAlive()) {
                    led.discard();
                    iterator.remove();
                }
            }
        }
    }

    public static void remove(Display.TextDisplay display) {
        if (display == null || ACTIVE_LEDS.isEmpty()) return;
        Iterator<ActiveLedText> iterator = ACTIVE_LEDS.iterator();
        while (iterator.hasNext()) {
            ActiveLedText led = iterator.next();
            if (led.display == display) {
                led.discard();
                iterator.remove();
                break;
            }
        }
    }

    public static void clearAll() {
        for (ActiveLedText led : ACTIVE_LEDS) {
            led.discard();
        }
        ACTIVE_LEDS.clear();
    }
}

