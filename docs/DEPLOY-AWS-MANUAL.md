# Manual: publicar una web estática en AWS (S3 + CloudFront), sin dominio propio

Guía para hacerlo tú manualmente desde la consola de AWS. Los nombres de botones pueden variar un poco
con el tiempo; la idea general se mantiene.

## Cómo funciona

```
Navegador  ──HTTPS──▶  CloudFront (URL tipo https://dxxxxxxxx.cloudfront.net)  ──▶  S3 (bucket PRIVADO)
```

- **S3** guarda tus archivos (`index.html`, CSS, JS, imágenes).
- **CloudFront** los entrega por HTTPS desde una URL gratuita `*.cloudfront.net` (no necesitas dominio).
- El bucket queda **privado**: solo CloudFront puede leerlo (mediante Origin Access Control, OAC).
  Es más seguro que el "hosting de sitio web" público de S3, que además no ofrece HTTPS.

## Paso 0: Antes de empezar

1. Entra a la consola con un usuario **IAM** (o IAM Identity Center), no con el usuario root. Activa MFA en el root.
2. Crea un aviso de costos: **Billing and Cost Management → Budgets → Create budget** (por ejemplo, 5 USD/mes).
3. Elige una región para el bucket. Para pruebas, `us-east-1` (N. Virginia) está bien. CloudFront es global.
4. Ten listos los archivos a subir: el **contenido** de la carpeta `src/` (no la carpeta en sí).
   `index.html` debe quedar en la raíz del bucket.

## Paso 1: Crear el bucket de S3

1. Consola → **S3 → Create bucket**.
2. **Bucket name:** debe ser único en todo AWS, en minúsculas. Ejemplo: `rentals-pruebas-mariano-001`.
3. **Object Ownership:** ACLs disabled (por defecto).
4. **Block all public access:** déjalo **activado** (todas las casillas marcadas).
5. **Versioning:** opcional; activarlo te permite recuperar versiones anteriores.
6. **Default encryption:** SSE-S3 (por defecto).
7. **Create bucket.**

## Paso 2: Subir los archivos

1. Abre el bucket → **Upload → Add files / Add folder**.
2. Sube el contenido de `src/` de modo que `index.html` quede en la raíz del bucket.
3. **Upload.**

## Paso 3: Crear la distribución de CloudFront

1. Consola → **CloudFront → Create distribution**.
2. **Origin domain:** elige tu bucket de la lista (el que termina en `.s3.<región>.amazonaws.com`).
   No uses el "website endpoint".
3. **Origin access:** selecciona **Origin access control settings (recommended)** →
   **Create new OAC** → déjalo con los valores por defecto (sign requests) → Create.
4. **Default root object:** `index.html`.
5. **Viewer protocol policy:** Redirect HTTP to HTTPS.
6. **Allowed HTTP methods:** GET, HEAD.
7. **Cache policy:** `CachingOptimized` (si vas a cambiar archivos muy seguido en pruebas, puedes usar `CachingDisabled`).
8. **WAF:** no lo actives para pruebas (tiene costo adicional).
9. Si la consola te pide elegir un plan de precios, elige la opción gratuita o más económica y revisa
   las condiciones antes de continuar.
10. **Create distribution.**

## Paso 4: Dar permiso a CloudFront para leer el bucket

Al crear la distribución con OAC, la consola muestra un aviso para actualizar la política del bucket.

1. Pulsa **Copy policy**.
2. Ve a **S3 → tu bucket → Permissions → Bucket policy → Edit**, pega la política y guarda.

La política tiene esta forma (con tus valores):

```json
{
  "Version": "2012-10-17",
  "Statement": {
    "Sid": "AllowCloudFrontServicePrincipalReadOnly",
    "Effect": "Allow",
    "Principal": { "Service": "cloudfront.amazonaws.com" },
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::NOMBRE-DEL-BUCKET/*",
    "Condition": {
      "StringEquals": {
        "AWS:SourceArn": "arn:aws:cloudfront::ID-DE-TU-CUENTA:distribution/ID-DE-LA-DISTRIBUCION"
      }
    }
  }
}
```

## Paso 5: Probar

1. En CloudFront, espera a que el estado de la distribución pase de *Deploying* a una fecha (unos minutos).
2. Copia el **Distribution domain name** (`dxxxxxxxx.cloudfront.net`) y ábrelo con `https://`.
3. Debes ver tu página.

## Paso 6: Actualizar la página después

1. Sube los archivos nuevos o modificados a S3 (sobrescribe los existentes).
2. Invalida la caché: **CloudFront → tu distribución → Invalidations → Create invalidation** →
   ruta `/*` → Create. Tarda un minuto o dos.
   (Las primeras rutas de invalidación de cada mes suelen ser gratuitas; confirma el límite vigente
   en la página de precios de CloudFront.)

## Paso 7 (opcional): Hacerlo con la línea de comandos

Solo cuando ya domines el proceso manual. Usa un usuario IAM con permisos mínimos, nunca el root.
Política mínima de despliegue (reemplaza los valores):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": "arn:aws:s3:::NOMBRE-DEL-BUCKET"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::NOMBRE-DEL-BUCKET/*"
    },
    {
      "Effect": "Allow",
      "Action": "cloudfront:CreateInvalidation",
      "Resource": "arn:aws:cloudfront::ID-DE-TU-CUENTA:distribution/ID-DE-LA-DISTRIBUCION"
    }
  ]
}
```

Comandos (los ejecutas tú, no los agentes):

```bash
aws s3 sync src/ s3://NOMBRE-DEL-BUCKET --delete
aws cloudfront create-invalidation --distribution-id ID-DE-LA-DISTRIBUCION --paths "/*"
```

## Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `AccessDenied` / 403 al abrir la URL | Falta o está mal la política del bucket, o `index.html` no está en la raíz | Revisa el Paso 4 y la ubicación de `index.html` |
| Ves la versión vieja | Caché de CloudFront | Crea una invalidación `/*` (Paso 6) |
| 403 al entrar a una subruta | Con bucket privado, un archivo inexistente responde 403 | Usa rutas a archivos reales (`/pagina.html`) o configura *Custom error responses* hacia `/index.html` si es una app de una sola página |
| `NoSuchBucket` | Elegiste mal el origen | Revisa el *Origin domain* de la distribución |
| La página carga sin estilos | Rutas absolutas (`/css/app.css`) o mayúsculas distintas | Usa rutas relativas y respeta exactamente nombres y mayúsculas |

## Limpieza al terminar las pruebas (para no pagar de más)

1. **CloudFront:** selecciona la distribución → **Disable**, espera a que termine → **Delete**.
2. **S3:** abre el bucket → **Empty** → luego **Delete**.
3. Revisa **Billing** al día siguiente para confirmar que no quedan cargos.

## Cuando el proyecto se vuelva dinámico

Dile al líder el nuevo requisito: el `architect` evaluará las opciones y las registrará en `docs/DECISIONS.md`.
Opciones habituales en AWS para añadir lógica a un sitio estático: API Gateway + Lambda (+ DynamoDB),
AWS Amplify, o un contenedor en App Runner/ECS. La web estática en S3 + CloudFront seguirá sirviendo el frontend.
