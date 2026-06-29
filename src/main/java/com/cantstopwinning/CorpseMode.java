package com.cantstopwinning;

/**
 * When the loss overlay should fire for a SkyHanni corpse-profit message.
 *
 * <ul>
 *   <li>{@link #SIMPLE} — any negative profit triggers it.</li>
 *   <li>{@link #PERCENTAGE} — only when the loss exceeds a configured % of the key cost.</li>
 * </ul>
 */
public enum CorpseMode {
    SIMPLE,
    PERCENTAGE
}
