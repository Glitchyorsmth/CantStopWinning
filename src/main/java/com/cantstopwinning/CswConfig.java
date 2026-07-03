package com.cantstopwinning;

import dev.anvil.api.config.Setting;
import dev.anvil.api.config.Validatable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    /**
     * Preset name → names of other presets it includes. An included preset's triggers fire
     * whenever the including preset is active, live (edit the included preset, every preset that
     * includes it updates too — nothing is copied). Only presets with empty includes of their own
     * are eligible to be included (no nesting, so cycles are structurally impossible).
     */
    public Map<String, List<String>> presetIncludes = new LinkedHashMap<>();

    @Setting(name = "Import confirm", description = "Composite import overwriting an existing preset: warn only, or click-to-confirm")
    public ImportConfirmMode importConfirmMode = ImportConfirmMode.CONFIRM;

    // ── Corpse-loss detection ─────────────────────────────────────────────────────────────────
    @Setting(name = "Corpse detection", description = "SkyHanni corpse-loss alerts")
    public boolean corpseDetectionEnabled = true;

    @Setting(name = "Corpse mode", description = "When to fire the loss overlay")
    public CorpseMode corpseMode = CorpseMode.SIMPLE;

    @Setting(name = "Key-saved feedback", description = "Resourceful perk saved your key: off / suppress the loss overlay / suppress + celebrate")
    public KeySavedMode keySavedMode = KeySavedMode.SUPPRESS;

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

    /** @return the live trigger list of the active preset's own triggers (never null). */
    public List<String> activeTriggers() {
        List<String> list = presets.get(activePreset);
        if (list == null) {
            list = new ArrayList<>();
            presets.put(activePreset, list);
        }
        return list;
    }

    /**
     * @return the active preset's own triggers plus every included preset's triggers, deduplicated
     *         and in stable order — what {@code CswChat} actually matches chat against. Editing
     *         should go through {@link #activeTriggers()} (own only); this is read-only/derived.
     */
    public List<String> effectiveTriggers() {
        List<String> own = activeTriggers();
        List<String> includes = presetIncludes.get(activePreset);
        if (includes == null || includes.isEmpty()) {
            return own;
        }
        LinkedHashSet<String> resolved = new LinkedHashSet<>(own);
        for (String name : includes) {
            List<String> included = presets.get(name);
            if (included != null) {
                resolved.addAll(included);
            }
        }
        return new ArrayList<>(resolved);
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
        if (keySavedMode == null) {
            keySavedMode = KeySavedMode.SUPPRESS;
        }
        if (importConfirmMode == null) {
            importConfirmMode = ImportConfirmMode.CONFIRM;
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

        // Includes: drop dangling entries (deleted owner or deleted/self/nested target).
        if (presetIncludes == null) {
            presetIncludes = new LinkedHashMap<>();
        }
        presetIncludes.keySet().retainAll(presets.keySet());
        for (Map.Entry<String, List<String>> e : presetIncludes.entrySet()) {
            String owner = e.getKey();
            List<String> list = e.getValue();
            if (list == null) {
                e.setValue(new ArrayList<>());
                continue;
            }
            list.removeIf(name -> name.equals(owner)
                    || !presets.containsKey(name)
                    || !presetIncludes.getOrDefault(name, List.of()).isEmpty());
        }
    }
}
