## GUÍA DE INSTALACIÓN & DESPLIEGUE BACKEND CON MAVEN

**IMPORTANTE**

asegúrate antes de instalar Maven de tener instalado el **JDK (Java Development Kit)** porque Maven lo necesita para compilar el código. 

Para ver si ya lo tienes instalado, ejecuta:
```cmd
java -version
```
<br>

**1. INSTALAR MAVEN** 

Ejecuta lo siguiente para ver si ya tienes Maven:
```cmd
mvn -version
```

Si no, entra a https://maven.apache.org/download.cgi y:

- Si usas Windows, descarga el "binary zip archive"

- Si usas Linux, el "bin tar.gz archive"

Una vez descargado, descomprímelo y ubica la carpeta donde sueles guardar el resto de programas (C:\ o C:\Program Files en Windows, por ejemplo).

Ahora copia la ruta de la carpeta /bin que está dentro de la carpeta descomprimida de Maven, y cópiala en el PATH.

Una vez hecho esto, vuelve a probar el comando de -version para ver si has instalado Maven correctamente.

<br>

**2. DESPLEGAR EL BACKEND CON MAVEN**
(Por ahora se tiene que hacer esto, pero seguramente el backend al final se despliegue con Docker a la vez que la base de datos).

Primero desde /onitama haz cd backend, que es la ruta donde está el archivo 'pom.xml' con dependencias. 

Una vez ya en onitama/backend, ejecuta

```cmd
mvn clean package
```

para compilar desde cero todo el backend con sus .jar necesarios, y luego: 

```cmd
java -jar target/backend.jar
```

Y ahí ya debería decir "Servidor iniciado, puerto -> 8080".