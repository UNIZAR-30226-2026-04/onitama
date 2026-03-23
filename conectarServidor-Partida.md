# Guía de integración: Servidor ↔ Frontend Web (Partida)

> **Para:** Compañero de backend  
> **Contexto:** El frontend web tiene implementada la lógica de partida de manera local (mock) para poder probarla sin servidor. Este documento explica exactamente qué cambios hay que hacer en el frontend cuando el servidor esté listo, y qué contrato de mensajes WebSocket se espera.

---

## 1. Estado actual del mock

Actualmente, cuando el usuario pulsa **Partida Pública**, el flujo es:

```
/partidas → /buscar → (mock: 1.5 s de espera) → /partida?id=mock-partida-001
```

Una vez en `/partida`, el frontend:
- **Genera él mismo las 7 cartas aleatoriamente** (esto debe eliminarse con el servidor)
- **Simula al oponente con una IA local** que mueve aleatoriamente tras 900 ms (esto debe reemplazarse por los mensajes WS del servidor)

---

## 2. Contrato de mensajes WebSocket esperado

### 2.1 Variables de entorno necesarias

El frontend lee la URL del servidor desde `.env.local`:

```
NEXT_PUBLIC_WS_URL=ws://<IP_SERVIDOR>:8080
```

Si esta variable no está definida (o el servidor no responde), el frontend usa automáticamente el mock. Por tanto, **activar el servidor real es tan simple como añadir esta línea al `.env.local`**.

---

### 2.2 Mensajes que envía el CLIENTE al servidor

#### `BUSCAR_JUGADORES`
Se envia cuando busques un jugador, la raiz corresponde con la subcadena

```json
{
  "tipo": "BUSCAR_JUGADORES",
  "raiz": "Iron"
}
```

---

#### `ACEPTAR_AMISTAD`
Se envia cuando aceptes la solicitud, el remitente es quien te mando la solicitud

```json
{
  "tipo": "ACEPTAR_AMISTAD",
  "remitente": "Iron",
  "destinatario": "Taisen"
}
```

---

#### `SOLICITUD_AMISTAD`

```json
{
  "tipo": "SOLICITUD_AMISTAD",
  "remitente": "Iron",
  "destinatario": "Taisen"
}
```

---
HE CAMBIADO DE "ABANDONO" A "ABANDONAR", QUE ES EL MENSAJE QUE ENVÍA EL FRONTEND DESDE PARTIDA.TS

#### `ABANDONAR`
Se envía al pulsar el boton de abandonar en la partida.

```json
{
  "tipo": "ABANDONAR",
  "equipo": 1
}
```

---

#### `REGISTRARSE`
Se envía al abrir registrarse una nueva cuenta. Necesita nombre, contraseña sin hashear y correo del nuevo jugador

```json
{
  "tipo": "REGISTRARSE",
  "password": "1234",
  "nombre": "Iron",
  "correo": "taisen@irontaisen.com"
}
```

---

#### `INICIAR_SESION`
Se envía al abrir iniciar sesion. Necesita nombre, contraseña sin hashear.

```json
{
  "tipo": "INICIAR_SESION",
  "password": "1234",
  "nombre": "Iron"
}
```

---

#### `BUSCAR_PARTIDA`
Se envía al abrir `/buscar`. No tiene parámetros adicionales por ahora. MODIFICACION DEL BACKEND -> Se debe pasar tus puntos y nombres para decirselos a tu adversario y para buscar un adversario que este parejo

```json
{
  "tipo": "BUSCAR_PARTIDA",
  "puntos": 1000,
  "nombre": "Iron" 
}
```

**Archivo:** `src/api/buscarpartida.ts` → función `buscarPartida()`

---

#### `MOVER`
Se envía cuando el jugador ejecuta un movimiento. Solo se incluyen los datos mínimos (sin estado del tablero) para que el servidor valide. MODIFICACION DEL BACKEND -> El server no utiliza el id de la partida, asi que es irrelevante 

```json
{
  "tipo": "MOVER",
  "partida_id": "123",
  "col_origen": 3,
  "fila_origen": 6,
  "col_destino": 3,
  "fila_destino": 4,
  "carta": "Tigre",
  "equipo": 2
}
```

