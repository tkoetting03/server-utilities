# Hologram Menu Mod

A Fabric mod for Minecraft **26.1.2** that adds:

- **Persistent text holograms** using Minecraft display entities
- **Hologram placement mode** — toggle with a keybind; any held item or fist places holograms
- **JSON-defined interactive menus** with server-validated actions
- **Container popup** in the bottom-left when viewing chests and other inventories
- **Client config file** for popup settings
- **Anvil text styling** with colors and formatting effects

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.3+
- Fabric API 0.152.1+26.1.2
- Java 25

## Hologram Placement Mode

Press **G** (default) to toggle placement mode on/off. While active:

- **Right-click** with any item or empty hand places a hologram where you aim (max **2 blocks**)
- Normal item use is blocked so your held item won't activate
- **Right-click an existing hologram** still opens the edit/delete menu

Default text is set in `config/hologrammenu-client.json` via `defaultPlacementText`.

## Commands

| Command | Description |
|---------|-------------|
| `/hologram create <text>` | Place a hologram in front of you |
| `/hologram remove` | Remove the nearest hologram |
| `/hologram list` | List nearby holograms |
| `/menu open <id>` | Open an interactive menu |

## Keybinds

| Key | Action |
|-----|--------|
| `G` | Toggle hologram placement mode |
| `H` | Toggle container popup |
| `M` | Open configured menu |

## Client Config

On first launch, the mod creates `config/hologrammenu-client.json`:

```json
{
  "showContainerPopup": true,
  "popupMenuId": "main",
  "defaultPlacementText": "Hologram"
}
```

| Option | Description |
|--------|-------------|
| `showContainerPopup` | Show the quick-menu popup in container inventories |
| `popupMenuId` | Menu opened by the popup button and `M` key |
| `defaultPlacementText` | Text used when placing holograms in placement mode |

Pressing `H` updates `showContainerPopup` and saves the file automatically.

## Custom Menus (JSON)

Add menu files under `data/<namespace>/menus/<id>.json` in a datapack or your mod resources.

```json
{
  "title": { "translate": "menu.example.title" },
  "buttons": [
    {
      "id": "say_hello",
      "label": { "text": "Say Hello" },
      "type": "message",
      "value": "Hello from a custom menu!"
    },
    {
      "id": "run_cmd",
      "label": { "translate": "menu.example.run" },
      "type": "command",
      "value": "say Hello"
    },
    {
      "id": "submenu",
      "label": { "text": "Open Tools" },
      "type": "open_menu",
      "value": "tools"
    },
    {
      "id": "close",
      "label": { "text": "Close" },
      "type": "close"
    }
  ]
}
```

**Button types:** `message`, `command`, `open_menu`, `close`

**Label/title text:** use `{ "translate": "key" }` or `{ "text": "Literal" }`

Run `/reload` to reload menus from datapacks.

## Anvil Styling

Open an anvil and click **Style** to pick colors and text effects for the rename field.

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
./gradlew build
```

The built jar is in `build/libs/`.

## License

MIT
