# Manual: backend Spring Boot en EC2 + PostgreSQL en RDS (Free Tier)

2026-10-03 · Mariano

## Cómo funciona

Tu API Spring Boot corre en una instancia EC2 y se conecta a un PostgreSQL administrado (RDS) dentro de la misma red; solo la EC2 puede hablar con la base de datos. Este manual continúa el de S3 + CloudFront (`DEPLOY-AWS-MANUAL.md`): el frontend sigue en S3 y CloudFront enruta `/api/*` hacia la EC2.

```
Navegador ──HTTPS──▶ CloudFront ──/*────▶ S3 (privado, frontend estático)
                         │
                         └────/api/*──▶ EC2 (Spring Boot, Java 25, :8080)  ──5432──▶ RDS PostgreSQL (no público)
                                        rentals-app-sg                              rentals-db-sg
                                        └──────────────── VPC por defecto ───────────────┘
Administración de la EC2: Session Manager (SSM), sin puerto 22 abierto.
```

**Decisiones que toma este manual** (puedes cambiarlas, pero los pasos asumen estas):

- **Región:** la misma de tu bucket (por ejemplo `us-east-1`). Todo vive en una sola región.
- **Red:** la VPC por defecto, para no pagar NAT Gateway ni armar subredes a mano.
- **Base de datos:** RDS PostgreSQL `db.t4g.micro`, Single-AZ, sin acceso público.
- **Servidor:** EC2 con Amazon Linux 2023 y Amazon Corretto 25 (Java 25), gestionado con `systemd`.
- **Acceso a la EC2:** Session Manager (SSM), sin abrir el puerto 22.
- **Secretos:** Parameter Store (gratis en nivel estándar) en vez de Secrets Manager (cobra por secreto).
- **HTTPS:** lo da CloudFront; la EC2 solo habla HTTP con CloudFront.

## Paso 0: antes de empezar

Prepara la cuenta, el control de costos y los nombres que usarás; todo lo demás depende de esto.

1. **Identidad:** entra con un usuario IAM o IAM Identity Center, no con root. Activa MFA en el root.
2. **Presupuesto:** Billing and Cost Management → Budgets → Create budget (por ejemplo 5 USD/mes). En Billing → Billing preferences activa las alertas de uso del Free Tier.
3. **Región:** elige la misma de tu bucket y no la cambies durante el manual (esquina superior derecha de la consola).
4. **En tu computador:** JDK 25, Maven o Gradle, y un proyecto Spring Boot que genere un JAR ejecutable con `spring-boot-starter-actuator` (para el endpoint `/actuator/health`). Usa una versión de Spring Boot que soporte Java 25 (3.5.x reciente o 4.x; verifícalo en la documentación de Spring).
5. **Convención de nombres** (así los pasos y los problemas frecuentes se leen igual):

| Recurso | Nombre |
| --- | --- |
| Security group de la EC2 | `rentals-app-sg` |
| Security group de la base | `rentals-db-sg` |
| Instancia RDS | `rentals-db` |
| Base de datos | `rentals` |
| Usuario administrador | `rentals_admin` |
| Usuario de la aplicación | `rentals_app` |
| Rol IAM de la EC2 | `rentals-ec2-role` |
| Instancia EC2 | `rentals-api` |
| Bucket de despliegue | `rentals-deploy-<tu-nombre>-001` |

## Paso 1: red y security groups

Usarás la VPC por defecto y dos security groups encadenados: la base solo acepta tráfico que venga de la EC2.

**1.1 Verifica la VPC por defecto**

1. Consola → **VPC → Your VPCs**. Debe haber una con *Default VPC = Yes*.
2. Si no existe: **Actions → Create default VPC**.
3. En **Subnets** comprueba que hay una subred por zona de disponibilidad (por ejemplo `us-east-1a`, `us-east-1b`...). Todas son públicas en la VPC por defecto.

**1.2 Crea `rentals-app-sg` (para la EC2)**

