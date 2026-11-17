# Art and audio credits

Aetheria uses a shared palette of ink navy, ivory, teal, amber and lilac, with distinct silhouettes for Astra, Nyx, Orion and the hostile families.

## Gameplay visuals

Characters, equipment, food/material icons, hearts, shuttle and terrain are original drawings generated with Java2D. The source lives in `OriginalArt`, `TerrainArt` and `PixelArt`; `SceneRenderer` adds world details and attack indicators, and `ParticleSystem` draws sparks, rings and floating labels.

Five regions share four terrain styles: Starfall Glade, Verdant Grove, Tidal Gardens, Ember Ruins and Eclipse Reach. The HUD, inventory, crafting, achievements and map are also drawn in Java.

Some historical asset filenames remain internal aliases. They resolve to the original generated artwork rather than borrowed character images.

## AI-generated title illustration

The [title background](../src/art/starfall-title.png) was created with an AI image-generation tool. The exact prompt is preserved in [TITLE_ART_PROMPT.txt](TITLE_ART_PROMPT.txt).

It depicts an alien valley, explorer and shuttle. Java adds the title, menu text and controls over the image. The prompt uses the earlier working title, Starfall Protocol; the illustration itself contains no title text. This is a bundled static image, so playing requires no image-generation service or network connection.

## Sound

`SoundEngine` synthesizes short event cues with JavaSound. There are no downloaded recordings or music packs. Mute is available with M, and gameplay continues silently when an audio device cannot be opened.