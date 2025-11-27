# OpenKoda Deployment Guide

This comprehensive guide provides detailed instructions for deploying OpenKoda in various environments, from local development to production cloud platforms. This documentation targets DevOps engineers and system administrators.

## Table of Contents

1. [Infrastructure Requirements](#1-infrastructure-requirements)
2. [Environment Setup](#2-environment-setup)
3. [Database Configuration](#3-database-configuration)
4. [Application Server Setup](#4-application-server-setup)
5. [Docker Deployment](#5-docker-deployment)
6. [Kubernetes Deployment](#6-kubernetes-deployment)
7. [Cloud Platform Deployment](#7-cloud-platform-deployment)
8. [Configuration Management](#8-configuration-management)
9. [Security Hardening](#9-security-hardening)
10. [Backup and Restore Procedures](#10-backup-and-restore-procedures)
11. [Monitoring Setup](#11-monitoring-setup)
12. [Logging Configuration](#12-logging-configuration)
13. [Performance Tuning](#13-performance-tuning)
14. [Scaling Strategies](#14-scaling-strategies)
15. [Disaster Recovery](#15-disaster-recovery)
16. [Maintenance Windows](#16-maintenance-windows)
17. [Upgrade Procedures](#17-upgrade-procedures)
18. [Troubleshooting Guide](#18-troubleshooting-guide)
19. [Support Channels](#19-support-channels)
20. [Operational Runbooks](#20-operational-runbooks)

---

## 1. Infrastructure Requirements

### Hardware Specifications

**Minimum Requirements (Development/Testing)**:
- CPU: 2 cores (2.0 GHz or higher)
- RAM: 4 GB
- Storage: 20 GB available disk space
- Network: 100 Mbps

**Recommended Requirements (Production)**:
- CPU: 4+ cores (2.5 GHz or higher)
- RAM: 8 GB or more
- Storage: 50 GB+ SSD storage
- Network: 1 Gbps with redundancy

### Operating System Requirements

OpenKoda supports deployment on:
- **Linux**: Ubuntu 18.04.1 LTS or higher (Ubuntu 22.04 LTS recommended)
- **Linux**: CentOS 7+, RHEL 7+, Amazon Linux 2
- **Container Platforms**: Docker 20.10+, Kubernetes 1.21+

Source: openkoda/doc/installation.md

### Network Requirements

**Required Ports**:
- 8080: Application HTTP port (default)
- 5432: PostgreSQL database port
- 443: HTTPS (production deployments with NGINX)
- 80: HTTP redirect to HTTPS

**Firewall Rules**:
- Allow inbound traffic on application port (8080 or configured port)
- Allow database connections from application servers only
- Restrict administrative access to specific IP ranges

### Database Requirements

- PostgreSQL 14.2 or higher (PostgreSQL 14.4+ recommended)
- Minimum 10 GB storage for database
- Recommended 50 GB+ for production with growth capacity

Source: docker/docker-compose.yaml:32, openkoda/doc/installation.md:14

---

## 2. Environment Setup

### Operating System Configuration

**Update System Packages**:
```bash
apt update && apt upgrade -y
```

**Install Essential Tools**:
```bash
apt install -y wget curl git build-essential
```

Source: openkoda/doc/installation.md:8

### Java 21 Installation

OpenKoda requires Java 21 LTS (OpenJDK 21 or higher).

**Download and Install Java 21**:
```bash
cd /usr/lib && mkdir -p jvm && cd jvm
wget https://download.java.net/java/GA/jdk21.0.5/b0dc2e7cf62c4ea1ba5c276ca1c45b2f/11/GPL/openjdk-21.0.5_linux-x64_bin.tar.gz
tar -xzf openjdk-21.0.5_linux-x64_bin.tar.gz
```

**Configure JAVA_HOME**:
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-21.0.5
export PATH=$JAVA_HOME/bin:$PATH
```

Add these lines to `/etc/profile` or `~/.bashrc` for persistence.

**Verify Installation**:
```bash
java --version
```

Expected output: `openjdk 21.0.5 2024-10-15`

Source: openkoda/doc/installation.md:76-94, docker/Dockerfile:2

### Maven Installation

Maven 3.8.6 or higher is required for building OpenKoda from source.

**Install Maven**:
```bash
wget https://dlcdn.apache.org/maven/maven-3/3.8.7/binaries/apache-maven-3.8.7-bin.tar.gz
tar -xzf apache-maven-3.8.7-bin.tar.gz -C /opt
export PATH=/opt/apache-maven-3.8.7/bin:$PATH
```

Source: openkoda/doc/installation.md:105

### Environment Variables

Configure the following system environment variables:

| Variable | Purpose | Example Value |
|----------|---------|---------------|
| JAVA_HOME | Java installation path | /usr/lib/jvm/jdk-21.0.5 |
| PATH | Include Java and Maven binaries | $JAVA_HOME/bin:$PATH |
| SPRING_DATASOURCE_URL | Database connection string | jdbc:postgresql://localhost:5432/openkoda |
| SPRING_PROFILES_ACTIVE | Active Spring profiles | openkoda,production |

Source: docker/Dockerfile:22-36

---

## 3. Database Configuration

### PostgreSQL Installation

**Install PostgreSQL 14**:
```bash
apt install postgresql-14
```

**Start PostgreSQL Service**:
```bash
systemctl start postgresql
systemctl enable postgresql
```

Source: openkoda/doc/installation.md:14-16

### Database Creation

**Create OpenKoda Database**:
```bash
su postgres
psql
```

```sql
CREATE DATABASE openkoda;
ALTER ROLE postgres WITH PASSWORD 'your-secure-password';
```

Source: openkoda/doc/installation.md:98-102

### User and Permissions

**Create Dedicated Database User** (Production Recommended):
```sql
CREATE USER openkoda_user WITH PASSWORD 'secure-password';
GRANT ALL PRIVILEGES ON DATABASE openkoda TO openkoda_user;
```

**Verify Connection**:
```bash
psql -h localhost -U openkoda_user -d openkoda
```

### Connection Pooling (HikariCP)

OpenKoda uses HikariCP for connection pooling. Configure in application properties:

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Performance Tuning

**PostgreSQL Configuration** (`/etc/postgresql/14/main/postgresql.conf`):

```properties
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 128MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
work_mem = 8MB
max_connections = 100
```

**Apply Changes**:
```bash
systemctl restart postgresql
```

---

## 4. Application Server Setup

### Standalone JAR Deployment

**Build OpenKoda Application**:
```bash
mvn -f openkoda/pom.xml clean install spring-boot:repackage -DskipTests
```

The compiled JAR file is located at `openkoda/build/openkoda.jar`.

Source: openkoda/doc/installation.md:108-111

### Database Initialization

**First-Time Setup** (creates schema and initial data):
```bash
java -Dloader.path=/BOOT-INF/classes \
     -Dspring.profiles.active=openkoda,drop_and_init_database \
     -jar openkoda.jar --server.port=8080
```

Source: openkoda/doc/installation.md:115-119, docker/entrypoint.sh:9-22

### Systemd Service Configuration

**Create Service File** (`/etc/systemd/system/openkoda.service`):

```ini
[Unit]
Description=OpenKoda Application
After=network.target postgresql.service

[Service]
Type=simple
User=openkoda-cloud
WorkingDirectory=/opt/openkoda
ExecStart=/usr/lib/jvm/jdk-21.0.5/bin/java \
  -Dloader.path=/BOOT-INF/classes \
  -Dspring.profiles.active=openkoda \
  -Dlogging.file.name=/var/log/openkoda/openkoda.log \
  -jar /opt/openkoda/openkoda.jar --server.port=8080
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Enable and Start Service**:
```bash
systemctl daemon-reload
systemctl enable openkoda
systemctl start openkoda
```

Source: docker/Dockerfile:8, docker/entrypoint.sh:35-44

### Startup Scripts

**Production Startup Command**:
```bash
java -Dloader.path=/BOOT-INF/classes \
     -Dspring.profiles.active=openkoda \
     -jar openkoda.jar --server.port=8080
```

Source: openkoda/doc/installation.md:138-139

### Logging Setup

OpenKoda logs to `/var/log/openkoda/openkoda.log` by default.

**Create Log Directory**:
```bash
mkdir -p /var/log/openkoda
chown openkoda-cloud:openkoda-cloud /var/log/openkoda
```

Source: docker/Dockerfile:11, docker/entrypoint.sh:18

---

## 5. Docker Deployment

### Docker Engine Installation

**Install Docker on Ubuntu**:
```bash
apt install -y docker.io
systemctl start docker
systemctl enable docker
```

**Verify Installation**:
```bash
docker --version
```

### Image Pull

**Pull OpenKoda Docker Image**:
```bash
docker pull openkoda/openkoda:latest
```

Source: docker/docker-compose.yaml:5

### Container Configuration

**Run OpenKoda Container**:
```bash
docker run -d \
  --name openkoda \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/openkoda \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -v openkoda-data:/data \
  openkoda/openkoda:latest
```

Source: docker/Dockerfile:22-24, docker/docker-compose.yaml:7-24

### Docker Compose Setup

**Create `docker-compose.yaml`**:

```yaml
version: '3'

services:
  openkoda:
    image: openkoda/openkoda:latest
    container_name: openkoda
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/openkoda
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      BASE_URL: http://localhost:8080
    depends_on:
      postgres:
        condition: service_healthy
  
  postgres:
    image: postgres:14.2
    container_name: postgres-db
    environment:
      POSTGRES_DB: openkoda
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d openkoda"]
      interval: 10s
      timeout: 5s
      retries: 5
```

**Start Services**:
```bash
docker-compose up -d
```

Source: docker/docker-compose.yaml

### Volume Management

**Data Persistence**:
- `/data`: Application file storage
- `/var/log/openkoda`: Application logs
- `/config`: External configuration files

**Create Named Volumes**:
```bash
docker volume create openkoda-data
docker volume create openkoda-logs
```

Source: docker/Dockerfile:11, docker/entrypoint.sh:5-6

### Network Configuration

Docker Compose creates a bridge network automatically. For custom networks:

```bash
docker network create openkoda-network
```

### Environment Variables

**Complete Environment Variable Table**:

| Variable | Default | Description | Source |
|----------|---------|-------------|--------|
| SPRING_DATASOURCE_URL | jdbc:postgresql://localhost:5432/openkoda | Database connection string | Dockerfile:22 |
| SPRING_DATASOURCE_USERNAME | postgres | Database username | Dockerfile:23 |
| SPRING_DATASOURCE_PASSWORD | postgres | Database password | Dockerfile:24 |
| BASE_URL | http://localhost:8080/ | Application base URL | Dockerfile:25 |
| APPLICATION_ADMIN_EMAIL | admin@yourdomain.org | Administrator email | Dockerfile:27 |
| INIT_ADMIN_USERNAME | admin | Initial admin username | Dockerfile:28 |
| INIT_ADMIN_PASSWORD | admin123 | Initial admin password | Dockerfile:29 |
| FILE_STORAGE_FILESYSTEM_PATH | /data | File storage location | Dockerfile:33 |
| SPRING_PROFILES_ACTIVE | openkoda,development | Active Spring profiles | Dockerfile:34 |
| STORAGE_TYPE | db | Storage backend (db/filesystem) | Dockerfile:35 |
| SPRING_CONFIG_LOCATION | classpath:/,/config/ | Configuration file locations | Dockerfile:36 |
| INIT_EXTERNAL_SCRIPT | (empty) | External initialization script | Dockerfile:32 |

Source: docker/Dockerfile:22-36, docker/docker-compose.yaml:9-24

### Healthchecks

**PostgreSQL Healthcheck** (docker-compose.yaml):
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d openkoda"]
  interval: 10s
  timeout: 5s
  retries: 5
```

Source: docker/docker-compose.yaml:39-43

---

## 6. Kubernetes Deployment

### Kubernetes Cluster Requirements

- Kubernetes 1.21 or higher
- kubectl CLI tool installed and configured
- Minimum 2 worker nodes for high availability
- Storage class configured for persistent volumes

### Deployment Manifests

**Namespace Creation**:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: openkoda
```

**OpenKoda Deployment**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openkoda
  namespace: openkoda
spec:
  replicas: 2
  selector:
    matchLabels:
      app: openkoda
  template:
    metadata:
      labels:
        app: openkoda
    spec:
      containers:
      - name: openkoda
        image: openkoda/openkoda:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: openkoda-config
              key: database-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: openkoda-secrets
              key: database-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: openkoda-secrets
              key: database-password
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

### Service Definition

**ClusterIP Service**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: openkoda-service
  namespace: openkoda
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: openkoda
```

### ConfigMap and Secret

**ConfigMap**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: openkoda-config
  namespace: openkoda
data:
  database-url: "jdbc:postgresql://postgres-service:5432/openkoda"
  base-url: "https://openkoda.example.com"
```

**Secret**:
```bash
kubectl create secret generic openkoda-secrets \
  --from-literal=database-username=postgres \
  --from-literal=database-password=your-secure-password \
  -n openkoda
```

### StatefulSet for PostgreSQL

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: openkoda
spec:
  serviceName: postgres-service
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:14.2
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: openkoda
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: openkoda-secrets
              key: database-username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: openkoda-secrets
              key: database-password
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
  - metadata:
      name: postgres-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 50Gi
```

### PersistentVolumeClaim

**OpenKoda Data PVC**:
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: openkoda-data
  namespace: openkoda
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

### Ingress Configuration

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openkoda-ingress
  namespace: openkoda
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - openkoda.example.com
    secretName: openkoda-tls
  rules:
  - host: openkoda.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: openkoda-service
            port:
              number: 8080
```

### Resource Limits and Requests

**Recommended Values**:
- Requests: CPU 1000m, Memory 2Gi
- Limits: CPU 2000m, Memory 4Gi

Adjust based on workload and monitoring data.

---

## 7. Cloud Platform Deployment

### AWS Deployment

#### EC2 Instance Setup

**Recommended Instance Types**:
- Development: t3.medium (2 vCPU, 4 GB RAM)
- Production: t3.xlarge (4 vCPU, 16 GB RAM) or c5.2xlarge

**Launch EC2 Instance**:
1. Choose Ubuntu Server 22.04 LTS AMI
2. Configure security group to allow ports 22, 80, 443, 8080
3. Attach IAM role with necessary permissions
4. Add storage: 50 GB+ GP3 SSD

#### Amazon RDS for PostgreSQL

```bash
aws rds create-db-instance \
  --db-instance-identifier openkoda-db \
  --db-instance-class db.t3.medium \
  --engine postgres \
  --engine-version 14.7 \
  --master-username openkoda_admin \
  --master-user-password your-secure-password \
  --allocated-storage 100 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-xxxxxxxxx
```

#### Amazon ECS/EKS

**ECS Task Definition** (excerpt):
```json
{
  "family": "openkoda",
  "containerDefinitions": [{
    "name": "openkoda",
    "image": "openkoda/openkoda:latest",
    "memory": 4096,
    "cpu": 2048,
    "portMappings": [{
      "containerPort": 8080,
      "protocol": "tcp"
    }]
  }]
}
```

### Azure Deployment

#### Azure Virtual Machines

**Create VM**:
```bash
az vm create \
  --resource-group openkoda-rg \
  --name openkoda-vm \
  --image UbuntuLTS \
  --size Standard_D2s_v3 \
  --admin-username azureuser \
  --generate-ssh-keys
```

#### Azure Database for PostgreSQL

```bash
az postgres flexible-server create \
  --resource-group openkoda-rg \
  --name openkoda-postgresql \
  --location eastus \
  --admin-user openkoda_admin \
  --admin-password your-secure-password \
  --sku-name Standard_D2s_v3 \
  --version 14
```

#### Azure Kubernetes Service (AKS)

```bash
az aks create \
  --resource-group openkoda-rg \
  --name openkoda-aks \
  --node-count 2 \
  --node-vm-size Standard_D2s_v3 \
  --enable-managed-identity \
  --generate-ssh-keys
```

### GCP Deployment

#### Google Compute Engine

**Create Instance**:
```bash
gcloud compute instances create openkoda-instance \
  --zone=us-central1-a \
  --machine-type=n1-standard-2 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=50GB
```

#### Cloud SQL for PostgreSQL

```bash
gcloud sql instances create openkoda-db \
  --database-version=POSTGRES_14 \
  --tier=db-custom-2-8192 \
  --region=us-central1
```

#### Google Kubernetes Engine (GKE)

```bash
gcloud container clusters create openkoda-cluster \
  --zone us-central1-a \
  --num-nodes 2 \
  --machine-type n1-standard-2
```

### Cloud-Specific Environment Variables

Configure cloud-specific connection strings:

**AWS RDS**:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://openkoda-db.xxxxx.us-east-1.rds.amazonaws.com:5432/openkoda
```

**Azure PostgreSQL**:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://openkoda-postgresql.postgres.database.azure.com:5432/openkoda
SPRING_DATASOURCE_USERNAME=openkoda_admin@openkoda-postgresql
```

**GCP Cloud SQL**:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://10.x.x.x:5432/openkoda
```

---

## 8. Configuration Management

### Application Properties Structure

OpenKoda uses Spring Boot's profile-based configuration system. Configuration files are located in `openkoda/src/main/resources/`.

**Primary Configuration Files**:
- `application.properties`: Profile activation
- `application-openkoda.properties`: Core application settings
- `application-drop_and_init_database.properties`: Database initialization profile

Source: openkoda/src/main/resources/application.properties

### Spring Profiles

**Available Profiles**:

| Profile | Purpose | When to Use |
|---------|---------|-------------|
| openkoda | Core application profile | Always active in production |
| local | Local development settings | Development workstations |
| drop_and_init_database | Database schema initialization | First-time setup only |
| development-module | Development module features | Development/testing environments |

**Activate Profiles**:
```bash
java -Dspring.profiles.active=openkoda,production -jar openkoda.jar
```

Source: docker/entrypoint.sh:11, docker/entrypoint.sh:25

### External Configuration

**Using SPRING_CONFIG_LOCATION**:
```bash
java -Dspring.config.location=classpath:/,/config/,/etc/openkoda/ -jar openkoda.jar
```

Place custom configuration files in `/config/` or `/etc/openkoda/`.

Source: docker/Dockerfile:36

### Property Precedence

Configuration properties are loaded in the following order (last wins):
1. Packaged `application.properties`
2. Profile-specific properties (`application-{profile}.properties`)
3. System properties (`-Dproperty=value`)
4. Environment variables

### Key Configuration Properties

**Database Configuration**:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/openkoda
spring.datasource.username=postgres
spring.datasource.password=your-secure-password
spring.datasource.driverClassName=org.postgresql.Driver
```

Source: openkoda/src/main/resources/application-openkoda.properties:5-8

**Application Settings**:
```properties
base.url=https://openkoda.example.com
application.name=Openkoda
application.admin.email=admin@yourdomain.org
init.admin.username=admin
init.admin.password=admin123
```

Source: openkoda/src/main/resources/application-openkoda.properties:81-83, 86-88

**File Storage**:
```properties
file.storage.filesystem.path=/data
file.storage.type=db
```

**Session Configuration**:
```properties
server.servlet.session.timeout=120m
```

Source: openkoda/src/main/resources/application-openkoda.properties:78

### Secrets Management

**Using Vault**:
```bash
export SPRING_CLOUD_VAULT_TOKEN=your-vault-token
export SPRING_CLOUD_VAULT_URI=https://vault.example.com:8200
```

**Using AWS Secrets Manager**:
```bash
export AWS_SECRET_NAME=openkoda/production/database
```

**Environment Variable Override**:
```bash
export SPRING_DATASOURCE_PASSWORD=$(aws secretsmanager get-secret-value --secret-id openkoda-db-password --query SecretString --output text)
```

### Multitenancy Configuration

**Single Database / Single Schema** (default):
```properties
is.multitenancy=false
```

**Single Database / Many Schemas**:
```properties
is.multitenancy=true
spring.jpa.properties.hibernate.multiTenancy=SCHEMA
spring.jpa.properties.hibernate.tenant_identifier_resolver=com.openkoda.core.multitenancy.TenantResolver
spring.jpa.properties.hibernate.multi_tenant_connection_provider=com.openkoda.core.multitenancy.HybridMultiTenantConnectionProvider
```

Source: openkoda/doc/installation.md:148-163

---

## 9. Security Hardening

### HTTPS/TLS Setup

#### Using NGINX as Reverse Proxy

**Install NGINX**:
```bash
apt install nginx
systemctl start nginx
```

**NGINX Configuration** (`/etc/nginx/conf.d/openkoda.conf`):
```nginx
server {
    server_name openkoda.example.com;
    
    location / {
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_pass http://127.0.0.1:8080/;
        proxy_buffer_size 32k;
        proxy_buffers 8 32k;
        proxy_read_timeout 3600s;
    }
}
```

Source: openkoda/doc/installation.md:44-59

#### SSL Certificate with Certbot

**Install Certbot**:
```bash
snap install certbot --classic
```

**Generate Certificate**:
```bash
certbot --nginx -d openkoda.example.com
```

Certbot automatically configures NGINX for HTTPS.

Source: openkoda/doc/installation.md:19-26, 69-71

### Database SSL Configuration

**Enable SSL in PostgreSQL** (`postgresql.conf`):
```properties
ssl = on
ssl_cert_file = '/etc/ssl/certs/server.crt'
ssl_key_file = '/etc/ssl/private/server.key'
```

**Connect with SSL**:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/openkoda?ssl=true&sslmode=require
```

### Authentication Configuration

**Change Default Admin Credentials**:
```bash
export INIT_ADMIN_USERNAME=your-admin-username
export INIT_ADMIN_PASSWORD=your-secure-password
```

**Enforce Strong Passwords**: Configure in application properties or via admin UI.

### Password Policies

Configure password requirements:
- Minimum length: 12 characters
- Complexity: Uppercase, lowercase, numbers, special characters
- Expiration: 90 days (configurable)
- History: Prevent reuse of last 5 passwords

### JWT Token Security

Configure JWT secret and expiration:
```properties
jwt.secret=your-very-long-and-secure-random-secret
jwt.expiration=3600000
```

### CORS Configuration

**Restrict CORS Origins**:
```properties
cors.allowed.origins=https://openkoda.example.com
```

### Security Headers

Configure security headers in NGINX or application:
```nginx
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Strict-Transport-Security "max-age=31536000" always;
```

### Audit Logging

OpenKoda includes built-in audit logging. Configure logging level:
```properties
logging.level.com.openkoda.core.audit=INFO
```

Source: openkoda/src/main/resources/application-openkoda.properties:27

### Cookie Security

**Enable Secure Cookies**:
```bash
java -Dsecure.cookie=true -jar openkoda.jar
```

Source: openkoda/doc/installation.md:139, docker/docker-compose.yaml:24

---

## 10. Backup and Restore Procedures

### PostgreSQL Backup (pg_dump)

**Full Database Backup**:
```bash
pg_dump -U postgres -h localhost -F c -b -v -f openkoda-backup-$(date +%Y%m%d-%H%M%S).dump openkoda
```

**Compressed Backup**:
```bash
pg_dump -U postgres openkoda | gzip > openkoda-backup-$(date +%Y%m%d).sql.gz
```

### Automated Backup Scripts

**Daily Backup Cron Job** (`/etc/cron.daily/openkoda-backup`):
```bash
#!/bin/bash
BACKUP_DIR=/backups/openkoda
DATE=$(date +%Y%m%d-%H%M%S)
pg_dump -U postgres -F c openkoda -f $BACKUP_DIR/openkoda-$DATE.dump
find $BACKUP_DIR -name "openkoda-*.dump" -mtime +30 -delete
```

Make executable: `chmod +x /etc/cron.daily/openkoda-backup`

### PostgreSQL Restore (pg_restore)

**Restore from Backup**:
```bash
pg_restore -U postgres -h localhost -d openkoda -c -v openkoda-backup-20240115.dump
```

### Point-in-Time Recovery (PITR)

**Enable WAL Archiving** (`postgresql.conf`):
```properties
wal_level = replica
archive_mode = on
archive_command = 'cp %p /var/lib/postgresql/wal_archive/%f'
```

**Perform PITR**:
```bash
pg_restore -U postgres -d openkoda --target-time="2024-01-15 14:30:00" backup.dump
```

### File Storage Backup

**Backup Application Data Directory**:
```bash
tar -czf openkoda-data-$(date +%Y%m%d).tar.gz /data
```

Source: docker/Dockerfile:33, docker/entrypoint.sh:17

### Backup Retention Policy

**Recommended Retention**:
- Daily backups: Retain 7 days
- Weekly backups: Retain 4 weeks
- Monthly backups: Retain 12 months

**Automated Cleanup**:
```bash
find /backups/daily -mtime +7 -delete
find /backups/weekly -mtime +30 -delete
find /backups/monthly -mtime +365 -delete
```

### Disaster Recovery Testing

**Quarterly DR Test Procedure**:
1. Restore latest backup to test environment
2. Verify application starts and data is accessible
3. Run smoke tests on critical functionality
4. Document restoration time and any issues
5. Update DR procedures based on findings

---

## 11. Monitoring Setup

### Application Health Endpoints

OpenKoda exposes health check endpoints:

**Health Check**:
```bash
curl http://localhost:8080/ping
```

Expected response: HTTP 200 OK

### Spring Boot Actuator

**Enable Actuator**:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

**Actuator Endpoints**:
- `/actuator/health`: Application health status
- `/actuator/info`: Application information
- `/actuator/metrics`: Application metrics
- `/actuator/prometheus`: Prometheus metrics export

### Prometheus Metrics

**Add Micrometer Dependency** (already included in Spring Boot):
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Prometheus Configuration** (`prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'openkoda'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana Dashboards

**Import Spring Boot Dashboard**:
1. Access Grafana UI
2. Import dashboard ID: 4701 (JVM Micrometer)
3. Configure Prometheus data source
4. Customize panels as needed

**Key Metrics to Monitor**:
- JVM heap usage
- HTTP request rate and duration
- Database connection pool usage
- Active threads
- Error rate

### JVM Monitoring

**Enable JMX Remote Monitoring**:
```bash
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar openkoda.jar
```

**Connect with JConsole**:
```bash
jconsole localhost:9010
```

### Database Monitoring

**PostgreSQL Query Statistics**:
```sql
SELECT * FROM pg_stat_activity WHERE datname = 'openkoda';
SELECT * FROM pg_stat_database WHERE datname = 'openkoda';
```

**Slow Query Logging** (`postgresql.conf`):
```properties
log_min_duration_statement = 1000
log_statement = 'all'
```

### Alerting

**Prometheus Alerting Rules** (`alerts.yml`):
```yaml
groups:
  - name: openkoda
    rules:
    - alert: HighMemoryUsage
      expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
      for: 5m
      annotations:
        summary: "High memory usage detected"
    - alert: HighErrorRate
      expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
      annotations:
        summary: "High error rate detected"
```

---

## 12. Logging Configuration

### Logback Configuration

OpenKoda uses Logback for logging. Default log file: `/var/log/openkoda/openkoda.log`

**Custom Logback Configuration** (`logback-spring.xml`):
```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/openkoda/openkoda.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/openkoda/openkoda-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

Source: docker/entrypoint.sh:18

### Log Levels

**Configure Log Levels**:
```properties
logging.level.root=INFO
logging.level.com.openkoda=DEBUG
logging.level.com.openkoda.core.audit=INFO
logging.level.org.springframework=INFO
logging.level.org.hibernate.SQL=DEBUG
```

Source: openkoda/src/main/resources/application-openkoda.properties:26-27

**Available Log Levels**:
- TRACE: Most detailed
- DEBUG: Detailed debugging information
- INFO: General informational messages
- WARN: Warning messages
- ERROR: Error messages

### MDC Request Tracking

OpenKoda uses MDC (Mapped Diagnostic Context) for request correlation.

**Request ID in Logs**:
Log entries include unique request IDs for tracing:
```
2024-01-15 10:30:45 [http-nio-8080-exec-1] INFO [requestId=abc123] com.openkoda.controller - Processing request
```

### Log Aggregation

#### ELK Stack (Elasticsearch, Logstash, Kibana)

**Logstash Configuration** (`logstash.conf`):
```
input {
  file {
    path => "/var/log/openkoda/openkoda.log"
    start_position => "beginning"
  }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} %{DATA:logger} - %{GREEDYDATA:message}" }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "openkoda-%{+YYYY.MM.dd}"
  }
}
```

#### Splunk

**Configure Splunk Forwarder**:
```bash
/opt/splunkforwarder/bin/splunk add monitor /var/log/openkoda/openkoda.log -index openkoda
```

### Log Rotation

**Logrotate Configuration** (`/etc/logrotate.d/openkoda`):
```
/var/log/openkoda/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0644 openkoda-cloud openkoda-cloud
    sharedscripts
    postrotate
        systemctl reload openkoda > /dev/null 2>&1 || true
    endscript
}
```

### Application Logs

**Primary Log File**:
- Location: `/var/log/openkoda/openkoda.log`
- Configured via: `-Dlogging.file.name=/var/log/openkoda/openkoda.log`

Source: openkoda/doc/installation.md:222-227

**PostgreSQL Logs**:
- Location: `/var/log/postgresql/postgresql-14-main.log`

Source: openkoda/doc/installation.md:230-231

---

## 13. Performance Tuning

### JVM Heap Sizing

**Recommended JVM Options**:
```bash
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/openkoda \
     -jar openkoda.jar
```

**Heap Size Guidelines**:
- Development: -Xms512m -Xmx1g
- Production: -Xms2g -Xmx4g
- High-traffic: -Xms4g -Xmx8g

### Garbage Collection Tuning

**G1GC Configuration** (recommended):
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
-XX:G1ReservePercent=10
```

**Monitor GC Activity**:
```bash
java -Xlog:gc*:file=/var/log/openkoda/gc.log -jar openkoda.jar
```

### HikariCP Connection Pool Sizing

**Optimal Pool Size Formula**: `connections = ((core_count * 2) + effective_spindle_count)`

**Configuration**:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**Monitor Pool Usage**:
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Query Optimization

**Enable Query Logging**:
```properties
spring.jpa.properties.hibernate.show_sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Identify Slow Queries**:
```sql
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE mean_exec_time > 1000
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### Caching Strategies

OpenKoda uses `RequestSessionCacheService` for in-process caching.

**Enable Second-Level Cache** (Hibernate):
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

**External Cache with Redis**:
```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

### Database Indexes

**Create Indexes for Common Queries**:
```sql
CREATE INDEX idx_organization_name ON organization(name);
CREATE INDEX idx_user_email ON user_account(email);
CREATE INDEX idx_audit_created_on ON audit_log(created_on);
```

**Analyze Index Usage**:
```sql
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;
```

### Application-Level Optimizations

**Lazy Loading Configuration**:
```properties
spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true
```

**Batch Size Configuration**:
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

---

## 14. Scaling Strategies

### Horizontal Scaling

**Stateless Application Design**: OpenKoda is designed to run multiple instances concurrently.

**Run Multiple Instances**:
```bash
# Instance 1
java -jar openkoda.jar --server.port=8081

# Instance 2
java -jar openkoda.jar --server.port=8082
```

**Docker Compose Scaling**:
```bash
docker-compose up -d --scale openkoda=3
```

### Load Balancing

#### NGINX Load Balancer

**NGINX Configuration** (`/etc/nginx/nginx.conf`):
```nginx
upstream openkoda_backend {
    least_conn;
    server 127.0.0.1:8081;
    server 127.0.0.1:8082;
    server 127.0.0.1:8083;
}

server {
    listen 80;
    server_name openkoda.example.com;
    
    location / {
        proxy_pass http://openkoda_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

#### AWS Application Load Balancer

**Create Target Group**:
```bash
aws elbv2 create-target-group \
  --name openkoda-targets \
  --protocol HTTP \
  --port 8080 \
  --vpc-id vpc-xxxxxxxx
```

**Register Targets**:
```bash
aws elbv2 register-targets \
  --target-group-arn arn:aws:elasticloadbalancing:... \
  --targets Id=i-xxxxxxxxx Id=i-yyyyyyyyy
```

### Database Read Replicas

**PostgreSQL Streaming Replication**:

**Primary Server** (`postgresql.conf`):
```properties
wal_level = replica
max_wal_senders = 3
wal_keep_size = 1GB
```

**Standby Server** (`postgresql.conf`):
```properties
hot_standby = on
```

**Application Configuration** (read/write split):
```properties
spring.datasource.primary.url=jdbc:postgresql://primary:5432/openkoda
spring.datasource.replica.url=jdbc:postgresql://replica:5432/openkoda
```

### Redis Caching Layer

**Add Redis Dependency**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Redis Configuration**:
```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=your-redis-password
```

### CDN for Static Assets

**Configure CDN for Static Resources**:
- Images, CSS, JavaScript files
- Use CloudFront (AWS), Azure CDN, or Cloudflare

**Update Base URL**:
```properties
static.resources.cdn.url=https://cdn.example.com
```

### Auto-Scaling

#### Kubernetes Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: openkoda-hpa
  namespace: openkoda
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: openkoda
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### AWS Auto Scaling Group

```bash
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name openkoda-asg \
  --launch-template LaunchTemplateName=openkoda-template \
  --min-size 2 \
  --max-size 10 \
  --desired-capacity 3 \
  --vpc-zone-identifier subnet-xxxxx,subnet-yyyyy
```

---

## 15. Disaster Recovery

### Recovery Time Objective (RTO) and Recovery Point Objective (RPO)

**Recommended Targets**:
- **RTO**: 4 hours (time to restore service)
- **RPO**: 1 hour (acceptable data loss)

Adjust based on business requirements and budget.

### Multi-Region Deployment

**Active-Passive Strategy**:
- Primary region: Active deployment
- Secondary region: Standby environment with automated failover
- Database replication to secondary region

**AWS Multi-Region Setup**:
```bash
# Primary Region: us-east-1
aws rds create-db-instance --region us-east-1 ...

# Read Replica in Secondary Region: us-west-2
aws rds create-db-instance-read-replica \
  --db-instance-identifier openkoda-replica \
  --source-db-instance-identifier openkoda-primary \
  --region us-west-2
```

### Database Replication

**PostgreSQL Physical Replication**:

**Primary Server Configuration**:
```properties
wal_level = replica
max_wal_senders = 5
wal_keep_size = 2GB
archive_mode = on
archive_command = 'cp %p /archive/%f'
```

**Standby Server Setup**:
```bash
pg_basebackup -h primary-host -U replication_user -D /var/lib/postgresql/14/standby -P -R
```

### Automated Failover

**Using Patroni for PostgreSQL HA**:

**Patroni Configuration** (`patroni.yml`):
```yaml
scope: openkoda-cluster
name: postgresql01

restapi:
  listen: 0.0.0.0:8008
  connect_address: node1.example.com:8008

etcd:
  hosts: etcd1:2379,etcd2:2379,etcd3:2379

bootstrap:
  dcs:
    postgresql:
      use_pg_rewind: true
      
postgresql:
  listen: 0.0.0.0:5432
  connect_address: node1.example.com:5432
  data_dir: /var/lib/postgresql/14/main
```

### Backup Restoration Procedures

**Full Disaster Recovery Steps**:

1. **Provision New Infrastructure**: Deploy fresh servers/containers
2. **Restore Database**: 
   ```bash
   pg_restore -U postgres -d openkoda backup.dump
   ```
3. **Restore Application Data**: Extract from backup archive
4. **Deploy Application**: Start OpenKoda with restored configuration
5. **Verify Functionality**: Run smoke tests
6. **Update DNS**: Point domain to new infrastructure

### Disaster Recovery Testing

**Quarterly DR Drill Checklist**:
1. [ ] Document start time
2. [ ] Trigger simulated disaster (e.g., terminate primary database)
3. [ ] Execute failover procedures
4. [ ] Restore from backup in secondary region
5. [ ] Verify application accessibility
6. [ ] Test critical user workflows
7. [ ] Measure RTO (actual recovery time)
8. [ ] Measure RPO (data loss window)
9. [ ] Document lessons learned
10. [ ] Update DR procedures

### Business Continuity Plan

**Communication Protocol**:
1. Identify incident severity
2. Notify stakeholders (operations team, management, customers)
3. Activate DR team
4. Execute recovery procedures
5. Provide status updates every 30 minutes
6. Document incident timeline

**Stakeholder Contact List**:
- Operations Lead: ops-lead@example.com
- Database Administrator: dba@example.com
- Engineering Manager: eng-manager@example.com
- Customer Support: support@example.com

---

## 16. Maintenance Windows

### Planned Maintenance Scheduling

**Recommended Maintenance Window**:
- Frequency: Monthly
- Day: First Sunday of each month
- Time: 02:00 - 04:00 AM (local timezone)
- Duration: 2 hours maximum

**Pre-Maintenance Checklist**:
1. [ ] Announce maintenance 7 days in advance
2. [ ] Prepare rollback plan
3. [ ] Backup database before maintenance
4. [ ] Verify backup restoration procedure
5. [ ] Review change list with team
6. [ ] Prepare communication templates

### Database Maintenance

**PostgreSQL VACUUM**:
```sql
VACUUM ANALYZE;
```

**PostgreSQL REINDEX**:
```sql
REINDEX DATABASE openkoda;
```

**Update Statistics**:
```sql
ANALYZE;
```

**Scheduled Maintenance Script**:
```bash
#!/bin/bash
# Run weekly during maintenance window
psql -U postgres -d openkoda -c "VACUUM ANALYZE;"
psql -U postgres -d openkoda -c "REINDEX DATABASE openkoda;"
```

### Application Updates

**Rolling Update Procedure**:
1. Scale up additional instance
2. Update first instance
3. Verify health checks pass
4. Update remaining instances one at a time
5. Scale down extra instance

**Zero-Downtime Deployment**:
```bash
# Start new version on different port
java -jar openkoda-v1.8.0.jar --server.port=8081

# Verify new version health
curl http://localhost:8081/ping

# Update load balancer to include new instance
# Remove old instance from load balancer
# Stop old instance
```

### Communication Procedures

**Maintenance Announcement Template**:
```
Subject: Scheduled Maintenance - OpenKoda Platform

Dear Users,

We will be performing scheduled maintenance on the OpenKoda platform:

Date: Sunday, February 4, 2024
Time: 02:00 AM - 04:00 AM EST
Duration: Up to 2 hours

During this window:
- Application will be temporarily unavailable
- All active sessions will be terminated
- No data loss is expected

Changes included:
- Application upgrade to version 1.8.0
- Database performance optimizations
- Security patches

We apologize for any inconvenience.

OpenKoda Operations Team
```

### Rollback Readiness

**Rollback Decision Criteria**:
- Critical functionality broken
- Data corruption detected
- Performance degradation >50%
- Security vulnerability introduced

**Rollback Procedure**:
```bash
# Stop current version
systemctl stop openkoda

# Restore previous version
cp /backups/openkoda-v1.7.1.jar /opt/openkoda/openkoda.jar

# Restore database if needed
pg_restore -U postgres -d openkoda backup-pre-maintenance.dump

# Restart application
systemctl start openkoda
```

---

## 17. Upgrade Procedures

### Version Upgrade Steps

**Upgrade from 1.7.0 to 1.7.1**:

1. **Pre-Upgrade Backup**:
   ```bash
   pg_dump -U postgres -F c openkoda -f openkoda-pre-upgrade.dump
   tar -czf openkoda-data-backup.tar.gz /data
   ```

2. **Download New Version**:
   ```bash
   wget https://github.com/openkoda/openkoda/releases/download/v1.7.1/openkoda-1.7.1.jar
   ```

3. **Stop Application**:
   ```bash
   systemctl stop openkoda
   ```

4. **Replace JAR File**:
   ```bash
   cp openkoda-1.7.1.jar /opt/openkoda/openkoda.jar
   ```

5. **Run Database Migration**: OpenKoda uses `DbVersionService` for automated schema migrations.
   ```bash
   java -jar openkoda.jar --migrate-only
   ```

6. **Start Application**:
   ```bash
   systemctl start openkoda
   ```

7. **Verify Upgrade**:
   ```bash
   curl http://localhost:8080/ping
   tail -f /var/log/openkoda/openkoda.log
   ```

Source: openkoda/doc/installation.md:115-119

### Database Schema Migrations

**DbVersionService Automation**: OpenKoda automatically runs migration scripts from `/migration/core_upgrade.sql`.

**Migration Script Format**:
```sql
-- Version: 1.7.1
-- Description: Add new table for feature X

CREATE TABLE IF NOT EXISTS feature_x (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255)
);

INSERT INTO db_version (major, minor, build, revision, done, note)
VALUES (1, 7, 1, 0, true, 'Added feature X table');
```

Source: openkoda/src/main/resources/application-openkoda.properties:58-73

### Configuration Changes

**Review Configuration Differences**:
```bash
diff application-openkoda-1.7.0.properties application-openkoda-1.7.1.properties
```

**Update Custom Properties**: Merge new required properties into your configuration.

### Data Migration

**Export/Import Dynamic Entities**:
```bash
# Export before upgrade
curl http://localhost:8080/api/export/entities > entities-backup.json

# Import after upgrade
curl -X POST http://localhost:8080/api/import/entities -d @entities-backup.json
```

### Testing Procedures

**Post-Upgrade Test Suite**:
1. [ ] Application starts without errors
2. [ ] Database connections successful
3. [ ] User authentication works
4. [ ] Dynamic entities load correctly
5. [ ] File uploads/downloads function
6. [ ] Integrations (if configured) work
7. [ ] Admin panel accessible
8. [ ] Custom modules load

### Rollback Plan

**If Upgrade Fails**:
```bash
# Stop application
systemctl stop openkoda

# Restore previous version
cp /backups/openkoda-1.7.0.jar /opt/openkoda/openkoda.jar

# Restore database
dropdb openkoda
createdb openkoda
pg_restore -U postgres -d openkoda openkoda-pre-upgrade.dump

# Start application
systemctl start openkoda
```

### Docker Image Upgrades

**Pull New Image**:
```bash
docker pull openkoda/openkoda:1.7.1
```

**Update docker-compose.yaml**:
```yaml
services:
  openkoda:
    image: openkoda/openkoda:1.7.1
```

**Restart with New Image**:
```bash
docker-compose down
docker-compose up -d
```

---

## 18. Troubleshooting Guide

### Common Startup Issues

#### Database Connectivity Errors

**Symptom**: Application fails to start with "Connection refused" or "Unable to connect to database"

**Diagnosis**:
```bash
# Check PostgreSQL is running
systemctl status postgresql

# Test database connection
psql -h localhost -U postgres -d openkoda
```

**Solution**:
```bash
# Start PostgreSQL
systemctl start postgresql

# Verify connection string
echo $SPRING_DATASOURCE_URL

# Check firewall rules
ufw status
```

#### Classpath Issues

**Symptom**: `ClassNotFoundException` or `NoClassDefFoundError`

**Solution**:
```bash
# Verify JAR integrity
jar tf openkoda.jar | grep "com/openkoda/App.class"

# Use loader.path correctly
java -Dloader.path=/BOOT-INF/classes -jar openkoda.jar
```

Source: openkoda/doc/installation.md:119, docker/entrypoint.sh:10

#### Port Already in Use

**Symptom**: `Address already in use: bind`

**Diagnosis**:
```bash
lsof -i :8080
netstat -tuln | grep 8080
```

**Solution**:
```bash
# Change port
java -jar openkoda.jar --server.port=8081

# Or kill conflicting process
kill -9 <PID>
```

### Performance Problems

#### Slow Query Execution

**Diagnosis**:
```sql
-- Enable pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Find slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE mean_exec_time > 1000
ORDER BY mean_exec_time DESC
LIMIT 10;
```

**Solution**:
- Add appropriate indexes
- Optimize query structure
- Increase connection pool size

#### Memory Leaks

**Diagnosis**:
```bash
# Monitor heap usage
jstat -gc <PID> 1000

# Generate heap dump
jmap -dump:live,format=b,file=heap.dump <PID>

# Analyze with VisualVM or Eclipse MAT
```

**Solution**:
```bash
# Increase heap size
java -Xms4g -Xmx8g -jar openkoda.jar

# Enable heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/openkoda -jar openkoda.jar
```

### Integration Failures

#### OAuth Connection Errors

**Symptom**: OAuth redirects fail or return errors

**Diagnosis**:
- Verify OAuth client ID and secret
- Check redirect URI configuration
- Confirm BASE_URL is correct

**Solution**:
```properties
# Verify BASE_URL matches OAuth redirect URI
base.url=https://openkoda.example.com
```

#### API Timeouts

**Symptom**: External API calls timeout

**Solution**:
```properties
# Increase timeout values
spring.mvc.async.request-timeout=14400000
```

Source: openkoda/src/main/resources/application-openkoda.properties:48

### Log Analysis Techniques

**Search for Errors**:
```bash
grep -i "error" /var/log/openkoda/openkoda.log
grep -i "exception" /var/log/openkoda/openkoda.log
```

**Follow Real-Time Logs**:
```bash
tail -f /var/log/openkoda/openkoda.log
```

**Extract Request Traces**:
```bash
# Find all logs for specific request ID
grep "requestId=abc123" /var/log/openkoda/openkoda.log
```

### Diagnostic Commands

**System Health**:
```bash
# CPU and memory usage
top -p $(pgrep -f openkoda.jar)

# Disk space
df -h

# Network connections
netstat -an | grep 8080
```

**JVM Diagnostics**:
```bash
# Thread dump
jstack <PID> > thread-dump.txt

# Heap summary
jmap -heap <PID>

# List JVM flags
java -XX:+PrintFlagsFinal -version | grep -i heap
```

**Database Diagnostics**:
```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Lock information
SELECT * FROM pg_locks WHERE NOT granted;

-- Table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## 19. Support Channels

### Official Documentation

**Primary Documentation Resources**:
- GitHub Repository: https://github.com/openkoda/openkoda
- Official Website: https://openkoda.com
- Generated API Documentation: `target/site/apidocs/index.html`

**Internal Documentation**:
- Installation Guide: `openkoda/doc/installation.md`
- 5-Minute Quick Start: `openkoda/doc/5-minute-guide.md`
- Admin Guide: `openkoda/doc/admin.md`
- Application Development: `openkoda/doc/app-development.md`

Source: openkoda/doc/

### GitHub Issues

**Bug Reports**: https://github.com/openkoda/openkoda/issues

**Issue Template** (for bug reports):
```
**Description**: Brief description of the issue

**Environment**:
- OpenKoda Version: 1.7.1
- Java Version: 21.0.5
- PostgreSQL Version: 14.4
- Operating System: Ubuntu 22.04

**Steps to Reproduce**:
1. Step one
2. Step two
3. ...

**Expected Behavior**: What should happen

**Actual Behavior**: What actually happens

**Logs**: Attach relevant log excerpts
```

**Feature Requests**: Use GitHub Discussions for feature proposals.

### Community Forums

**Discussion Channels**:
- GitHub Discussions: https://github.com/openkoda/openkoda/discussions
- Stack Overflow: Tag questions with `openkoda`

**Community Guidelines**:
- Search existing issues before posting
- Provide complete context and logs
- Be respectful and constructive

### Commercial Support Options

**Enterprise Support**: Contact Openkoda CDX Sp. z o.o. for commercial support agreements.

**Support Tiers**:
- **Community**: GitHub issues and discussions (free)
- **Professional**: Email support with 48-hour response time
- **Enterprise**: 24/7 support with dedicated account manager

Contact: info@openkoda.com

### Emergency Escalation Contacts

**Critical Issues** (Production Down):
1. Create GitHub issue with "CRITICAL" label
2. Email: support@openkoda.com (Subject: CRITICAL - Production Issue)
3. Include: Environment details, error logs, steps to reproduce

**Response Time Expectations**:
- Critical (P1): 4 hours
- High (P2): 1 business day
- Medium (P3): 3 business days
- Low (P4): Best effort

### Contributing

**Contribution Guidelines**: See `CONTRIBUTING.md` in repository root.

**How to Contribute**:
1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit pull request
5. Address code review feedback

Source: openkoda/CONTRIBUTING.md

### Security Vulnerabilities

**Report Security Issues**: Email security@openkoda.com (do NOT use public GitHub issues)

**Include**:
- Vulnerability description
- Steps to reproduce
- Potential impact
- Suggested fix (if applicable)

**Security Response Timeline**:
- Acknowledgment: 48 hours
- Initial assessment: 7 days
- Fix and disclosure: 30-90 days (depending on severity)

---

## 20. Operational Runbooks

### Daily Operations Checklist

**Morning Health Checks** (10 minutes):
```bash
#!/bin/bash
# Daily health check script

echo "=== OpenKoda Daily Health Check ==="
echo "Date: $(date)"

# Check application status
echo "1. Application Status:"
systemctl status openkoda | grep "Active:"

# Check application health endpoint
echo "2. Health Endpoint:"
curl -s http://localhost:8080/ping || echo "FAILED"

# Check database connectivity
echo "3. Database Status:"
psql -U postgres -d openkoda -c "SELECT 1;" > /dev/null && echo "OK" || echo "FAILED"

# Check disk space
echo "4. Disk Space:"
df -h | grep -E "/$|/data"

# Check log for errors
echo "5. Recent Errors:"
grep -i "error" /var/log/openkoda/openkoda.log | tail -5

# Check active users (last 24 hours)
echo "6. Active Users:"
psql -U postgres -d openkoda -c "SELECT COUNT(DISTINCT user_id) FROM audit_log WHERE created_on > NOW() - INTERVAL '24 hours';"

echo "=== Health Check Complete ==="
```

**Daily Tasks**:
1. [ ] Review application logs for errors
2. [ ] Check disk space usage
3. [ ] Verify database backup completion
4. [ ] Monitor CPU and memory usage
5. [ ] Review slow query log
6. [ ] Check active user sessions

### Weekly Maintenance Tasks

**Weekly Checklist** (30 minutes):
1. [ ] Review Grafana dashboards for anomalies
2. [ ] Analyze database performance metrics
3. [ ] Check database vacuum/analyze completion
4. [ ] Review security logs
5. [ ] Update documentation for any configuration changes
6. [ ] Test backup restoration (sample test)
7. [ ] Review and close resolved GitHub issues

**Database Maintenance Script**:
```bash
#!/bin/bash
# Run weekly during low-traffic period

echo "Running weekly database maintenance..."

# Vacuum and analyze
psql -U postgres -d openkoda -c "VACUUM ANALYZE;"

# Reindex if needed
psql -U postgres -d openkoda -c "REINDEX DATABASE openkoda;"

# Update statistics
psql -U postgres -d openkoda -c "ANALYZE;"

# Check for bloat
psql -U postgres -d openkoda -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) FROM pg_tables WHERE schemaname='public' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC LIMIT 10;"

echo "Weekly maintenance complete."
```

### Monthly Maintenance Procedures

**Monthly Tasks** (2 hours):
1. [ ] Apply security patches to operating system
2. [ ] Review and update dependencies
3. [ ] Analyze long-term performance trends
4. [ ] Review access logs and user permissions
5. [ ] Test disaster recovery procedures (quarterly)
6. [ ] Update SSL certificates if expiring within 30 days
7. [ ] Review and optimize database queries
8. [ ] Generate capacity planning report
9. [ ] Review and update documentation
10. [ ] Conduct security audit

**Security Patch Application**:
```bash
#!/bin/bash
# Monthly security updates

# Update package lists
apt update

# List available security updates
apt list --upgradable | grep -i security

# Apply updates (requires review)
apt upgrade -y

# Reboot if kernel updated
[ -f /var/run/reboot-required ] && shutdown -r +5 "System reboot for security updates in 5 minutes"
```

### Incident Response Playbook

**Severity Levels**:
- **P1 (Critical)**: Production down, data loss, security breach
- **P2 (High)**: Major functionality broken, significant performance degradation
- **P3 (Medium)**: Minor functionality broken, workaround available
- **P4 (Low)**: Cosmetic issues, feature requests

**Incident Response Steps**:

1. **Identify and Assess** (5 minutes):
   - Determine severity level
   - Identify affected systems and users
   - Document initial symptoms

2. **Notify Stakeholders** (10 minutes):
   - Notify operations team via Slack/email
   - Update status page if customer-facing
   - Escalate to management if P1/P2

3. **Investigate** (varies):
   ```bash
   # Collect diagnostic data
   systemctl status openkoda
   tail -100 /var/log/openkoda/openkoda.log
   psql -U postgres -d openkoda -c "SELECT * FROM pg_stat_activity;"
   ```

4. **Implement Fix** (varies):
   - Apply immediate mitigation
   - Test fix in staging if possible
   - Deploy to production

5. **Verify Resolution** (15 minutes):
   - Confirm issue resolved
   - Check for side effects
   - Monitor for recurrence

6. **Communicate Resolution**:
   - Notify stakeholders of resolution
   - Update status page
   - Close incident ticket

7. **Post-Mortem** (within 48 hours):
   - Document root cause
   - Identify preventive measures
   - Update runbooks
   - Schedule improvements

### Escalation Paths

**Escalation Matrix**:

| Severity | Initial Contact | Escalation 1 (30 min) | Escalation 2 (60 min) |
|----------|----------------|----------------------|----------------------|
| P1 | On-call engineer | Engineering lead | CTO |
| P2 | On-call engineer | Engineering lead | Engineering manager |
| P3 | Ticket assignment | Engineering lead | - |
| P4 | Ticket assignment | - | - |

**On-Call Rotation**: Maintain 24/7 on-call coverage for P1/P2 incidents.

### Post-Mortem Procedures

**Post-Mortem Template**:

```markdown
# Incident Post-Mortem

**Date**: YYYY-MM-DD
**Severity**: P1/P2/P3/P4
**Duration**: X hours Y minutes
**Incident Owner**: Name

## Summary
Brief description of what happened.

## Timeline
- HH:MM - Incident detected
- HH:MM - Engineering team notified
- HH:MM - Root cause identified
- HH:MM - Fix implemented
- HH:MM - Service restored

## Root Cause
Technical explanation of what caused the incident.

## Impact
- Users affected: X
- Revenue impact: $Y
- Data loss: Yes/No

## Resolution
What was done to resolve the incident.

## Preventive Measures
1. Action item 1 (Owner: Name, Due: Date)
2. Action item 2 (Owner: Name, Due: Date)

## Lessons Learned
What we learned and what we'll do differently.
```

**Follow-Up**:
- Schedule action items
- Update documentation
- Share learnings with team
- Implement monitoring improvements

---

## Appendix

### Environment Variable Quick Reference

| Variable | Default | Required |
|----------|---------|----------|
| SPRING_DATASOURCE_URL | jdbc:postgresql://localhost:5432/openkoda | Yes |
| SPRING_DATASOURCE_USERNAME | postgres | Yes |
| SPRING_DATASOURCE_PASSWORD | postgres | Yes |
| BASE_URL | http://localhost:8080/ | Yes |
| SPRING_PROFILES_ACTIVE | openkoda,development | Yes |
| FILE_STORAGE_FILESYSTEM_PATH | /data | No |
| STORAGE_TYPE | db | No |
| INIT_ADMIN_USERNAME | admin | No |
| INIT_ADMIN_PASSWORD | admin123 | No |

### Common Commands Reference

```bash
# Build from source
mvn -f openkoda/pom.xml clean install spring-boot:repackage -DskipTests

# Start application
java -Dloader.path=/BOOT-INF/classes -Dspring.profiles.active=openkoda -jar openkoda.jar --server.port=8080

# Database backup
pg_dump -U postgres -F c openkoda -f openkoda-backup.dump

# Database restore
pg_restore -U postgres -d openkoda openkoda-backup.dump

# View logs
tail -f /var/log/openkoda/openkoda.log

# Health check
curl http://localhost:8080/ping
```

### Version History

- **1.7.1**: Current version
- **1.7.0**: Previous stable release
- See full changelog: https://github.com/openkoda/openkoda/releases

---

**Document Version**: 1.0  
**Last Updated**: 2024-01-15  
**Maintained By**: OpenKoda Operations Team

For questions or feedback on this deployment guide, please open an issue on GitHub or contact support@openkoda.com.