> **Importante:** Las coordenadas usan fila/col basadas en 0.  
> Fila 0 = arriba (equipo 1). Fila 6 = abajo (equipo 2).  
> El servidor debe validar que ese movimiento es legal con esa carta.

**Archivo:** `src/api/partida.ts` → función `enviarMovimiento()`

---

#### `CANCELAR`
Se envía cuando el jugador cancela la búsqueda de partida pública.
```json
{
  "tipo": "CANCELAR"
}
```

---

#### `RECHAZAR_AMISTAD`
Se envía cuando el jugador rechaza una solicitud de amistad. El `idNotificacion` se recibe
en la notificación original que llegó al hacer login.
```json
{
  "tipo": "RECHAZAR_AMISTAD",
  "idNotificacion": 1
}
```

--- 

### 2.2.1 Partidas privadas

#### `INVITACION_PARTIDA`
Se envía cuando el jugador quiere invitar a un amigo a una partida privada.
```json
{
  "tipo": "INVITACION_PARTIDA",
  "remitente": "Iron",
  "destinatario": "Taisen"
}
```

---

#### `ACEPTAR_INVITACION`
Se envía cuando el jugador acepta una invitación a partida privada. El `idNotificacion` se recibe en la notificación original.
```json
{
  "tipo": "ACEPTAR_INVITACION",
  "idNotificacion": 1
}
```

---

#### `RECHAZAR_INVITACION`
Se envía cuando el jugador rechaza una invitación a partida privada.
```json
{
  "tipo": "RECHAZAR_INVITACION",
  "idNotificacion": 1
}
```

---

### 2.3 Mensajes que envía el SERVIDOR al cliente

#### `INFORMACION_JUGADORES`

```json
{
  "tipo": "INFORMACION_JUGADORES",
  "info": [
    {
      "nombre": "Iron",
      "puntos": 10
    },
    {
      "nombre": "Iron1",
      "puntos": 25
    }
  ]
}
```

---
#### `NO_ENCONTRADOS`
Mensaje que manda el servidor si no encuentra ningun jugador con la subcadena

```json
{
  "tipo": "NO_ENCONTRADOS"
}
```

---
#### `AMISTAD_ACEPTADA`

```json
{
  "tipo": "AMISTAD_ACEPTADA",
  "amigo": "Iron"
}
```

---

#### `SOLICITUD_AMISTAD`

```json
{
  "tipo": "SOLICITUD_AMISTAD",
  "remitente": "Iron",
  "fecha_ini": "10/03/2026",
  "fecha_fin": "20/03/2026",
  "idNotificacion": 1
}
```

**Nota:** Dado que el frontend cierra el WebSocket tras el login, registro y fin de partida, este mensaje solo se recibe de forma fiable ==al hacer login==, cuando el servidor vuelca todas las solicitudes pendientes que llegaron mientras el jugador estaba desconectado.

---

#### `ERROR_SOLICITUD_AMISTAD`
Responde `ERROR_SOLICITUD_AMISTAD` si no se ha podido crear la notificación

```json
{
  "tipo": "ERROR_SOLICITUD_AMISTAD",
  "destinatario": "Taisen"
}
```

---

#### `ERROR_SESION_PSSWD`
Responde al `ERROR_SESION_PSSWD` si la contraseña es incorrecta.

```json
{
  "tipo": "ERROR_SESION_PSSWD"
}
```

---

#### `ERROR_SESION_USS`
Responde al `ERROR_SESION_USS` si no existe el usuario al que se intentan registrar.

```json
{
  "tipo": "ERROR_SESION_USS"
}
```

---

#### `REGISTRO_EXITOSO`
Confirma que el registro se ha completado correctamente.
```json
{
  "tipo": "REGISTRO_EXITOSO"
}
```

---

#### `REGISTRO_ERRONEO`
El registro ha fallado y puede ser porque el usuario o correo ya existe.
```json
{
  "tipo": "REGISTRO_ERRONEO"
}
```

---

#### `INICIO_SESION_EXITOSO`
Responde al `INICIO_SESION_EXITOSO` al jugador que se ha registrado correctamente.

```json
{
  "tipo": "INICIO_SESION_EXITOSO",
  "nombre": "Iron",
  "puntos": 1000,
  "correo": "itor@as.com",
  "partidas_ganadas": 2,
  "partidas_jugadas": 2,
  "cores": 221
}
```

