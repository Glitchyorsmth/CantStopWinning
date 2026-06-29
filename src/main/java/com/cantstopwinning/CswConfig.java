package com.cantstopwinning;

import dev.anvil.api.config.Setting;
import dev.anvil.api.config.Validatable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CantStopWinning settings. Registered with {@code Framework.config()}; field initializers are the
 * defaults. {@link #validate()} repairs out-of-range values + the preset structure after every load.
 * {@link Setting} labels feed Anvil's auto config screen (used as the 26.x fallback).
 */
public final class CswConfig implements Validatable {

    public static final String DEFAULT_PRESET = "Default";
    private static final String DEFAULT_TRIGGER = "Wow! You dug out 1,000,000 coins!";

    @Setting(name = "Enabled", description = "Master toggle for the mod")
    public boolean enabled = true;

    @Setting(name = "Volume", description = "Celebration audio volume (0.0–1.0)")
    public double volume = 0.5;

    @Setting(name = "Overlay opacity", description = "Overlay transparency (0.0–1.0)")
    public double overlayOpacity = 1.0;

    // ── Trigger presets ───────────────────────────────────────────────────────────────────────
    /** Named trigger sets. Only {@link #activePreset}'s list fires. Order preserved for the UI. */
    public Map<String, List<String>> presets = new LinkedHashMap<>();

    /** Name of the preset currently in effect. */
    public String activePreset = DEFAULT_PRESET;

    // ── Corpse-loss detection ─────────────────────────────────────────────────────────────────
    @Setting(name = "Corpse detection", description = "SkyHanni corpse-loss alerts")
    public boolean corpseDetectionEnabled = true;

    @Setting(name = "Corpse mode", description = "When to fire the loss overlay")
    public CorpseMode corpseMode = CorpseMode.SIMPLE;

    @Setting(name = "Min loss (M)", description = "Set-amount mode: minimum loss in millions to fire")
    public double simpleAmountMillions = 1.0;

    @Setting(name = "Corpse threshold %", description = "Loss % to trigger (percentage mode)")
    public double corpseThreshold = 50.0;

    @Setting(name = "Umber key cost (M)", description = "Umber key price in millions")
    public double umberKeyCost = 1.5;

    @Setting(name = "Tungsten key cost (M)", description = "Tungsten key price in millions")
    public double tungstenKeyCost = 5.0;

    @Setting(name = "Vanguard key cost (M)", description = "Skeleton/Vanguard key price in millions")
    public double vanguardKeyCost = 30.0;

    // ── Helpers ───────────────────────────────────────────────────────────────────────────────

    /** @return the live trigger list of the active preset (never null). */
    public List<String> activeTriggers() {
        List<String> list = presets.get(activePreset);
        if (list == null) {
            list = new ArrayList<>();
            presets.put(activePreset, list);
        }
        return list;
    }

    @Override
    public void validate() {
        volume = Math.clamp(volume, 0.0, 1.0);
        overlayOpacity = Math.clamp(overlayOpacity, 0.0, 1.0);
        corpseThreshold = Math.clamp(corpseThreshold, 0.0, 100.0);
        simpleAmountMillions = Math.max(0.0, simpleAmountMillions);
        umberKeyCost = Math.max(0.0, umberKeyCost);
        tungstenKeyCost = Math.max(0.0, tungstenKeyCost);
        vanguardKeyCost = Math.max(0.0, vanguardKeyCost);
        if (corpseMode == null) {
            corpseMode = CorpseMode.SIMPLE;
        }

        // Always have at least the Default preset with the default trigger.
        if (presets == null) {
            presets = new LinkedHashMap<>();
        }
        presets.values().removeIf(list -> list == null);
        if (presets.isEmpty()) {
            List<String> def = new ArrayList<>();
            def.add(DEFAULT_TRIGGER);
            presets.put(DEFAULT_PRESET, def);
        }
        // Active preset must exist; fall back to the first one.
        if (activePreset == null || !presets.containsKey(activePreset)) {
            activePreset = presets.keySet().iterator().next();
        }
    }
}
