# Onitama - Frontend Web

Frontend de la aplicación web del juego Onitama, desarrollado con Next.js.

---

## Requisitos previos

Antes de ejecutar el proyecto, necesitas tener instalado:

| Herramienta | Versión mínima | Descarga |
|-------------|----------------|----------|
| **Node.js** | 18 o superior  | [nodejs.org](https://nodejs.org/) |
| **npm**     | 9 o superior    | Viene incluido con Node.js       |

### Comprobar la instalación

Abre una terminal y ejecuta:

```bash
node --version   # Debe mostrar v18.x o superior
npm --version    # Debe mostrar 9.x o superior
```

---

## Pasos para ejecutar la web

### 1. Abrir la carpeta del proyecto

```bash
cd webFinal
```

### 2. Instalar dependencias

```bash
npm install
```

### 3. Iniciar el servidor de desarrollo

```bash
npm run dev
```

### 4. Abrir en el navegador

Abre [http://localhost:3000](http://localhost:3000) en tu navegador.

---

## Configurar la conexión con el servidor

El frontend puede funcionar de dos modos:

- **Sin servidor:** Usa datos mock (ideal para probar pantallas sin backend).
- **Con servidor:** Se conecta a la API Java para login y registro reales.

### Pasos para conectar con el servidor

1. **Crea el fichero de variables de entorno:**

   En Linux/Mac:
   ```bash
   touch .env.local
   ```

   En Windows (PowerShell):
   ```powershell
   New-Item .env.local -ItemType File
   ```

2. **Editar `.env.local`** y poner la siguiente linea:

   ```
   NEXT_PUBLIC_WS_URL=ws://localhost:8080
   ```

   Cambia `8080` por el puerto en el que corre tu servidor Java.

3. **Reiniciar el servidor de desarrollo** después de cambiar `.env.local`:

   ```bash
   npm run dev
   ```

4. **Ajustar los endpoints en `src/api/auth.ts`** según la API que exponga el servidor. Los endpoints actuales son ejemplos y pueden variar:

   - `POST /api/auth/verificar-email` – Verificar si el correo existe
   - `POST /api/auth/login` – Iniciar sesión
   - `POST /api/auth/registro` – Registrar nuevo usuario

---

## Estructura del proyecto

```
src/
├── api/           # Archivos que se conectan con el servidor
│   └── auth.ts    # Cliente API de autenticación (login, registro)
├── lib/           # Utilidades y lógica compartida (sin servidor)
│   └── validacion.ts
├── components/    # Componentes reutilizables
├── app/           # Páginas y rutas
```

---

## Comandos útiles

| Comando        | Descripción                    |
|----------------|--------------------------------|
| `npm run dev`  | Inicia el servidor de desarrollo |
| `npm run build`| Genera la versión de producción |
| `npm run start`| Ejecuta la versión de producción |
| `npm run lint` | Ejecuta el linter              |
