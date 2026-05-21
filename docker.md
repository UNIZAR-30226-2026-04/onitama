**DESPLIEGUE DE LA BBDD Y DEL BACKEND CON DOCKER**

Los ejecutas desde el directorio /onitama.

- Para empezar y realizar un primer despliegue, vamos a tener que hacer 
```cmd
docker compose down -v
```
porque el volumen que guarda el contenido de la bbdd al desplegar se llama igual que el que había en el `compose.yaml` de la carpeta `/BaseDatos`. 

La próxima vez que hagas `docker compose up --build` volverán a ejecutarse los scripts desde cero.

# COMANDOS

| CASO | COMANDO |
|---|---|
| 1a vez o he cambiado codigo | `docker compose up --build` |
| he cambiado scripts sql | `docker compose down -v` → `docker compose up --build` |
| paro sin perder datos | `docker compose down` |
| para partir de cero | `docker compose down -v` |

También puedes usar -d para ejecutar en segundo plano, si no tendrás que ejecutar el up en una terminal, ahí verás los logs y en otra hacer el down.