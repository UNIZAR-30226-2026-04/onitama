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

### 2.3 Mensajes que envía el SERVIDOR al cliente

#### `VICTORIA`
Responde al `VICTORIA` al jugador que ha ganado la partida.

```json
{
  "tipo": "VICTORIA"
}
```

> **Importante:**
> Solo lo pasa si el jugador a ganado por causa externa, es decir si el realiza un movimiento que le da la partida NO SE LE AVISA

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
