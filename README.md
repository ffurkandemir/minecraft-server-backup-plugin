# Server Backup Plugin v2.0.0

An advanced backup system for Minecraft 1.21.1 servers with powerful integrations and smart rollback capabilities.

## 🚀 Features

### Core Features
- **Automatic & Manual Backups**: Schedule automatic backups or create on-demand
- **Async Processing**: Non-blocking backup operations won't lag your server
- **Smart Compression**: ZIP compression with locked file handling
- **World & Full Backups**: Backup worlds only or include plugins
- **Auto-save Management**: Temporarily disables auto-save during backup to prevent file locks
- **Disk Space Monitoring**: Automatic checks to prevent disk full errors
- **Backup Rotation**: Auto-delete old backups based on configurable limits

### 🔌 Advanced Integrations

#### CoreProtect Integration
- **Smart Rollback System**: 3-tier intelligent grief detection
  - Large Grief (1000+ blocks): Creates full backup before rollback
  - Medium Grief (100-999 blocks): Creates world backup before rollback  
  - Small Grief (<100 blocks): Direct rollback without backup
- **Automatic Recovery**: Failed rollbacks auto-restore from backup
- **Performance Optimized**: Background processing for large operations

#### LuckPerms Integration
- **Rate Limiting**: Per-group backup quotas (configurable)
- **Permission-based Access**: Fine-grained control over backup operations
- **Quota Tracking**: Hourly reset system with usage warnings

#### PlaceholderAPI Integration
- `%serverbackup_last_backup%` - Time since last backup
- `%serverbackup_backup_count%` - Total backup count
- `%serverbackup_total_size%` - Total backup size
- `%serverbackup_next_auto%` - Next auto-backup time

#### WorldGuard Integration (Planned)
- Region-specific backups
- Protected area restoration

### 🌐 Network Mode (BungeeCord/Velocity)
- **Centralized Backup Orchestration**: Trigger backups across all servers
- **Network-wide Coordination**: Synchronized backup sessions
- **Plugin Messaging**: Seamless communication between servers

## 📋 Commands

### Basic Commands
- `/backup` or `/backup world` - Create world backup (default)
- `/backup full` - Create full backup (worlds + plugins)
- `/backup now` - Alias for default backup
- `/backup auto` - Toggle automatic backups on/off
- `/backup info` or `/backup status` - Show plugin status & integrations

### Management Commands
- `/backuplist` or `/bl` - List all backups with sizes and dates
- `/backupsize` or `/bsize` - Show backup statistics and disk usage
- `/backuprestore <backup-name>` - Get restore instructions
- `/backupdelete <backup-name>` - Delete a specific backup

### Advanced Commands
- `/smartrollback <time> [user] [radius]` - Smart CoreProtect rollback with auto-backup
  - Aliases: `/sr`, `/srollback`
  - Example: `/sr 30m Player123 50` - Rollback Player123 within 50 blocks for last 30 minutes

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `serverbackup.*` | Access to all commands | op |
| `serverbackup.backup` | Create backups | op |
| `serverbackup.list` | List and view backup info | op |
| `serverbackup.restore` | View restore instructions | op |
| `serverbackup.delete` | Delete backups | op |
| `serverbackup.smartrollback` | Use smart rollback with CoreProtect | op |

## 📦 Installation

1. Download the latest JAR from [Releases](../../releases)
2. Place in your server's `plugins/` folder
3. **(Optional)** Install integrations: CoreProtect, LuckPerms, PlaceholderAPI
4. Restart your server
5. Configure `plugins/ServerBackupPlugin/config.yml`

## 🔧 Building from Source

```bash
# Clone repository
git clone https://github.com/ffurkandemir/minecraft-server-backup-plugin.git
cd minecraft-server-backup-plugin

# Build with Maven
mvn clean package -DskipTests

# JAR will be in target/server-backup-plugin-1.0.0.jar
```

## ⚙️ Configuration

Key configuration options in `config.yml`:

```yaml
backup:
  directory: "backups"
  auto-backup-enabled: false
  auto-backup-interval: 720  # minutes (12 hours)
  max-backups: 10
  compress: true
  min-free-space-gb: 5
  worlds: []  # Empty = all worlds

integrations:
  coreprotect:
    enabled: false
    backup-thresholds:
      large: 1000   # blocks
      medium: 100   # blocks
  
  luckperms:
    enabled: false
    rate-limits:
      default: 5    # per hour
      vip: 10
      admin: 20
  
  placeholderapi:
    enabled: false
  
  network:
    enabled: false
    mode: "disabled"  # or "bungee" / "velocity"
```

## 🎯 Use Cases

### Single Server Setup
1. Enable auto-backup for daily protection
2. Use `/backup full` before major updates
3. Monitor with `/backupsize` to track disk usage

### CoreProtect Integration
1. Enable CoreProtect integration in config
2. Use `/smartrollback` for automatic backup before rollback
3. Large griefs auto-create full backups for safety

### Network Setup (BungeeCord/Velocity)
1. Enable network mode on coordinator server
2. Install plugin on all backend servers
3. Trigger network-wide backups from coordinator

## 📊 Statistics Example

```
/backupsize output:

Total Backups: 8
Total Size: 2.4 GB
Average Size: 307.2 MB

Largest: backup-2025-12-07_14-30-00.zip (512.5 MB)
Smallest: backup-2025-12-06_02-00-00.zip (198.3 MB)

Disk Usage:
  Free: 45.2 GB
  Used: 114.8 GB (71.8%)
```

## 🔄 Version History

### v2.0.0 (Current)
- ✅ Added CoreProtect smart rollback integration
- ✅ Added LuckPerms rate limiting
- ✅ Added PlaceholderAPI support
- ✅ Added network orchestrator for BungeeCord/Velocity
- ✅ Added `/backupsize` command with statistics
- ✅ Added `/backup info` status display
- ✅ Fixed async world save issues
- ✅ Fixed file locking during backup
- ✅ Improved backup list with numbering and totals

### v1.0.0
- Initial release with basic backup functionality

## 📋 Requirements

- **Minecraft**: 1.21.1 (Paper/Spigot)
- **Java**: 21 or higher
- **Optional Dependencies**:
  - CoreProtect 22.2+ (for smart rollback)
  - LuckPerms 5.4+ (for rate limiting)
  - PlaceholderAPI 2.11+ (for placeholders)
  - WorldGuard 7.0+ (planned)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is open source and available under the MIT License.

## 💬 Support

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)
- **Wiki**: [Documentation Wiki](../../wiki)