> **Importante:**
> Puede que falten los amigos, notificaciones o skines, pero es una version basica

---

#### `VICTORIA`
Responde al `VICTORIA` al jugador que ha ganado la partida.

```json
{
  "tipo": "VICTORIA",
  "motivo": "FIN_PARTIDA",
  "equipo_responsable": 1
}
```

> **Importante:**
> Solo lo pasa si el jugador a ganado por causa externa, es decir si el realiza un movimiento que le da la partida NO SE LE AVISA. En caso de que se haya abandonado el motivo será "ABANDONO"

---

#### `DERROTA`
Responde al `DERROTA` al jugador que ha perdido la partida.

```json
{
  "tipo": "DERROTA"
}
```

---

#### `MOVIMIENTO_INVALIDO`
Responde al `MOVIMIENTO_INVALIDO` si detecta que el movimiento no es valido.

```json
{
  "tipo": "MOVIMIENTO_INVALIDO"
}
```

---

#### `CARTA_INVALIDA`
Responde al `CARTA_INVALIDA` si detecta que la carta jugada no está en la partida.

```json
{
  "tipo": "CARTA_INVALIDA"
}
```

---

#### `PARTIDA_ENCONTRADA`
Responde al `BUSCAR_PARTIDA` cuando se ha emparejado a dos jugadores. MODIFICACION DEL BACKEND -> El server te dira tambien que puntos tiene tu oponenete

```json
{
  "tipo": "PARTIDA_ENCONTRADA",
  "partida_id": "123",
  "equipo": 2,
  "oponente": "granluchador",
  "oponentePt": 1000,
  "cartas_jugador": [{
      "nombre": "Tigre",
      "movimientos": [{ "x": 0, "y": 1 },{ "x": 1, "y": 0 }]
    },
    {
      "nombre": "Dragon",
      "movimientos": [{ "x": -1, "y": 1 },{ "x": 1, "y": 1 }]
    }],
  "cartas_oponente": [{
      "nombre": "Rana",
      "movimientos": [ { "x": 0, "y": 2 } ]
    },
    {
      "nombre": "Conejo",
      "movimientos": [ { "x": -1, "y": -1 } ]
    }],
  "carta_siguiente": [{
      "nombre": "Oso",
      "movimientos": [ { "x": 1, "y": 1 }, { "x": 1, "y": -1 } ]
    }]
}
```

> **Importante:**
> - `equipo` indica si este cliente es equipo 1 (arriba, rojo) o equipo 2 (abajo, azul)
> - `cartas_jugador`, `cartas_oponente` y `carta_siguiente` deben ser exactamente los **nombres** de las cartas tal como están definidas en el frontend (ver sección 4)
> - Son 7 cartas en total: 2 + 2 + 3 (cola)
> - El servidor es quien elige las cartas aleatoriamente (no el cliente)

**Archivo:** `src/api/partida.ts` → interface `RespuestaPartidaEncontrada`

---

#### `MOVER`
Se envía al cliente cuando el **oponente** ha ejecutado un movimiento.

```json
{
  "tipo": "MOVER",
  "col_origen": 3,
  "fila_origen": 0,
  "col_destino": 3,
  "fila_destino": 2,
  "carta": "Dragon"
}
```

> **Importante:** El servidor solo envía este mensaje al cliente que NO movió (el oponente). El cliente que movió ya actualizó su estado local al enviar.

**Archivo:** `src/api/partida.ts` → interface `RespuestaMover`

---

### 2.3.1 Partidas privadas

#### `INVITACION_PARTIDA`
Se envía al destinatario cuando otro jugador le invita a una partida privada. Llega a la bandeja de notificaciones.
```json
{
  "tipo": "INVITACION_PARTIDA",
  "remitente": "Iron",
  "idNotificacion": 1
}
```

---

