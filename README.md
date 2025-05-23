# InventoryWhitelistPlugin

一个为 Multiverse-Inventories 插件添加白名单功能的 Minecraft 插件，可以控制玩家在切换世界时能够携带的物品。

## 功能特性

- 🎒 **物品白名单**: 创建自定义物品组，控制玩家在世界间切换时可携带的物品
- 🌍 **世界组支持**: 与 Multiverse-Inventories 完美集成
- ⚡ **实时管理**: 支持热重载配置，无需重启服务器
- 📋 **Tab 补全**: 完整的命令补全支持

## 依赖

- **Minecraft**: 1.21+
- **Paper/Spigot**: 1.21+
- **Multiverse-Inventories**: 5.0.0+

## 安装

1. 确保服务器已安装 Multiverse-Inventories 插件
2. 下载最新版本的 InventoryWhitelistPlugin
3. 将插件文件放入服务器的 `plugins` 文件夹
4. 重启服务器

## 使用方法

### 命令

- `/mvi create <组名>` - 创建一个新的白名单组
- `/mvi add <组名> <物品名称|hand>` - 向组中添加物品
- `/mvi remove <组名> <物品名称|hand>` - 从组中移除物品
- `/mvi list [组名]` - 列出所有组或指定组的物品
- `/mvi delete <组名>` - 删除一个白名单组
- `/mvi reload` - 重新加载插件配置

### 权限

- `inventorywhitelist.custom` - 允许管理白名单（默认：OP）
- `inventorywhitelist.bypass` - 绕过白名单限制（默认：无）

### 配置示例

```yaml
custom-groups:
  tools:
    - "minecraft:diamond_pickaxe"
    - "minecraft:diamond_axe"
    - "minecraft:diamond_shovel"
  food:
    - "minecraft:bread"
    - "minecraft:cooked_beef"
    - "minecraft:golden_apple"
```

## 开发

### 构建项目

```bash
mvn clean package
```

### 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── io/github/begonia599/inventoryWhitelistPlugin/
│   │       ├── InventoryWhitelistPlugin.java
│   │       ├── CustomWhitelistCommand.java
│   │       └── CustomWhitelistTabCompleter.java
│   └── resources/
│       └── plugin.yml
└── test/
```

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 更新日志

### v1.0.0
- 初始版本发布
- 支持自定义物品白名单组
- 完整的命令系统和权限控制
