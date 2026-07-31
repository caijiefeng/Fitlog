package com.example.fitlog.domain.avatar

/**
 * How the user's profile avatar is sourced.
 *
 * Persisted as its [name] in the `user_profiles.avatar_type` column.
 */
enum class AvatarType {
    /** One of the built-in sports-style cartoon avatars (drawable). */
    BUILT_IN,

    /** A photo the user picked from the phone; [UserProfile.customAvatarPath] points at it. */
    CUSTOM,

    /** No avatar chosen yet — the UI falls back to a person placeholder. */
    DEFAULT,
}
