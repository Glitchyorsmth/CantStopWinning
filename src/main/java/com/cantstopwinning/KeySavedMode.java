package com.cantstopwinning;

/**
 * What to do when Hypixel's Resourceful-perk message confirms a corpse key wasn't consumed. The
 * loss overlay is suppressed for that corpse in every mode except {@link #OFF} — a saved key means
 * zero coins were spent, so it can never be a real loss regardless of what SkyHanni's profit line
 * (which doesn't account for the save) reports.
 *
 * <ul>
 *   <li>{@link #OFF} — ignore the message; loss fires purely off SkyHanni's profit number.</li>
 *   <li>{@link #SUPPRESS} — suppress the loss overlay for that corpse, no extra feedback.</li>
 *   <li>{@link #CELEBRATE} — suppress the loss overlay and also fire the win celebration.</li>
 * </ul>
 */
public enum KeySavedMode {
    OFF,
    SUPPRESS,
    CELEBRATE
}
