package com.cantstopwinning;

/**
 * What happens when importing a composite preset would overwrite a locally-existing preset with
 * different content (e.g. importing a "General" bundle that names a "Slayer" sub-preset you
 * already have, with different triggers).
 */
public enum ImportConfirmMode {
    /** Overwrite immediately; one chat line afterward summarizes what got overwritten. */
    WARN,
    /** Stage the overwrite and post a clickable chat confirm; expires (skipped) after 5s unmet. */
    CONFIRM
}
