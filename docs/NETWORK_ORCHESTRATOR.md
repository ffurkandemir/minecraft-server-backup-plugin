# Network Orchestrator - Multi-Server Backup System

The Network Orchestrator allows you to coordinate backups across multiple Spigot/Paper servers from a single BungeeCord or Velocity proxy.

## Architecture

```
┌─────────────────────────────────────────────────┐
│           Proxy (BungeeCord/Velocity)           │
│                                                  │
│  ┌────────────────────────────────────────┐    │
│  │  ServerBackup-BungeeCord/Velocity       │    │
│  │  (Coordinator Plugin)                   │    │
│  │                                          │    │
│  │  Commands: /networkbackup               │    │
│  └────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
         │                    │                │
         │ Plugin Messaging   │                │
         ▼                    ▼                ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Lobby      │    │   Survival   │    │   Creative   │
│  (Backend)   │    │  (Backend)   │    │  (Backend)   │
│              │    │              │    │              │
│ ServerBackup │    │ ServerBackup │    │ ServerBackup │
│   Plugin     │    │   Plugin     │    │   Plugin     │
└──────────────┘    └──────────────┘    └──────────────┘
```

## Setup

### 1. Backend Servers (Spigot/Paper)

Install the main `ServerBackup-2.0.0.jar` on each backend server and enable network mode:

**config.yml:**
```yaml
integrations:
  network:
    enabled: true
    mode: 'bungee'  # or 'velocity'
```

Restart each backend server. You should see:
```
[ServerBackup] Network backup listener registered (BungeeCord/Velocity)
[ServerBackup]   Listening on channel: serverbackup:request
```

### 2. Proxy Server (BungeeCord)

Install `ServerBackup-BungeeCord-2.0.0.jar` in your BungeeCord `plugins/` folder and restart:

```
[ServerBackup-BungeeCord] Backup coordinator initialized
[ServerBackup-BungeeCord] Command: /networkbackup [server|all]
```

### 3. Proxy Server (Velocity)

Install `ServerBackup-Velocity-2.0.0.jar` in your Velocity `plugins/` folder and restart:

```
[ServerBackup-Velocity] Backup coordinator initialized
[ServerBackup-Velocity] Command: /networkbackup [server|all]
```

## Usage

### Commands (Proxy Only)

All commands are executed from the proxy console or by players with `serverbackup.network` permission:

```bash
# Backup all servers sequentially
/networkbackup
/networkbackup all

# Backup all servers in parallel
/networkbackup parallel

# Backup specific server
/networkbackup lobby
/networkbackup survival

# Specify backup type
/networkbackup all world      # World-only backup
/networkbackup all full       # Full server backup
/networkbackup lobby full     # Full backup of lobby server

# Help
/networkbackup help
```

### Permissions

- `serverbackup.network` - Access to `/networkbackup` command (proxy-side)

### Example Output

```
╔════════════════════════════════════════╗
║    NETWORK BACKUP INITIATED            ║
╚════════════════════════════════════════╝
Session ID: a3f8e912
Servers: 3
Mode: Sequential

→ lobby - Starting...
✓ lobby - Completed (1243ms, 45MB)
→ survival - Starting...
✓ survival - Completed (3821ms, 128MB)
→ creative - Starting...
✓ creative - Completed (2156ms, 89MB)

═══════════════════════════════════════════
Network Backup Summary
═══════════════════════════════════════════
Session: a3f8e912
Total Time: 42s
Success: 3 / Failed: 0
═══════════════════════════════════════════
```

## Configuration

### Backend Server (config.yml)

```yaml
integrations:
  network:
    enabled: true
    mode: 'bungee'  # bungee or velocity
    coordinator:
      sequential: true  # Execute backups one at a time
      delay-between-servers: 5  # Seconds between servers
      timeout: 300  # Timeout in seconds (5 minutes)
```

### Sequential vs Parallel Mode

**Sequential Mode (Recommended):**
- Backups run one server at a time
- Prevents overloading the network/storage
- Predictable resource usage
- Slower but safer

**Parallel Mode:**
- All servers backup simultaneously
- Faster completion
- Higher resource usage
- Risk of I/O bottlenecks

## Plugin Messaging Channels

The system uses two plugin messaging channels:

1. **serverbackup:request** - Proxy → Backend
   - Sends backup commands to backend servers
   
2. **serverbackup:response** - Backend → Proxy
   - Returns backup status and results

## Troubleshooting

### "No online players" error
- At least one player must be online on a backend server for plugin messaging to work
- This is a BungeeCord/Velocity limitation

### Backend not responding
- Check that network integration is enabled: `integrations.network.enabled: true`
- Verify the correct mode is set: `mode: 'bungee'` or `mode: 'velocity'`
- Check backend server logs for errors

### Timeout issues
- Increase timeout in config: `coordinator.timeout: 600` (10 minutes)
- Large backups may take longer than default 5 minutes

## Advanced Usage

### Sequential Backup with Custom Delay

Edit the proxy's backend config to add delays between servers:

```yaml
integrations:
  network:
    coordinator:
      sequential: true
      delay-between-servers: 10  # 10 seconds between each server
```

### Automated Network Backups

Use a scheduler plugin on the proxy (like BungeeScheduler) to run:
```
/networkbackup all full
```

Or create a cron job:
```bash
# Every 6 hours
0 */6 * * * screen -S bungee -p 0 -X stuff "networkbackup all^M"
```

## Performance Tips

1. **Use Sequential Mode** during peak hours to avoid lag spikes
2. **Use Parallel Mode** during off-peak hours for faster completion
3. **Stagger manual backups** if you have many servers (>5)
4. **Monitor disk I/O** - multiple simultaneous backups can saturate storage
5. **Enable compression** on backend servers to reduce file sizes

## Compatibility

- **BungeeCord:** 1.19+
- **Velocity:** 3.3.0+
- **Backend:** Spigot/Paper 1.21.1 with ServerBackup 2.0.0+

## API for Other Plugins

Other proxy plugins can trigger network backups programmatically:

**BungeeCord:**
```java
ServerBackupBungee plugin = (ServerBackupBungee) ProxyServer.getInstance()
    .getPluginManager().getPlugin("ServerBackup-BungeeCord");
plugin.getCoordinator().startNetworkBackup(sender, true, "full");
```

**Velocity:**
```java
ServerBackupVelocity plugin = (ServerBackupVelocity) server.getPluginManager()
    .getPlugin("serverbackup-velocity").orElse(null);
plugin.getCoordinator().startNetworkBackup(source, true, "full");
```
