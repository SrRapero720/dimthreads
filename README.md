# Dimensional Threading ~~Reforged~~ Reforked

Optimizes the processing of multiple Dimensions, by assigning them independent threads.
It works on both the **client and server**, and **does not** require the mod to be installed on both sides.

This is an unofficial fork of 
[DimensionalThreading-Reforged](https://github.com/CCr4ft3r/DimensionalThreading-Reforged) 
which is also an unofficial port of 
[DimensionalThreading](https://github.com/WearBlackAllDay/DimensionalThreading) 
by WearBlackAllDay.

## Changes:
- Fixed [#17](https://github.com/CCr4ft3r/DimensionalThreading-Reforged/issues/17)
- Ported to 1.20.1 and 1.16.5

This port should be fully compatible with Embeddium, Immersive Portals and Oculus
(at least no crashes/issues occurred while testing it).

---

# WIKI
**DimThread comes with 2 native Gamerules:**
- `/gamerule dimthread_active <true/false>` enables/disables the mod
- `/gamerule dimthread_thread_count <count>`changes the amount of threads used

---

# FAQ

### Does the mod change Vanilla behaviour?
DimThread aims to conserve vanilla-parity in all points. 
At the moment there are no known deviations in behaviour from Mojang's`server.jar`.
If you notice any, feel free to [open an issue.](https://github.com/CCr4ft3r/DimensionalThreading-Reforged/issues)

### Can my dimensions get de-synchronized?
As stated above, this is **NOT** the case.
DimThread will always synchronize the dimensions it threads with each other,
setting the overall MSPT to the slowest individual dimension.

### Are dimension counts above 3 supported?
Yes, but you will have to adjust the GameRule accordingly.

### Will the server run faster if assign more than 3 threads to DimThread?
No, the mod can only assign one dimension to one thread.

### Can i use DimThread if i have less threads on my CPU than dimensions?
The mod will not crash if you do not have enough threads available,
but it will make the game **slower**. You should always have 
at least`DimensionCount + 1`threads available.

### How is the compatibility with other mods?
Compatibility with [JellySquids](https://github.com/jellysquid3) performance mods and [Carpet](https://github.com/gnembon/fabric-carpet) will be
ensured and issues concerning them are accepted.
If you plan on using a different mod compatibility cannot be guaranteed,
since not every author writes their mod thread safe.

### What about older versions of Minecraft?
Supported versions are 1.16.5, 1.18.2, 1.19.2 and 1.20.1. Nothing else.

---