#### `PARTIDA_PRIVADA_ENCONTRADA`
Se envía a ambos jugadores cuando se acepta una invitación a partida privada. Tiene los mismos campos que `PARTIDA_ENCONTRADA`.
```json
{
  "tipo": "PARTIDA_PRIVADA_ENCONTRADA",
  "partida_id": "123",
  "equipo": 2,
  "oponente": "Taisen",
  "oponentePt": 1000,
  "cartas_jugador": [{
      "nombre": "Tigre",
      "movimientos": [{ "x": 0, "y": 1 },{ "x": 1, "y": 0 }]
    },
    {
      "nombre": "Dragon",
      "movimientos": [{ "x": -1, "y": 1 },{ "x": 1, "y": 1 }]
    }],
  "cartas_oponente": [{
      "nombre": "Rana",
      "movimientos": [ { "x": 0, "y": 2 } ]
    },
    {
      "nombre": "Conejo",
      "movimientos": [ { "x": -1, "y": -1 } ]
    }],
  "carta_siguiente": [{
      "nombre": "Oso",
      "movimientos": [ { "x": 1, "y": 1 }, { "x": 1, "y": -1 } ]
    }]
}
```

---

#### `INVITACION_RECHAZADA`
Se envía al remitente cuando el destinatario rechaza su invitación.
```json
{
  "tipo": "INVITACION_RECHAZADA"
}
```

---

#### `ERROR_DESCONECTADO`
Se envía al remitente cuando el destinatario no está conectado al intentar invitarle.
```json
{
  "tipo": "ERROR_DESCONECTADO"
}
```

---

#### `ERROR_NO_UNIDO`
Se envía al remitente cuando el temporizador de 2 minutos expira sin que el destinatario haya respondido.
```json
{
  "tipo": "ERROR_NO_UNIDO"
}
```

---

## 3. Cambios exactos en el frontend al conectar el servidor

### 3.1 `src/app/buscar/page.tsx`
**Cambio:** Ninguno. Ya está preparado. Cuando el servidor responda con `PARTIDA_ENCONTRADA`, el frontend navega automáticamente a `/partida?id=<partida_id>`.

---

### 3.2 `src/app/partida/page.tsx`

Este archivo tiene **dos bloques marcados con `TODO`** que hay que cambiar:

#### Cambio A: Crear estado inicial desde los datos del servidor

**Buscar este bloque** (función `PartidaInterna`):
```typescript
// MOCK ACTUAL - genera cartas aleatorias localmente
const [estado, setEstado] = useState<EstadoJuego>(() => crearEstadoInicial());
```

**Reemplazar por:**
```typescript
// CON SERVIDOR - crear estado a partir de los datos del mensaje PARTIDA_ENCONTRADA
// Los datos vendrían de useSearchParams o de un store global (Context/Zustand)
const [estado, setEstado] = useState<EstadoJuego>(() =>
  crearEstadoDesdeServidor(datosPartidaRecibidos)
);
```

Habrá que crear la función `crearEstadoDesdeServidor()` en `src/lib/juego.ts`:
```typescript
import { TODAS_LAS_CARTAS } from "./cartas";

export function crearEstadoDesdeServidor(datos: {
  cartasJugador: string[];
  cartasOponente: string[];
  cartasSiguientes: string[];
  equipo: EquipoID;
}): EstadoJuego {
  const buscar = (nombre: string) =>
    TODAS_LAS_CARTAS.find((c) => c.nombre === nombre)!;

  return {
    tablero: crearTableroInicial(),
    turnoActual: datos.equipo === 2 ? 2 : 1, // Según quién empieza (acordar con backend)
    cartasJugador: datos.cartasJugador.map(buscar),
    cartasOponente: datos.cartasOponente.map(buscar),
    cartasSiguientes: datos.cartasSiguientes.map(buscar),
    fichaSeleccionada: null,
    cartaSeleccionada: null,
    movimientosValidos: [],
    ganador: null,
    ultimoMovimiento: null,
  };
}
```

#### Cambio B: Eliminar la IA local y escuchar mensajes del servidor

**Buscar y eliminar este bloque** (el `useEffect` de la IA):
```typescript
// TODO: eliminar y sustituir por recepción de WS mensaje MOVER (api/partida.ts)
const ejecutarIa = useCallback((est: EstadoJuego) => { ... });
useEffect(() => {
  if (estado.turnoActual === 1 && !estado.ganador) ejecutarIa(estado);
}, [estado, ejecutarIa]);
```

