Paso 1: Abre la terminal en la carpeta donde esté el compose.yaml
Paso 2: Ejecuta el siguiente comando sin las comillas "docker compose up -d"
Paso 3: Ejecuta el siguiente comando para la creación de las tablas "Get-Content .\crear.sql | docker exec -i proy-postgres psql -U postgres -d postgres"

IMPORTANTE: Tienes que tener el docker instalado en tu ordenador y que el fichero crear.sql esté preferiblemente en la misma carpeta que el compose

Si no tienes docker: https://docs.docker.com/engine/install/