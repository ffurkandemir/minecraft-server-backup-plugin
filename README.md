# Server Backup Plugin

A comprehensive backup plugin for Minecraft 1.21.1 servers.

## Features

- **Automatic Backups**: Schedule automatic backups at configurable intervals
- **Manual Backups**: Create backups on-demand with a simple command
- **Compression**: Backups are compressed as ZIP files to save disk space
- **Backup Management**: List, delete, and manage your backups
- **World Selection**: Choose which worlds to backup
- **Configurable Messages**: Customize all plugin messages
- **Broadcast Options**: Choose whether to broadcast backup notifications
- **Max Backup Limit**: Automatically delete old backups when limit is reached

## Commands

- `/backup [now|auto|cancel]` - Create a backup or manage auto-backup
- `/backuplist` - List all available backups
- `/backuprestore <backup-name>` - Get instructions to restore a backup
- `/backupdelete <backup-name>` - Delete a specific backup

## Permissions

- `serverbackup.*` - Access to all commands
- `serverbackup.backup` - Create backups
- `serverbackup.list` - List backups
- `serverbackup.restore` - View restore instructions
- `serverbackup.delete` - Delete backups

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/ServerBackupPlugin/config.yml`

## Building from Source

```bash
mvn clean package
```

The compiled JAR will be in the `target` directory.

## Configuration

Edit `config.yml` to customize:
- Backup directory location
- Auto-backup interval
- Maximum number of backups to keep
- Which worlds to backup
- Custom messages

## Requirements

- Minecraft Server 1.21.1
- Java 21 or higher
- Spigot/Paper API

## Support

For issues and feature requests, please open an issue on the GitHub repository.

## License

This project is open source and available under the MIT License.
