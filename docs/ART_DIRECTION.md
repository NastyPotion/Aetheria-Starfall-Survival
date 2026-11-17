# Art and audio credits

Aetheria uses a shared palette of ink navy, ivory, teal, amber and lilac, with distinct silhouettes for Astra, Nyx, Orion and the hostile families.

## Gameplay visuals

Characters, equipment, food/material icons, hearts, shuttle and terrain are original drawings generated with Java2D. The source lives in `OriginalArt`, `TerrainArt` and `PixelArt`; `SceneRenderer` adds world details and attack indicators, and `ParticleSystem` draws sparks, rings and floating labels.

Five regions share four terrain styles: Starfall Glade, Verdant Grove, Tidal Gardens, Ember Ruins and Eclipse Reach. The HUD, inventory, crafting, achievements and map are also drawn in Java.

Some historical asset filenames remain internal aliases. They resolve to the original generated artwork rather than borrowed character images.


## Sound

`SoundEngine` synthesizes short event cues with JavaSound. There are no downloaded recordings or music packs. Mute is available with M, and gameplay continues silently when an audio device cannot be opened.
