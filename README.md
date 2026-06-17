# Mouse Lock / Ungrab with inventory preview.

Fabric 1.21.11 client-side mod.

## Usage

Toggle **Free Mouse Lock** from Minecraft's normal keybind menu in miscellaneous.

Default Free Mouse Lock bind: **Mouse Button 4**.

Optional inventory viewer default bind: **I**.

Optional inventory viewer position editor: **unbound by default**.

You can rebind it in:

```text
Options > Controls > Key Binds > Miscellaneous
```

It can be bound to normal keyboard keys or mouse buttons like:

- Middle Click
- Mouse Button 4
- Mouse Button 5

When active:

- the mouse cursor stays released/ungrabbed from the Minecraft window
- the live multiplayer game screen stays active instead of opening a pause menu
- yaw and pitch stay locked to the angle from when you toggled it on
- keyboard movement works only while Minecraft is focused and no GUI/screen is open
- GUIs/inventory/chat/menus remain usable and do not disable the mode
- if Minecraft loses focus, the mode stays on but movement/use/attack inputs are released
- when you refocus Minecraft, the cursor is still free and the locked-view state resumes

The optional inventory viewer is a read-only HUD overlay. It shows your own
hotbar, main inventory, armor, and offhand using Minecraft's normal item
rendering. It does not open a menu, capture the mouse, move items, click slots,
drop items, send packets, or automate gameplay.

The inventory viewer position is saved in:

```text
config/free_mouse_locked_view.json
```

Bind **Edit Inventory Viewer Position** in Controls to move the HUD in game.
The editor is a non-pausing screen where you can drag the panel, nudge it with
arrow keys, hold Shift for larger nudges, reset to the default top-right
position, and close with Done or Escape.

## Controls

- Free Mouse Lock default: Mouse Button 4
- Inventory Viewer default: I
- Inventory Viewer Position Editor default: unbound
- Both are rebindable in Minecraft's Controls menu
- Free Mouse Lock supports mouse buttons such as middle click, Mouse Button 4, Mouse Button 5, and keyboard keys

## Hypixel/SkyBlock safety design

This mod intentionally does **not**:

- press keys for you
- click for you
- rotate, aim, snap, or track anything for you
- keep moving while Minecraft is unfocused
- keep using/attacking while Minecraft is unfocused
- send custom packets
- send inventory/action packets
- change movement physics
- reveal hidden info or change perspective
- move, rearrange, drop, use, or edit inventory items

It only releases the local cursor, prevents mouse-look while active, and can
render a read-only HUD view of inventory data the client already has.

## Use at your own risk on Hypixel

Hypixel says all modifications are used at your own risk. This mod is designed to avoid automation and obvious unfair-advantage behavior, but no third-party mod can be guaranteed safe by anyone except Hypixel.

## Build

Requirements:

- Java 21+
- Gradle 9.2+

From the project folder:

```bash
gradle build
```

The jar will be in:

```text
build/libs/free-mouse-locked-view-1.1.0.jar
```

Install with:

- Minecraft 1.21.11
- Fabric Loader 0.18.5+
- Fabric API 0.141.4+1.21.11

Put the built jar in your instance `mods` folder.
