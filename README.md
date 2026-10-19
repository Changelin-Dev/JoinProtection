<div align="center">

[![GitHub release](https://img.shields.io/github/v/release/<your-github-username>/JoinProtection?style=for-the-badge&labelColor=29355F&color=009B98)](https://github.com/<your-github-username>/JoinProtection/releases)
![Minecraft Versions](https://img.shields.io/badge/minecraft-26.2-009B98?style=for-the-badge&logoColor=blue&labelColor=29355F)
![Java](https://img.shields.io/badge/java-25+-009B98?style=for-the-badge&logoColor=blue&labelColor=29355F)

# JoinProtection

A plugin that protects players from mobs and damage for a set period of time after they join\
...with many configurable options.

*Fork of [rockquiet/JoinProtection](https://github.com/rockquiet/JoinProtection) for Paper 26.2.*

</div>

## Features

- After joining the server the player is invincible for X seconds
    - Protection time can be extended via permission
- **Bossbar showing the remaining protection time**
- **Chat message at the start of the protection** (explains that attacking a player cancels it)
- **Invisibility during the protection**
- Toggle if entities target players with active join protection
- Disable damage...
    - ...by entities
    - ...by blocks
    - ...by anything else (fall damage, etc.)
- Prevent the player from dropping/picking up items
- Cancel the join protection if the player...
    - ...attacks an entity (melee or projectile)
    - ...moves X blocks
    - ...breaks/places blocks
- Particles aura during active join protection
- Play a sound to players trying to attack others with active join protection
- Whitelist/Blacklist worlds to restrict the plugin's functionality
- [PlaceholderAPI](https://www.spigotmc.org/resources/6245)
  and [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) support
- [LuckPerms Context](https://luckperms.net/wiki/Context) support
- Everything can be edited, bypassed with permissions, or completely disabled

## Changelog

### v2.0.0

#### ✨ Nouveautés

* Bossbar de protection : le temps restant avant la fin de la protection s'affiche en haut de l'écran et se vide au fil des secondes. Elle disparaît à la fin ou à l'annulation de la protection. Titre, couleur et style configurables dans `boss-bar`.
* Message de début de protection dans le tchat : le joueur est prévenu qu'il est protégé et que frapper un autre joueur lui fait perdre sa protection. Modifiable dans `messages.protection-start`.
* Invisibilité pendant la protection : l'effet est appliqué sans particules ni icône, et retiré à la fin, à l'annulation, à la mort, à la déconnexion et à l'arrêt du plugin. L'objet tenu en main reste visible. Désactivable via `invisibility.enabled`.

#### 🔧 Améliorations

* Compatibilité Paper 26.2 (Java 25, `api-version: '26.2'`).
* Le plugin utilise désormais Adventure et MiniMessage de Paper, sans les embarquer. Spigot n'est plus supporté.
* Les projectiles (flèches, tridents, boules de neige, etc.) lancés par un joueur protégé font maintenant aussi perdre sa protection.
* Les vérifications de mise à jour sont désactivées par défaut.
* Les principaux messages sont maintenant en français par défaut.

#### 🐛 Corrections

* Fix : la bossbar et l'invisibilité ne restent plus actives après une annulation de protection, une mort, une déconnexion ou un rechargement du plugin.

#### ⚠️ Mise à jour depuis une 1.x

Les nouvelles options sont ajoutées automatiquement dans le `config.yml`. La section `messages` est réinitialisée par la migration et doit être reconfigurée.

#### 📦 Installation

Téléchargez `JoinProtection.jar` ci-dessous et placez-le dans le dossier `plugins/` de votre serveur Paper 26.2.

## Commands & Permissions

| command                  | permission              | description        |
|--------------------------|-------------------------|--------------------|
| `/joinprotection reload` | `joinprotection.reload` | Reloads the config |

| permission                                       | default | description                                                                                                                              |
|--------------------------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------|
| `joinprotection.use`                             | true    | If the Player has join protection                                                                                                        |
| `joinprotection.reload`                          | op      | Allows the usage of `/joinprotection reload`                                                                                             |
| `joinprotection.plus-NUMBER`                     | false   | Extends the protection time by NUMBER amount of seconds<br/>(If a player has multiple permissions, only the highest value will be added) |
| `joinprotection.bypass.cancel-on-move`           | false   | Bypass the cancel on move option                                                                                                         |
| `joinprotection.bypass.cancel-on-attack`         | false   | Bypass the cancel on attack option                                                                                                       |
| `joinprotection.bypass.cancel-on-block-interact` | false   | Bypass the cancel on block interact option                                                                                               |
| `joinprotection.bypass.world-list`               | false   | Bypass the world-list option                                                                                                             |
| `joinprotection.bypass.*`                        | false   | Grants all bypass permissions                                                                                                            |
| `joinprotection.*`                               | false   | Grants all permissions of the plugin                                                                                                     |

## Placeholders

| [PlaceholderAPI](https://www.spigotmc.org/resources/6245) | [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) | description                     |
|-----------------------------------------------------------|------------------------------------------------------------------|---------------------------------|
| `%joinprotection_status%`                                 | `<joinprotection_status>`                                        | Protection status of the player |

## LuckPerms Context

The plugin updates an `joinprotection` context for each player:\
It can be either `true` (player is protected) or `false` (player is not protected).

More information on how it works is available on the [LuckPerms Wiki](https://luckperms.net/wiki/Context).

## Download

Plugin jars are available in the [GitHub releases](https://github.com/<your-github-username>/JoinProtection/releases) section.

## Credits

- Original plugin by [rockquiet](https://github.com/rockquiet/JoinProtection), released under the MIT License.
- The v2.0.0 additions of this fork (bossbar, start message, invisibility, projectile attack cancel and the Paper 26.2 port) were partly coded with the help of AI (Claude, by Anthropic).

## Metrics

This plugin uses bStats to collect some (non-identifying) data about the servers it runs on.
You can opt out by editing the `config.yml` in the `/plugins/bStats` folder located in your server directory.
(More information [here](https://bstats.org/getting-started))

[<img alt="bstats" src="https://bstats.org/signatures/bukkit/JoinProtection.svg">](https://bstats.org/plugin/bukkit/JoinProtection)