1. **EC2 → Security Groups → Create security group**.
2. Name: `rentals-app-sg`. Description: `API Spring Boot`. VPC: la por defecto.
3. **Inbound rules:** déjalas vacías por ahora. Entrarás por SSM y el puerto 8080 se abre en el Paso 7.
4. **Outbound rules:** deja la regla por defecto (todo el tráfico saliente). La EC2 la necesita para instalar paquetes y hablar con SSM.
5. **Create security group** y anota su ID (`sg-xxxxxxxx`).

**1.3 Crea `rentals-db-sg` (para RDS)**

1. **Create security group** de nuevo. Name: `rentals-db-sg`. Description: `PostgreSQL desde la API`.
2. **Inbound rules → Add rule:**

| Type | Protocol | Port | Source |
| --- | --- | --- | --- |
| PostgreSQL | TCP | 5432 | Custom → selecciona `rentals-app-sg` |

3. **Create security group.**

La fuente es otro security group, no una IP: así la regla sigue válida aunque la EC2 cambie de dirección, y nada más en Internet puede llegar al puerto 5432.

## Paso 2: crear la base de datos RDS PostgreSQL

Crearás una sola instancia pequeña, Single-AZ y sin acceso público; tarda unos 10 minutos en quedar disponible. Los nombres de las opciones pueden variar un poco con el tiempo.

1. Consola → **RDS → Databases → Create database**.
2. **Choose a database creation method:** *Standard create* (si ves una opción *Full configuration*, es la misma idea).
3. **Engine type:** PostgreSQL. **Engine version:** la versión estable más reciente que ofrezca la consola (por ejemplo 17.x o 18.x).
4. **Templates:** *Free tier* si aparece; si no, *Dev/Test*. Nunca *Production* (activa Multi-AZ).
5. **Settings:**
    - DB instance identifier: `rentals-db`.
    - Master username: `rentals_admin`.
    - Credentials management: *Self managed* y una contraseña larga. Guárdala en un gestor de contraseñas; la usarás una vez para crear el usuario de la aplicación. (*Managed in AWS Secrets Manager* funciona, pero cobra por secreto.)
6. **Instance configuration:** *Burstable classes* → `db.t4g.micro` (o `db.t3.micro`). Elige la que la consola marque como elegible para tu plan.
7. **Storage:** gp3, 20 GiB. Desactiva *Enable storage autoscaling* o pon un máximo bajo (por ejemplo 30 GiB).
8. **Availability:** *Single-AZ* (en la consola aparece como *Do not create a standby instance*).
9. **Connectivity:**
    - Compute resource: *Don't connect to an EC2 compute resource* (los security groups ya los creaste a mano).
    - VPC: la por defecto. DB subnet group: *default*.
    - **Public access: No.**
    - VPC security group: *Choose existing* → quita `default` y selecciona `rentals-db-sg`.
10. **Monitoring:** desactiva *Performance Insights* y *Enhanced Monitoring* (pueden generar cargos).
11. **Additional configuration:**
    - Initial database name: `rentals` (si lo dejas vacío no se crea ninguna base y tendrás que crearla tú).
    - Automated backups: déjalos activos; con retención de 1 a 7 días basta (el plan Free puede limitar el máximo).
    - Encryption: activada (por defecto).
    - **Deletion protection:** desactívala; es un laboratorio y si no, no podrás borrar la instancia sin pasos extra.
12. Revisa el cuadro de costos estimados y pulsa **Create database**.
13. Cuando el estado sea *Available*, abre la instancia y copia el **Endpoint** (`rentals-db.xxxxxxxx.us-east-1.rds.amazonaws.com`). Lo usarás en los pasos 4 y 5.

RDS exige conexiones cifradas en las versiones recientes de PostgreSQL; por eso la URL JDBC del Paso 5 lleva `sslmode=require`.

## Paso 3: crear la instancia EC2

Primero el rol IAM y el bucket de despliegue; luego la instancia y una IP fija.

**3.1 Bucket de despliegue (S3)**

1. **S3 → Create bucket**, nombre `rentals-deploy-<tu-nombre>-001`, misma región, *Block all public access* activado. Distinto del bucket del frontend.
2. Aquí subirás el JAR de cada versión.

