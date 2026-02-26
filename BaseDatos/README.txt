INSTALACIÓN INICIAL (Primera vez):
=====================================
Paso 1: Abre la terminal en la carpeta donde esté el compose.yaml
Paso 2: Ejecuta el siguiente comando sin las comillas "docker compose up -d"
Paso 3: Ejecuta el siguiente comando para la creación de las tablas "Get-Content .\crear.sql | docker exec -i proy-postgres psql -U postgres -d postgres"
Paso 4: (Opcional) Poblar con datos de prueba: "Get-Content .\construir.sql | docker exec -i proy-postgres psql -U postgres -d postgres"

ACTUALIZAR BASE DE DATOS EXISTENTE:
====================================
Si ya tienes datos en la base de datos y solo quieres añadir las nuevas columnas
Ejecuta: "Get-Content .\actualizar.sql | docker exec -i proy-postgres psql -U postgres -d postgres"
( En actualizar.sql pondras los comandos sql para añadir columnas nuevas,...)

(tambien puedes borrar todo y hacerlo todo de nuevo xd)
BORRAR Y RECREAR TABLAS:
=========================
Si quieres borrar todas las tablas y empezar de cero:
Paso 1: "Get-Content .\borrar.sql | docker exec -i proy-postgres psql -U postgres -d postgres"
Paso 2: "Get-Content .\crear.sql | docker exec -i proy-postgres psql -U postgres -d postgres"

VER DATOS:
==========
Para ver el contenido de las tablas: "Get-Content .\ver.sql | docker exec -i proy-postgres psql -U postgres -d postgres"

IMPORTANTE: Tienes que tener el docker instalado en tu ordenador y que los ficheros SQL estén preferiblemente en la misma carpeta que el compose

Si no tienes docker: https://docs.docker.com/engine/install/