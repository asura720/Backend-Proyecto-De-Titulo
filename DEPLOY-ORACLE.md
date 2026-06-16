# Desplegar CuidApp Backend GRATIS en Oracle Cloud (Always Free)

Oracle Cloud regala una VM ARM (Ampere A1) **gratis para siempre** de hasta
**4 CPU y 24 GB de RAM** — suficiente para correr todo el stack siempre encendido
y sin pagar. Se usa el **mismo `docker-compose.aws.yml`** (todo se compila en ARM
sin cambios, porque las imágenes base ya son multi-arquitectura).

> Para pedir la cuenta Oracle te piden una tarjeta para verificar identidad, pero
> mientras uses **shapes "Always Free eligible"** no se cobra nada.

---

## 1. Crear la cuenta y la instancia

1. Crea cuenta en https://www.oracle.com/cloud/free/ (elige tu país y región).
2. En la consola: **Menu → Compute → Instances → Create instance.**
3. **Nombre:** `cuidapp-backend`
4. **Image and shape:**
   - **Image:** Canonical **Ubuntu 22.04**
   - **Shape:** clic en *Change shape* → **Ampere** → `VM.Standard.A1.Flex`
   - Ajusta a **2 OCPU** y **12 GB RAM** (dentro del Always Free; sobra para el stack).
     Verás la etiqueta verde **"Always Free-eligible"**.
5. **Add SSH keys:** elige *Generate a key pair for me* y **descarga la llave privada**
   (o sube tu clave pública si ya tienes una).
6. **Networking:** deja que cree una **VCN nueva** con subred pública. Asegúrate de
   "Assign a public IPv4 address".
7. **Create.** Espera a que quede en estado *Running* y anota la **IP pública**.

> 💡 Si te sale "Out of host capacity" al crear la A1: prueba otra *Availability
> Domain* en el formulario, u otra región cercana. Es temporal por demanda.

---

## 2. Abrir los puertos (¡DOS lugares! — el error más común)

En Oracle hay que abrir el puerto **en la red Y en el firewall del sistema**.

### 2a. En la red (Security List de la VCN)
1. Menu → **Networking → Virtual Cloud Networks** → tu VCN → **Subnet** → **Security List** (default).
2. **Add Ingress Rules:**
   | Source CIDR | Protocolo | Puerto destino | Para qué |
   |-------------|-----------|----------------|----------|
   | `0.0.0.0/0` | TCP | 22 | SSH |
   | `0.0.0.0/0` | TCP | 8083 | API Gateway (la app) |

### 2b. En el sistema operativo (la imagen Ubuntu de Oracle bloquea todo por defecto)
Después de conectarte por SSH (paso 3), ejecuta:
```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 8083 -j ACCEPT
sudo netfilter-persistent save
```
(El puerto 22 ya viene abierto.)

---

## 3. Conectarte por SSH

Desde tu PC, en la carpeta donde descargaste la llave:
```bash
chmod 400 ssh-key-*.key       # en Git Bash; en Windows usa Git Bash si da error de permisos
ssh -i ssh-key-*.key ubuntu@LA_IP_PUBLICA
```

---

## 4. Instalar Docker (una sola vez, en el servidor)

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
```

Verifica: `docker --version && docker compose version`

---

## 5. Subir el código

**Con Git (recomendado):**
```bash
git clone https://github.com/TU_USUARIO/Backend-Proyecto-De-Titulo-main.git
cd Backend-Proyecto-De-Titulo-main
```
**O con scp** desde tu PC (Git Bash, en la carpeta padre del backend):
```bash
scp -i ssh-key-*.key -r "Backend-Proyecto-De-Titulo-main" ubuntu@LA_IP_PUBLICA:~/
```

---

## 6. Configurar y levantar

```bash
cp .env.example .env
nano .env                 # cambia DB_PASSWORD por una contraseña segura
docker compose -f docker-compose.aws.yml up --build -d
```
La primera vez compila los 5 servicios en ARM (varios minutos). Verifica:
```bash
docker compose -f docker-compose.aws.yml ps
docker compose -f docker-compose.aws.yml logs -f apigateway   # Ctrl+C para salir
```

---

## 7. Probar y conectar la app

Desde tu PC:
```bash
curl http://LA_IP_PUBLICA:8083/api/auth/login -X POST -H "Content-Type: application/json" -d "{}"
```
Compila la app Flutter apuntando al servidor:
```bash
flutter build apk --dart-define=API_BASE_URL=http://LA_IP_PUBLICA:8083
```

---

## 8. "Que funcione siempre"

- Todos los servicios tienen `restart: always` → se reinician solos.
- La VM Always Free no se apaga ni cobra. La IP pública es estable.
- Los datos de MySQL persisten en el volumen `mysql-data`.

Comandos:
```bash
docker compose -f docker-compose.aws.yml restart
docker compose -f docker-compose.aws.yml up --build -d   # tras actualizar el código
```

---

## Notas
- **Costo: 0** mientras te quedes en el shape Always Free (Ampere A1, ≤4 OCPU / ≤24 GB total).
- ARM no es problema: las imágenes (`eclipse-temurin`, `mysql:8.0`) son multi-arquitectura
  y se compilan en el propio servidor.
- ¿HTTPS con dominio? Cuando quieras te dejo la config de Caddy (HTTPS automático y gratis).