**3.2 Rol IAM para la EC2**

1. **IAM → Roles → Create role** → *AWS service* → caso de uso *EC2*.
2. Adjunta la política administrada `AmazonSSMManagedInstanceCore` (permite usar Session Manager).
3. Name: `rentals-ec2-role` → **Create role**.
4. Abre el rol → **Add permissions → Create inline policy → JSON** y pega esto, con tus valores. Es el mínimo: leer el JAR y leer los parámetros de `/rentals/`.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::NOMBRE-BUCKET-DEPLOY"
    },
    {
      "Effect": "Allow",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::NOMBRE-BUCKET-DEPLOY/*"
    },
    {
      "Effect": "Allow",
      "Action": ["ssm:GetParameter", "ssm:GetParameters"],
      "Resource": "arn:aws:ssm:REGION:ID-DE-TU-CUENTA:parameter/rentals/*"
    }
  ]
}
```

5. Nombre de la política: `rentals-ec2-access`.

**3.3 Lanzar la instancia**

1. **EC2 → Instances → Launch instances.**
2. Name: `rentals-api`.
3. **AMI:** *Amazon Linux 2023* (Free tier eligible).
4. **Instance type:** `t3.micro`, o la que la consola marque como *Free tier eligible* para tu cuenta. Tiene solo 1 GiB de RAM: por eso en el Paso 5 limitas el heap de Java y creas un swap.
5. **Key pair:** *Proceed without a key pair*. Entrarás por Session Manager.
6. **Network settings → Edit:**
    - VPC: la por defecto. Subnet: cualquiera pública; si quieres, la misma zona de disponibilidad que RDS.
    - Auto-assign public IP: *Enable*.
    - Firewall: *Select existing security group* → `rentals-app-sg`.
7. **Storage:** 1 volumen gp3 de 10 GiB.
8. **Advanced details → IAM instance profile:** `rentals-ec2-role`. Deja *Metadata version* en *V2 only* (IMDSv2).
9. **Launch instance** y espera a *Running* con *2/2 checks passed*.

**3.4 IP fija (Elastic IP)**

Sin ella, la IP pública y el nombre DNS cambian cada vez que detienes y arrancas la instancia, y CloudFront perdería su origen.

1. **EC2 → Elastic IPs → Allocate Elastic IP address → Allocate.**
2. Selecciónala → **Actions → Associate Elastic IP address** → instancia `rentals-api` → **Associate**.
3. Anota el *Public IPv4 DNS* de la instancia (`ec2-x-x-x-x.compute-1.amazonaws.com`).

AWS cobra por toda dirección IPv4 pública (alrededor de 0,005 USD por hora, unos 3,65 USD al mes), esté asociada o no. Libera la Elastic IP al terminar (Limpieza).

## Paso 4: conectar a la EC2 y probar RDS con psql

Entra por Session Manager, comprueba que la EC2 llega a la base y crea un usuario con permisos limitados para la aplicación.

1. **EC2 → Instances →** selecciona `rentals-api` **→ Connect → Session Manager → Connect.** Se abre una terminal en el navegador.
    - Si el botón está deshabilitado, espera 2 a 5 minutos tras el arranque y revisa que la instancia tenga el rol `rentals-ec2-role` y salida a Internet.
2. Actualiza el sistema e instala el cliente de PostgreSQL:

```bash
sudo dnf update -y
sudo dnf install -y postgresql17
```

Si el paquete no existe, busca el nombre vigente con `dnf search postgresql` (por ejemplo `postgresql16`).

3. Conéctate a la base con el usuario administrador (te pedirá la contraseña):

```bash
psql "host=ENDPOINT-DE-RDS port=5432 dbname=rentals user=rentals_admin sslmode=require"
```

4. Dentro de `psql`, crea el usuario de la aplicación. La contraseña se asigna con `\password` para que no quede en el historial de comandos:

```sql
CREATE ROLE rentals_app LOGIN;
\password rentals_app
GRANT CONNECT ON DATABASE rentals TO rentals_app;
GRANT USAGE, CREATE ON SCHEMA public TO rentals_app;
\q
```

5. Prueba el usuario nuevo: `psql "host=ENDPOINT-DE-RDS dbname=rentals user=rentals_app sslmode=require"` y ejecuta `SELECT 1;`.

La aplicación usará `rentals_app`, nunca el administrador. Así un fallo de seguridad en la API no da control total de la instancia.

Si `psql` se queda esperando y termina en *timeout*, casi siempre es el security group: revisa que `rentals-db-sg` acepte el puerto 5432 desde `rentals-app-sg` y que la instancia RDS use ese security group.

## Paso 5: instalar Java 25 y desplegar el JAR de Spring Boot

Instalarás Corretto 25, subirás el JAR a S3 y lo ejecutarás como servicio de `systemd` con la memoria limitada para una máquina de 1 GiB. Todo en la terminal de Session Manager.

**5.1 Swap de 1 GiB (red de seguridad contra falta de memoria)**

```bash
sudo dd if=/dev/zero of=/swapfile bs=128M count=8
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

**5.2 Java 25**

```bash
sudo dnf install -y java-25-amazon-corretto-headless
java -version
```

Si el paquete no aparece, ejecuta `dnf search corretto` para ver los nombres disponibles.

**5.3 Usuario y carpetas del servicio**

```bash
sudo useradd -r -s /sbin/nologin rentals
sudo mkdir -p /opt/rentals /etc/rentals
sudo chown rentals:rentals /opt/rentals
```

**5.4 Subir y descargar el JAR**

En tu computador, genera el JAR y súbelo al bucket de despliegue (con un usuario IAM de despliegue con permisos mínimos, no root):

```bash
mvn clean package
aws s3 cp target/rentals-api.jar s3://NOMBRE-BUCKET-DEPLOY/releases/rentals-api.jar
```

En la EC2:

```bash
sudo aws s3 cp s3://NOMBRE-BUCKET-DEPLOY/releases/rentals-api.jar /opt/rentals/rentals-api.jar
sudo chown rentals:rentals /opt/rentals/rentals-api.jar
```

**5.5 Configuración por variables de entorno**

Crea `/etc/rentals/rentals.env` con `sudo nano /etc/rentals/rentals.env`:

```properties
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://ENDPOINT-DE-RDS:5432/rentals?sslmode=require
SPRING_DATASOURCE_USERNAME=rentals_app
SERVER_PORT=8080
```

No pongas la contraseña aquí: la cargas en el Paso 6. Restringe el archivo:

```bash
sudo chmod 600 /etc/rentals/rentals.env
```

Tip de diseño: mantén la configuración de infraestructura fuera del dominio. En Spring, léela con `@ConfigurationProperties` y un `application-prod.properties` que solo referencie variables de entorno; así el código de negocio no conoce ni la URL ni las credenciales. Si usas Flyway para el esquema, activa `spring.jpa.hibernate.ddl-auto=validate` en producción.

**5.6 Servicio systemd**

Crea `/etc/systemd/system/rentals.service`:

```ini
[Unit]
Description=Rentals API
After=network-online.target
Wants=network-online.target

[Service]
User=rentals
EnvironmentFile=/etc/rentals/rentals.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError -jar /opt/rentals/rentals-api.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
```

Actívalo (todavía fallará la conexión a la base hasta cargar la contraseña en el Paso 6):

```bash
sudo systemctl daemon-reload
sudo systemctl enable rentals
```

## Paso 6: credenciales con Parameter Store

La contraseña de `rentals_app` vive cifrada en Parameter Store; la EC2 la lee al arrancar con su rol IAM y nunca queda escrita en el JAR ni en el repositorio.

**6.1 Crear el parámetro**

1. **Systems Manager → Parameter Store → Create parameter.**
2. Name: `/rentals/db/password`. Tier: *Standard* (gratis). Type: *SecureString*. KMS key: la administrada por AWS (`alias/aws/ssm`).
3. Value: la contraseña que asignaste a `rentals_app`. Usa solo letras y números (evita `"`, `\` y `$`, que complican el archivo de entorno).
4. **Create parameter.**

El rol `rentals-ec2-role` ya permite leer `/rentals/*` (Paso 3.2).

**6.2 Script que carga el secreto al arrancar**

Crea `/usr/local/bin/rentals-load-secrets.sh`:

```bash
#!/bin/bash
set -euo pipefail
umask 077
mkdir -p /run/rentals
PASS=$(aws ssm get-parameter --name /rentals/db/password --with-decryption --query Parameter.Value --output text --region REGION)
echo "SPRING_DATASOURCE_PASSWORD=${PASS}" > /run/rentals/secrets.env
```

```bash
sudo chmod 700 /usr/local/bin/rentals-load-secrets.sh
```

El archivo se escribe en `/run`, que vive en memoria y se borra al reiniciar.

**6.3 Conectar el script al servicio**

Edita `/etc/systemd/system/rentals.service` y agrega estas dos líneas dentro de `[Service]`, junto a `EnvironmentFile`:

```ini
ExecStartPre=+/usr/local/bin/rentals-load-secrets.sh
EnvironmentFile=-/run/rentals/secrets.env
```

El prefijo `+` ejecuta el script con privilegios completos aunque el servicio corra como usuario `rentals`.

**6.4 Arrancar y verificar**

```bash
sudo systemctl daemon-reload
sudo systemctl start rentals
sudo systemctl status rentals
sudo journalctl -u rentals -n 50 --no-pager
curl -s localhost:8080/actuator/health
```

Debes ver `{"status":"UP"}`. Si aparece `password authentication failed`, revisa el parámetro; si aparece `Connection refused` o *timeout* hacia la base, vuelve al Paso 1.3.

**Secrets Manager en lugar de Parameter Store:** permite rotación automática, pero cobra por secreto al mes. Para un laboratorio con una sola contraseña, Parameter Store es suficiente.

## Paso 7: exponer la API y conectarla con CloudFront

Primero pruebas la API directo desde tu IP; después la publicas detrás de tu distribución de CloudFront, que aporta HTTPS y evita configurar CORS.

**7.1 Prueba directa (temporal)**

1. En `rentals-app-sg` → **Inbound rules → Edit → Add rule:** Custom TCP, puerto `8080`, Source *My IP*.
2. Desde tu computador: `curl http://IP-ELASTICA:8080/actuator/health`. Debe responder `UP`.
3. Cuando pase la prueba, elimina esa regla.

**7.2 Agregar la EC2 como origen en CloudFront**

1. **CloudFront →** tu distribución **→ Origins → Create origin.**
2. Origin domain: el *Public IPv4 DNS* de la EC2 (`ec2-x-x-x-x.compute-1.amazonaws.com`). Debe ser un nombre DNS, no una IP.
3. Protocol: *HTTP only*. HTTP port: `8080`. Name: `rentals-api`.
4. **Create origin.**

**7.3 Crear el comportamiento para `/api/*`**

1. **Behaviors → Create behavior.**
2. Path pattern: `/api/*`. Origin: `rentals-api`.
3. Viewer protocol policy: *Redirect HTTP to HTTPS*.
4. Allowed HTTP methods: *GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE*.
5. Cache policy: `CachingDisabled`. Origin request policy: `AllViewerExceptHostHeader`.
6. **Create behavior.**

CloudFront reenvía la ruta tal cual: tus controladores deben responder bajo `/api/...` (por ejemplo `@RequestMapping("/api/rentals")`). El frontend llama a `/api/...` con ruta relativa, así que el navegador ve un solo origen y no hace falta CORS.

**7.4 Cerrar el puerto 8080 a todos menos CloudFront**

1. En `rentals-app-sg` → **Inbound rules → Add rule:** Custom TCP, puerto `8080`, Source: la *prefix list* administrada `com.amazonaws.global.cloudfront.origin-facing`.
2. Comprueba que no quede ninguna regla de 8080 abierta a `0.0.0.0/0` ni a tu IP.

Esa prefix list consume muchas reglas del límite de un security group; por eso la EC2 usa un grupo propio y limpio.

**7.5 Probar de extremo a extremo**

Espera a que la distribución termine de desplegar y abre `https://dxxxxxxxx.cloudfront.net/api/...` (o `/api/actuator/health` si expones actuator bajo ese prefijo con `management.endpoints.web.base-path`).

**Limitación a tener presente:** el tramo CloudFront → EC2 va en HTTP por Internet, sin cifrar. Para un laboratorio es aceptable. Para datos reales, pon un Application Load Balancer con certificado (ACM) delante de la EC2, o cifra ese tramo con HTTPS en el origen.

## Paso 8: operación diaria

Desplegar una versión nueva toma tres comandos; el resto de tareas se hace desde Session Manager.

**8.1 Script de despliegue**

Crea `/usr/local/bin/deploy-rentals.sh` en la EC2 y dale permisos (`sudo chmod 700`):

```bash
#!/bin/bash
set -euo pipefail
aws s3 cp s3://NOMBRE-BUCKET-DEPLOY/releases/rentals-api.jar /opt/rentals/rentals-api.jar.new
mv /opt/rentals/rentals-api.jar.new /opt/rentals/rentals-api.jar
chown rentals:rentals /opt/rentals/rentals-api.jar
systemctl restart rentals
```

Flujo completo: en tu computador `mvn clean package` y `aws s3 cp` al bucket; en la EC2 `sudo /usr/local/bin/deploy-rentals.sh`. Durante el reinicio la API no responde unos segundos.

**8.2 Tareas habituales**

| Tarea | Cómo |
| --- | --- |
| Ver el estado | `sudo systemctl status rentals` |
| Ver logs en vivo | `sudo journalctl -u rentals -f` |
| Reiniciar | `sudo systemctl restart rentals` |
| Actualizar el sistema | `sudo dnf update -y` y, si actualiza el kernel, reiniciar la instancia |
| Copia manual de la base | RDS → Snapshots → Take snapshot (antes de cambios grandes) |
| Restaurar | RDS → Snapshots → Restore snapshot (crea una instancia nueva) |
| Ahorrar mientras no practicas | EC2: Stop instance. RDS: Actions → Stop temporarily (AWS la reinicia sola a los 7 días) |

RDS hace copias automáticas diarias y permite restaurar a un punto en el tiempo dentro de la retención que configuraste. Las copias manuales no caducan: bórralas cuando ya no las necesites, porque se cobran.

## Costos, alertas y trampas del Free Tier

Lo que sale gratis depende de la fecha de creación de tu cuenta, y varios recursos cobran aunque no tengan tráfico.

- **Cuenta creada desde el 15 de julio de 2025 (plan Free):** recibes créditos (100 USD automáticos y hasta otros 100 USD por tareas de onboarding) que duran hasta 6 meses o hasta agotarse. EC2 y RDS descuentan de ese saldo a tarifa normal. Al terminar el plan Free tendrás que pasar a plan Paid o la cuenta se cierra; revisa las condiciones vigentes en la página del Free Tier.
- **Cuenta anterior a esa fecha:** 750 horas al mes de una instancia micro de EC2 y de RDS Single-AZ durante 12 meses.

| Recurso | ¿Cobra? | Cómo controlarlo |
| --- | --- | --- |
| EC2 `t3.micro` | Por hora encendida | Detenla si no practicas |
| Volumen EBS gp3 (10 GiB) | Por GiB al mes, aunque la EC2 esté detenida | Bórralo al terminar |
| RDS `db.t4g.micro` | Por hora encendida | Stop temporal (máx. 7 días) o bórrala |
| Almacenamiento RDS (20 GiB) y snapshots manuales | Por GiB al mes | Borra snapshots que no uses |
| IPv4 pública (EC2 y Elastic IP) | Unos 0,005 USD/h (≈ 3,65 USD/mes) cada una | Libera la Elastic IP al terminar |
| NAT Gateway | Por hora y por GB | No lo crees; la VPC por defecto no lo necesita |
| RDS Multi-AZ | Duplica el costo | Mantén Single-AZ |
| Performance Insights / Enhanced Monitoring | Puede cobrar | Déjalos desactivados |
| Secrets Manager | Por secreto al mes | Usa Parameter Store estándar |

La salida a Internet incluye un bloque mensual gratuito; confirma el límite vigente en la página de precios. Revisa **Billing → Cost Explorer** al día siguiente de crear los recursos y cada semana mientras dure el laboratorio.

## Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
| --- | --- | --- |
| Session Manager aparece deshabilitado | Falta el rol, el agente aún no registró, o no hay salida a Internet | Revisa el rol `rentals-ec2-role`, espera unos minutos y comprueba la IP pública y la regla de salida |
| `psql` o la API: *timeout* hacia RDS | Security group mal armado | `rentals-db-sg` debe aceptar 5432 desde `rentals-app-sg`; la instancia RDS debe usar `rentals-db-sg` |
| `no pg_hba.conf entry ... no encryption` | Conexión sin SSL | Añade `sslmode=require` a `psql` y a la URL JDBC |
| `password authentication failed` | Contraseña distinta o usuario mal escrito | Reasigna con `\password rentals_app` y actualiza el parámetro `/rentals/db/password` |
| `permission denied for schema public` | El usuario no tiene permisos en el esquema | Ejecuta `GRANT USAGE, CREATE ON SCHEMA public TO rentals_app;` como administrador |
| `database "rentals" does not exist` | No se indicó la base inicial al crear RDS | Conéctate a `postgres` y ejecuta `CREATE DATABASE rentals;` |
| El servicio se reinicia en bucle | Falta de memoria o error de arranque | `journalctl -u rentals -n 100`; revisa `-Xmx512m` y el swap (`free -m`) |
| `Unable to locate credentials` en la EC2 | La instancia no tiene el rol asociado | EC2 → Actions → Security → Modify IAM role |
| `AccessDenied` al leer el parámetro | Región o ruta distinta a la de la política | Compara el ARN de la política con el nombre y la región del parámetro |
| CloudFront devuelve 502 o 504 en `/api/*` | El origen no responde en 8080 | Revisa `systemctl status rentals`, la regla del security group y que el origen use *HTTP only* en el puerto 8080 |
| La API responde 404 desde CloudFront | CloudFront no quita el prefijo `/api` | Haz que los controladores respondan bajo `/api/...` |
| Después de detener e iniciar la EC2 el sitio falla | La IP cambió porque no hay Elastic IP | Asocia una Elastic IP (Paso 3.4) y actualiza el origen en CloudFront |
| `Unsupported class file major version` | JAR compilado con una versión de Java mayor que la instalada | Compila con JDK 25 y ejecuta con Corretto 25 |

## Limpieza al terminar

Borra en este orden para que ningún recurso bloquee a otro, y confirma en Billing al día siguiente que no queda ningún cargo.

1. **CloudFront:** si ya no la necesitas, elimina el comportamiento `/api/*` y el origen `rentals-api` (o toda la distribución: Disable, espera, Delete).
2. **EC2:** selecciona `rentals-api` → **Instance state → Terminate instance.** Eso también borra su volumen EBS.
3. **Elastic IP:** **EC2 → Elastic IPs →** selecciónala → **Release Elastic IP addresses.** Es el paso que más se olvida.
4. **RDS:** **RDS → Databases →** `rentals-db` **→ Actions → Delete.** Desmarca *Create final snapshot* y *Retain automated backups* si no los quieres; confirma escribiendo `delete me`.
5. **Snapshots manuales:** **RDS → Snapshots →** bórralos.
6. **Security groups:** borra primero `rentals-db-sg` y después `rentals-app-sg` (el primero hace referencia al segundo).
7. **Parameter Store:** elimina `/rentals/db/password`.
8. **IAM:** elimina el rol `rentals-ec2-role` y su política en línea.
9. **S3:** abre `rentals-deploy-<tu-nombre>-001` → **Empty** → **Delete.**
10. **Billing:** revisa **Cost Explorer** y **Free Tier** al día siguiente.
