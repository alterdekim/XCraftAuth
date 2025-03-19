# 🔐 XCraftAuth – Yggdrasil Authentication

![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.awain.net%2Fjob%2FXCraftAuth%2F)
![Gitea Last Commit](https://img.shields.io/gitea/last-commit/alterwain/XCraftAuth?gitea_url=https%3A%2F%2Fgitea.awain.net%2F)


A powerful **custom session server** plugin for Spigot-based servers, allowing full **Yggdrasil authentication** support across **1.8-1.16.5 Minecraft versions**! This plugin enables true **online mode authentication** with a self-hosted session server, providing flexibility for your Minecraft network.

**THIS PLUGIN ONLY WORKS ON SERVERS THAT RUN UNDER JAVA 8, NEWER VERSIONS OF JAVA WILL BREAK THIS PLUGIN**

**🌟 No need for AuthMe!** This plugin completely replaces AuthMe by handling authentication through a custom session server.

**🎨 Built-in skin & cape support!** Players can edit their skins and capes without needing Mojang's servers.

---

## ✨ Features

✅ **Supports all modern Minecraft versions** – From 1.8 to the 1.16.5 release.  
✅ **Compatible with all Bukkit/Spigot-based server software**, including:
- **CraftBukkit**
- **Spigot**
- **Paper**
- **CatServer** (Forge + Bukkit)
- **Magma** (Forge + Bukkit)
- **Mohist** (Forge + Bukkit)  

✅ **Full Yggdrasil authentication** – Works with your custom authentication system.  
✅ **Self-hosted session server** – No need for Mojang's authentication services.  
✅ **Seamless integration** – Works as a drop-in replacement for online mode.  
✅ **Replaces AuthMe** – No need for password-based authentication plugins.  
✅ **Custom skin & cape support** – Change your skin and cape at any time!  
✅ **Customizable** – Modify authentication endpoints to fit your needs.

---

## 📥 Installation

1. **Download the latest release** from [Jenkins](https://jenkins.awain.net/job/XCraftAuth/lastSuccessfulBuild/).
2. **Place the JAR file** in your `plugins` folder.
3. **Restart your server** to generate the configuration file.
4. **Configure your public domain and public port (for reverse proxy setup)** in the config file (`config.yml`).
5. **Restart your server** to apply changes.

---

## 🎨 Skin & Cape Editing

If you use this plugin, **players can change their skins and capes** freely! The skin system is independent of Mojang’s servers and fully integrated with the custom session server. The only thing you need is XCraft launcher

---

## ⚙️ Configuration

Edit `plugins/XCraftAuth/config.yml`:

```yaml
public_domain: "localhost" # Should be the same as the domain of your minecraft server
public_port: 8999 # Reverse proxy port (leave same if there's no reverse proxy)
internal_port: 8999 # Session server port
use_https: false # Set true if you're using reverse proxy setup
```

---

## 🚀 Usage

Once installed and configured, your server will authenticate players using your **custom session server**, just like Mojang’s online mode. Players must authenticate via your system before joining.

---

## ❓ FAQ

**Q: Can this replace Mojang's authentication system?**  
A: Yes! This plugin enables **custom online mode authentication**, independent of Mojang’s servers.

**Q: Does this work with cracked clients?**  
A: Yes, but **only** when using the **XCraft launcher**, which integrates with the custom session system.

**Q: Can players edit their skins and capes?**  
A: Yes! With this plugin, players can change their skins and capes without Mojang’s servers.

**Q: Can this replace AuthMe?**  
A: Yes! Since this plugin uses a session server for authentication, you **do not need AuthMe**.

---

## 📜 License

This project is **open-source** and licensed under the **MIT License**.


