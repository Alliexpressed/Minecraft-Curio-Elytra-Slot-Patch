# Elytra Slot - Elytra Toggle compatibility patch

Replaces Elytra Slot's own elytra-detection logic with a wrapper that also checks the
Elytra Toggle mod's on/off state, without modifying a single line of Elytra Slot's original
compiled code.

## How it works

Elytra Slot finds its platform implementation (`NeoForgeElytraPlatform`) through Java's
`ServiceLoader` mechanism, and only ever calls it through the `IElytraPlatform` interface -
never the concrete class directly. This project provides an alternative implementation
(`ElytraToggleAwareElytraPlatform`) that:

- Delegates every method straight through to the real, untouched `NeoForgeElytraPlatform`.
- Except for `isEquipped`/`getEquipped`, which first check whether Elytra Toggle's
  `elytra_flight_enabled` attachment is `false` for that player - looked up purely by its
  registry name (a plain string), so this has no compile-time dependency on Elytra Toggle at
  all. If Elytra Toggle isn't installed, the attachment is never found and behavior is
  identical to the original.

The `patchedJar` Gradle task then takes the original jar, swaps in a `META-INF/services`
file pointing at our wrapper instead of the original class, and adds our one new compiled
class - everything else in the jar (all other classes, mixins, resources) is copied through
byte-for-byte.

## Building

```bash
gradle patchedJar
```

The output lands at `build/libs/elytraslot-neoforge-9.0.2+1.21.1-elytratoggle-patch.jar`.

## Installing

Replace the original `elytraslot-neoforge-9.0.2+1.21.1.jar` in your `mods/` folder (both
client and server) with this patched jar. Do not run both at once.

## Updating to a different Elytra Slot version

Drop the new jar in `libs/`, update `original_jar_name` (and `patched_jar_name`) in
`gradle.properties`, and rebuild. If Elytra Slot's `IElytraPlatform` interface or
`NeoForgeElytraPlatform` class ever change shape, `ElytraToggleAwareElytraPlatform.java` may
need updating to match.
