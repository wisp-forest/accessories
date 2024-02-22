package io.wispforest.accessories.api;

/**
 * DropRule class indicates the action to perform when dropping an Accessory like on death
 *
 * <p> {@link #KEEP}: Should always remain within the players inventory. </p>
 * <p> {@link #DROP}: Should always drop on the ground. </p>
 * <p> {@link #DESTROY}: Should always be destroyed. </p>
 * <p> {@link #DEFAULT}: Defers to vanilla handling of items. </p>
 */
public enum DropRule {
    KEEP,
    DROP,
    DESTROY,
    DEFAULT
}
