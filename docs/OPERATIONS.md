# TaskFlow Local Operations (Linux + systemd)

This document describes how to keep TaskFlow running after reboot and how to deploy updates with a single command.

## 1. One-time setup

### 1.1 Build the project

```bash
cd /home/joao/Documentos/Projetos/task
./mvnw clean package -DskipTests
```

### 1.2 Create service user and runtime directories

```bash
sudo useradd --system --home /opt/taskflow --shell /usr/sbin/nologin taskflow || true
sudo mkdir -p /opt/taskflow/current /opt/taskflow/releases
sudo chown -R taskflow:taskflow /opt/taskflow
```

### 1.3 Create environment file

```bash
sudo cp /home/joao/Documentos/Projetos/task/ops/env/taskflow.env.example /etc/taskflow.env
sudo chmod 600 /etc/taskflow.env
sudo chown root:root /etc/taskflow.env
```

Edit `/etc/taskflow.env` with real DB values.

### 1.4 Install and enable systemd service

```bash
sudo cp /home/joao/Documentos/Projetos/task/ops/systemd/taskflow.service /etc/systemd/system/taskflow.service
sudo systemctl daemon-reload
sudo systemctl enable taskflow
sudo systemctl start taskflow
sudo systemctl status taskflow
```

## 2. Logs and diagnostics

```bash
journalctl -u taskflow -f
journalctl -u taskflow --since "1 hour ago"
```

## 3. Deploy updates with one command

First time only:

```bash
chmod +x /home/joao/Documentos/Projetos/task/scripts/deploy-taskflow.sh
chmod +x /home/joao/Documentos/Projetos/task/scripts/healthcheck-taskflow.sh
```

Then deploy:

```bash
/home/joao/Documentos/Projetos/task/scripts/deploy-taskflow.sh
```

The deploy script performs:

1. `git pull --ff-only`
2. `./mvnw clean package -DskipTests`
3. publish jar to `/opt/taskflow/releases/<timestamp>/task.jar`
4. update symlink `/opt/taskflow/current/task.jar`
5. restart `taskflow`
6. run healthcheck (`/board` by default)
7. rollback automatically if restart/healthcheck fails

## 4. Optional hardening

- Keep `/etc/taskflow.env` as `600`.
- Use a dedicated DB user with least privilege.
- Configure journal retention if needed via `/etc/systemd/journald.conf`.