**Reemplazar por:**
```typescript
import { conectarPartida, desconectarPartida, type RespuestaMover } from "@/api/partida";
import { TODAS_LAS_CARTAS } from "@/lib/cartas";

// Escuchar movimientos del oponente vía WebSocket
useEffect(() => {
  const desconectar = conectarPartida((msg) => {
    if (msg.tipo === "MOVER") {
      const m = msg as RespuestaMover;
      const carta = TODAS_LAS_CARTAS.find((c) => c.nombre === m.carta);
      if (!carta) return;

      setEstado((prev) => {
        const { nuevoEstado } = ejecutarMovimiento(
          prev,
          m.fila_origen,
          m.col_origen,
          m.fila_destino,
          m.col_destino,
          carta
        );
        return nuevoEstado;
      });
    }
  });
  return desconectar; // Cierra la conexión al salir de la pantalla
}, []);
```

#### Cambio C: Enviar el movimiento al servidor al ejecutarlo

**En la función `handleCelda`**, después de llamar a `ejecutarMovimiento()`:
```typescript
// Añadir tras setEstado(nuevoEstado):
enviarMovimiento({
  tipo: "MOVER",
  partida_id: partidaId,  // viene de useSearchParams
  col_origen: estado.fichaSeleccionada!.col,
  fila_origen: estado.fichaSeleccionada!.fila,
  col_destino: col,
  fila_destino: fila,
  carta: estado.cartaSeleccionada!.nombre,
});
```

---

## 4. Nombres de cartas disponibles

El servidor debe usar estos nombres **exactamente** (sensibles a mayúsculas/minúsculas) al enviar `PARTIDA_ENCONTRADA`:

```
Tigre, Dragon, Rana, Conejo, Cangrejo, Elefante,
Ganso, Gallo, Mono, Mantis, Caballo, Buey,
Grulla, Oso, Aguila, Cobra
```

Definidos en: `src/lib/cartas.ts` → array `TODAS_LAS_CARTAS`

---


## 5. Flujo completo con servidor activo

```
[Cliente A]                    [Servidor]                  [Cliente B]
    |                              |                              |
    |-- BUSCAR_PARTIDA ----------->|                              |
    |                              |<-------- BUSCAR_PARTIDA -----|
    |                              |                              |
    |                    (empareja A y B, elige 7 cartas,        |
    |                     guarda en Partida_Cartas_Mov)          |
    |                              |                              |
    |<-- PARTIDA_ENCONTRADA -------|                              |
    |    (equipo:2, cartas...)     |-- PARTIDA_ENCONTRADA ------->|
    |                              |    (equipo:1, cartas...)     |
    |                              |                              |
    | [Jugador A mueve]            |                              |
    |-- MOVER (origen,destino,carta)-->                          |
    |                    (valida, guarda en BD)                   |
    |                              |-- MOVER -(origen,destino,carta)->|
    |                              |    [Cliente B actualiza tablero] |
```

---

## 6. Archivos clave del frontend a conocer

| Archivo | Descripción |
|---|---|
| `src/api/buscarpartida.ts` | Cliente WS para buscar partida |
| `src/api/partida.ts` | Cliente WS para mensajes de juego (MOVER, etc.) |
| `src/lib/cartas.ts` | Definición estática de las 16 cartas y sus movimientos |
| `src/lib/juego.ts` | Lógica del tablero: tipos, inicialización, validación de movimientos |
| `src/app/buscar/page.tsx` | Pantalla de búsqueda (navega a /partida al encontrar) |
| `src/app/partida/page.tsx` | Pantalla de juego completa |
| `.env.local` | `NEXT_PUBLIC_WS_URL=ws://<IP>:8080` para activar el servidor |





## RECREAR PRUEBA :

1.- Abrir 3 terminales:

- Terminal 1: -> cd BaseDatos
              -> seguir el readme de esta carpeta para levantar la base de datos + contruir.sql

- Terminal 2: -> cd backend/java
             -> $LIB = "lib\Java-WebSocket-1.5.4.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar;lib\json-20231013.jar;lib\postgresql-42.2.5.jar;lib\jbcrypt-0.4.jar"
             -> javac -cp $LIB -d out Servidor.java VO\*.java JDBC\*.java gestor\*.java 
             (quiza necesites mkdir out)
             ->java -cp "out;$LIB" Servidor
- Terminal 3: -> cd webFinal
             -> npm install
             -> npm run dev  
             (es necesario tener npm instalado)


