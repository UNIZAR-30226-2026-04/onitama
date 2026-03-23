"use client";

/**
 * Pantalla principal del jugador autenticado.
 *
 * Menú lateral:
 *   - Mis cartas / Mis tableros / Tienda → "Próximamente"
 *   - Mis amigos → panel con dos pestañas: lista de amigos + buscar
 *   - Notificaciones → panel con solicitudes de amistad pendientes
 *
 * La campanita del sidebar muestra un badge con el número de notificaciones
 * pendientes. Se actualiza tanto con las que llegan al login (desde sessionStorage)
 * como con las que llegan en tiempo real por WebSocket.
 */

import { useState, useEffect, useCallback } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { obtenerJugadorActivo, guardarSesion, type DatosSesion } from "@/lib/sesion";
import { obtenerPerfil } from "@/api/auth";
import {
  leerNotificaciones,
  eliminarNotificacion,
  type Notificacion,
} from "@/lib/notificaciones";
import {
  buscarJugadores,
  enviarSolicitudAmistad,
  aceptarSolicitudAmistad,
  rechazarSolicitudAmistad,
  type InfoJugadorBusqueda,
} from "@/api/social";
import * as WS from "@/api/ws";

// ─── Constantes ───────────────────────────────────────────────────────────────

const TIPOS_PARTIDA = [
  {
    id: "publica",
    nombre: "Partida Pública",
    descripcion: "Lucha contra un usuario en línea.",
    imagen: "/pPublica.png",
  },
  {
    id: "entrenamiento",
    nombre: "Partida Entrenamiento",
    descripcion: "Lucha contra el robot Iron.",
    imagen: "/pEntrenamiento.png",
  },
  {
    id: "privada",
    nombre: "Partida Privada",
    descripcion: "Lucha contra tus amigos.",
    imagen: "/pPrivada.png",
  },
];

const MENU_LATERAL = [
  { id: "cartas", label: "Mis cartas" },
  { id: "tableros", label: "Mis tableros" },
  { id: "amigos", label: "Mis amigos" },
  { id: "tienda", label: "Tienda" },
  { id: "notificaciones", label: "Notificaciones" },
];

const NIVELES_DIFICULTAD = [
  {
    id: "facil",
    nombre: "Principiante",
    descripcion: "IA básica. Ideal para aprender los movimientos.",
    color: "bg-[#e8e8e8] text-[#1a2d4a]",
    textColor: "text-[#1a2d4a]",
    descColor: "text-[#1a2d4a]/70",
    hover: "hover:bg-white hover:scale-[1.02]",
  },
  {
    id: "medio",
    nombre: "Guerrero",
    descripcion: "IA equilibrada. Un desafío moderado.",
    color: "bg-[#1a2d4a] text-white border-2 border-white/20",
    textColor: "text-white",
    descColor: "text-white/60",
    hover: "hover:bg-[#203a60] hover:border-white/40 hover:scale-[1.02]",
  },
  {
    id: "dificil",
    nombre: "Maestro",
    descripcion: "IA experta con visión estratégica profunda.",
    color: "bg-red-900/30 text-red-100 border-2 border-red-500/40",
    textColor: "text-red-100",
    descColor: "text-red-200/70",
    hover: "hover:bg-red-900/50 hover:border-red-400 hover:scale-[1.02] shadow-[0_0_15px_rgba(239,68,68,0.2)]",
  },
];

// ─── Componente principal ─────────────────────────────────────────────────────

export default function PartidasPage() {
  const router = useRouter();
  const [jugador, setJugador] = useState<DatosSesion>(obtenerJugadorActivo);
  const [mostrarModalDificultad, setMostrarModalDificultad] = useState(false);

  // Panel lateral activo (null = pantalla principal con las tarjetas de partida)
  const [panelActivo, setPanelActivo] = useState<string | null>(null);

  // ── Notificaciones ──────────────────────────────────────────────────────────
  const [notificaciones, setNotificaciones] = useState<Notificacion[]>([]);

  // ── Amigos ──────────────────────────────────────────────────────────────────
  // La lista de amigos existentes no se puede pedir al servidor todavía (falta
  // OBTENER_AMIGOS). Se actualiza durante la sesión cuando se acepta una solicitud.
  const [amigos, setAmigos] = useState<string[]>([]);
  const [tabAmigos, setTabAmigos] = useState<"lista" | "buscar">("lista");

  // ── Búsqueda de jugadores ───────────────────────────────────────────────────
  const [textoBusqueda, setTextoBusqueda] = useState("");
  const [resultados, setResultados] = useState<InfoJugadorBusqueda[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [solicitudesEnviadas, setSolicitudesEnviadas] = useState<Set<string>>(
    new Set()
  );

  // ── Efectos ─────────────────────────────────────────────────────────────────

  /** Cargar notificaciones del sessionStorage y suscribirse a nuevas por WS. */
  useEffect(() => {
    setNotificaciones(leerNotificaciones());

    // Nuevas notificaciones en tiempo real
    const unsubSolicitud = WS.suscribir("SOLICITUD_AMISTAD", (msg) => {
      const nueva: Notificacion = {
        idNotificacion: msg.idNotificacion as number,
        tipo: "SOLICITUD_AMISTAD",
        remitente: msg.remitente as string,
        fecha_ini: msg.fecha_ini as string | undefined,
        fecha_fin: msg.fecha_fin as string | undefined,
      };
      setNotificaciones((prev) =>
        prev.find((n) => n.idNotificacion === nueva.idNotificacion)
          ? prev
          : [...prev, nueva]
      );
    });

    // Cuando se acepta una amistad, añadirla a la lista local
    const unsubAmistad = WS.suscribir("AMISTAD_ACEPTADA", (msg) => {
      const amigo = msg.amigo as string;
      setAmigos((prev) => (prev.includes(amigo) ? prev : [...prev, amigo]));
    });

    return () => {
      unsubSolicitud();
      unsubAmistad();
    };
  }, []);

  /** Refrescar puntos/cores desde el servidor al entrar a la pantalla. */
  useEffect(() => {
    const sesion = obtenerJugadorActivo();
    if (!sesion.nombre) return;
    obtenerPerfil(sesion.nombre)
      .then((datos) => {
        guardarSesion(datos);
        setJugador(datos);
      })
      .catch(() => {});
  }, []);

  /** Debounce: enviar búsqueda al servidor 400 ms después de que el usuario deje de escribir. */
  useEffect(() => {
    const texto = textoBusqueda.trim();
    if (texto.length < 2) {
      setResultados([]);
      return;
    }
    const timer = setTimeout(async () => {
      setBuscando(true);
      try {
        const res = await buscarJugadores(texto);
        setResultados(res);
      } finally {
        setBuscando(false);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [textoBusqueda]);

  // ── Handlers ────────────────────────────────────────────────────────────────

  const handleMenuClick = (id: string) => {
    setPanelActivo((prev) => (prev === id ? null : id));
  };

  const handleIniciarPartida = (id: string) => {
    if (id === "publica") router.push("/buscar");
    else if (id === "entrenamiento") setMostrarModalDificultad(true);
    // TODO: id === "privada" → INVITACION_PARTIDA
  };

  const handleSeleccionarDificultad = (dificultad: string) => {
    setMostrarModalDificultad(false);
    router.push(`/partida?modo=entrenamiento&dificultad=${dificultad}`);
  };

  const handleAceptarSolicitud = useCallback(
    (notif: Notificacion) => {
      aceptarSolicitudAmistad(notif.remitente, jugador.nombre, notif.idNotificacion);
      eliminarNotificacion(notif.idNotificacion);
      setNotificaciones((prev) =>
        prev.filter((n) => n.idNotificacion !== notif.idNotificacion)
      );
    },
    [jugador.nombre]
  );

  const handleRechazarSolicitud = useCallback((notif: Notificacion) => {
    rechazarSolicitudAmistad(notif.idNotificacion);
    eliminarNotificacion(notif.idNotificacion);
    setNotificaciones((prev) =>
      prev.filter((n) => n.idNotificacion !== notif.idNotificacion)
    );
  }, []);

  const handleEnviarSolicitud = useCallback(
    (destinatario: string) => {
      enviarSolicitudAmistad(jugador.nombre, destinatario);
      setSolicitudesEnviadas((prev) => new Set(prev).add(destinatario));
    },
    [jugador.nombre]
  );

  // ── Render ───────────────────────────────────────────────────────────────────

  const notifPendientes = notificaciones.filter(
    (n) => n.tipo === "SOLICITUD_AMISTAD"
  ).length;

  return (
    <div className="min-h-screen flex flex-col">
      {/* ─── Header ─────────────────────────────────────────────────────── */}
      <header className="bg-[#1a2d4a] px-6 py-3 flex items-center justify-between shrink-0">
        <Link href="/" className="flex items-center">
          <Image
            src="/nombre.png"
            alt="Onitama"
            width={130}
            height={36}
            priority
            className="h-9 w-auto object-contain"
          />
        </Link>

        <div className="flex items-center gap-5">
          <div className="w-11 h-11 rounded-full bg-[#2a4a6a] border-2 border-white/30 flex items-center justify-center overflow-hidden">
            <span className="text-white/50 text-xs select-none">
              {jugador.nombre.charAt(0).toUpperCase()}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Image src="/katanas.png" alt="Puntos" width={22} height={22} className="h-5 w-auto" />
            <span className="text-white font-semibold text-sm">
              {jugador.puntos.toLocaleString()}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Image src="/core.png" alt="Cores" width={22} height={22} className="h-5 w-auto" />
            <span className="text-white font-semibold text-sm">
              {jugador.cores.toLocaleString()}
            </span>
          </div>
        </div>
      </header>

      {/* ─── Cuerpo ──────────────────────────────────────────────────────── */}
      <div className="flex flex-1 overflow-hidden">

        {/* ─── Sidebar ─────────────────────────────────────────────────── */}
        <aside className="w-60 bg-[#7b8fa8] flex flex-col shrink-0">
          <button
            type="button"
            onClick={() => setPanelActivo(null)}
            className="px-6 pt-6 pb-3 text-left w-full hover:bg-white/10 transition-colors"
          >
            <span className="text-white font-bold text-lg tracking-wide uppercase">
              ¡A jugar!
            </span>
          </button>

          <nav className="flex flex-col mt-2">
            {MENU_LATERAL.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => handleMenuClick(item.id)}
                className={`flex items-center text-sm font-semibold tracking-widest uppercase px-6 py-4 text-left transition-colors border-b border-white/10 last:border-0
                  ${panelActivo === item.id
                    ? "bg-white/20 text-white"
                    : "text-white/90 hover:bg-white/10"
                  }`}
              >
                <span className="flex-1">{item.label}</span>

                {/* Badge de notificaciones */}
                {item.id === "notificaciones" && notifPendientes > 0 && (
                  <span className="ml-2 bg-red-500 text-white text-xs font-bold rounded-full min-w-[20px] h-5 flex items-center justify-center px-1.5">
                    {notifPendientes}
                  </span>
                )}
              </button>
            ))}
          </nav>
        </aside>

        {/* ─── Área principal ──────────────────────────────────────────── */}
        <main className="flex-1 bg-stone-100 overflow-y-auto">

          {/* Pantalla principal: tarjetas de partida */}
          {!panelActivo && (
            <div className="flex items-center justify-center min-h-full px-8 py-10">
              <div className="flex flex-wrap gap-10 items-center justify-center">
                {TIPOS_PARTIDA.map((tipo) => (
                  <div key={tipo.id} className="flex flex-col items-center gap-4">
                    <button
                      type="button"
                      className="group"
                      aria-label={tipo.nombre}
                      onClick={() => handleIniciarPartida(tipo.id)}
                    >
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={tipo.imagen}
                        alt={tipo.nombre}
                        className="w-44 h-44 rounded-full object-cover shadow-lg group-hover:scale-105 group-hover:shadow-xl transition-all duration-200"
                      />
                    </button>
                    <div className="bg-stone-300 rounded-lg px-4 py-3 text-center w-52 shadow-sm">
                      <p className="text-xs font-bold text-stone-700 uppercase tracking-wide">
                        {tipo.nombre}
                      </p>
                      <p className="text-xs text-stone-600 mt-1">{tipo.descripcion}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Panel: Mis amigos */}
          {panelActivo === "amigos" && (
            <PanelAmigos
              jugador={jugador}
              amigos={amigos}
              tabActiva={tabAmigos}
              onCambiarTab={setTabAmigos}
              textoBusqueda={textoBusqueda}
              onCambiarBusqueda={setTextoBusqueda}
              resultados={resultados}
              buscando={buscando}
              solicitudesEnviadas={solicitudesEnviadas}
              onEnviarSolicitud={handleEnviarSolicitud}
            />
          )}

          {/* Panel: Notificaciones */}
          {panelActivo === "notificaciones" && (
            <PanelNotificaciones
              notificaciones={notificaciones}
              onAceptar={handleAceptarSolicitud}
              onRechazar={handleRechazarSolicitud}
            />
          )}

          {/* Paneles pendientes de implementar */}
          {(panelActivo === "cartas" ||
            panelActivo === "tableros" ||
            panelActivo === "tienda") && (
            <div className="flex items-center justify-center min-h-full">
              <div className="text-center text-stone-400">
                <p className="text-5xl mb-4">🚧</p>
                <p className="text-xl font-semibold uppercase tracking-widest">
                  Próximamente
                </p>
                <p className="text-sm mt-2">Esta sección está en desarrollo.</p>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* ─── Modal: selección de dificultad ──────────────────────────── */}
      {mostrarModalDificultad && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm bg-[#1a2d4a]/95 backdrop-blur-md border border-white/10 rounded-2xl shadow-2xl p-8 flex flex-col items-center gap-6">
            <div className="flex flex-col items-center gap-3 w-full border-b border-white/10 pb-6">
              <div className="w-16 h-16 rounded-full bg-stone-300 flex items-center justify-center shadow-[0_0_15px_rgba(255,255,255,0.2)]">
                <Image
                  src="/pEntrenamiento.png"
                  alt="Iron Bot"
                  width={64}
                  height={64}
                  className="rounded-full object-cover"
                />
              </div>
              <h2 className="text-2xl font-bold text-white uppercase tracking-widest text-center mt-2">
                Entrenamiento
              </h2>
              <p className="text-white/60 text-sm text-center">
                Elige la dificultad del bot Iron
              </p>
            </div>

            <div className="flex flex-col gap-4 w-full pt-2">
              {NIVELES_DIFICULTAD.map((nivel) => (
                <button
                  key={nivel.id}
                  type="button"
                  onClick={() => handleSeleccionarDificultad(nivel.id)}
                  className={`w-full flex flex-col items-center justify-center py-4 rounded-xl transition-all duration-200 cursor-pointer active:scale-100 ${nivel.color} ${nivel.hover}`}
                >
                  <span className={`font-bold text-lg uppercase tracking-widest ${nivel.textColor}`}>
                    {nivel.nombre}
                  </span>
                  <span className={`text-xs mt-1 ${nivel.descColor}`}>
                    {nivel.descripcion}
                  </span>
                </button>
              ))}
            </div>

            <button
              type="button"
              onClick={() => setMostrarModalDificultad(false)}
              className="text-white/50 text-sm hover:text-white transition-colors mt-2"
            >
              ← Cancelar y volver
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Subcomponentes ───────────────────────────────────────────────────────────

interface PanelAmigosProps {
  jugador: DatosSesion;
  amigos: string[];
  tabActiva: "lista" | "buscar";
  onCambiarTab: (tab: "lista" | "buscar") => void;
  textoBusqueda: string;
  onCambiarBusqueda: (texto: string) => void;
  resultados: InfoJugadorBusqueda[];
  buscando: boolean;
  solicitudesEnviadas: Set<string>;
  onEnviarSolicitud: (destinatario: string) => void;
}

function PanelAmigos({
  jugador,
  amigos,
  tabActiva,
  onCambiarTab,
  textoBusqueda,
  onCambiarBusqueda,
  resultados,
  buscando,
  solicitudesEnviadas,
  onEnviarSolicitud,
}: PanelAmigosProps) {
  return (
    <div className="max-w-xl mx-auto px-6 py-8">
      <h2 className="text-xl font-bold text-stone-800 uppercase tracking-widest mb-6">
        Mis amigos
      </h2>

      {/* Pestañas */}
      <div className="flex border-b border-stone-300 mb-6">
        {(["lista", "buscar"] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => onCambiarTab(tab)}
            className={`px-6 py-3 text-sm font-semibold uppercase tracking-wider transition-colors
              ${tabActiva === tab
                ? "border-b-2 border-[#1a2d4a] text-[#1a2d4a]"
                : "text-stone-500 hover:text-stone-700"
              }`}
          >
            {tab === "lista" ? "Mis amigos" : "Buscar"}
          </button>
        ))}
      </div>

      {/* Pestaña: lista de amigos */}
      {tabActiva === "lista" && (
        <div>
          {amigos.length === 0 ? (
            <div className="text-center py-16 text-stone-400">
              <p className="text-4xl mb-3">👥</p>
              <p className="font-semibold">Aún no tienes amigos añadidos.</p>
              <p className="text-sm mt-1">
                Usa la pestaña &ldquo;Buscar&rdquo; para encontrar jugadores.
              </p>
              <p className="text-xs mt-3 text-stone-300">
                La lista de amigos existentes se cargará cuando el servidor
                añada soporte para OBTENER_AMIGOS.
              </p>
            </div>
          ) : (
            <ul className="space-y-2">
              {amigos.map((nombre) => (
                <li
                  key={nombre}
                  className="flex items-center gap-3 bg-white rounded-xl px-4 py-3 shadow-sm"
                >
                  <div className="w-9 h-9 rounded-full bg-[#1a2d4a] flex items-center justify-center text-white font-bold text-sm">
                    {nombre.charAt(0).toUpperCase()}
                  </div>
                  <span className="font-medium text-stone-800">{nombre}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Pestaña: buscar jugadores */}
      {tabActiva === "buscar" && (
        <div>
          <input
            type="text"
            value={textoBusqueda}
            onChange={(e) => onCambiarBusqueda(e.target.value)}
            placeholder="Escribe un nombre de usuario…"
            className="w-full px-4 py-3 rounded-xl border border-stone-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent mb-4"
          />

          {buscando && (
            <p className="text-sm text-stone-400 text-center py-4">Buscando…</p>
          )}

          {!buscando && textoBusqueda.trim().length >= 2 && resultados.length === 0 && (
            <p className="text-sm text-stone-400 text-center py-4">
              No se encontraron jugadores.
            </p>
          )}

          {!buscando && resultados.length > 0 && (
            <ul className="space-y-2">
              {resultados.map((j) => {
                    const esMismoUsuario = j.nombre === jugador.nombre;
                    const esAmigo = amigos.includes(j.nombre);
                    const yaEnviado = solicitudesEnviadas.has(j.nombre);

                    return (
                      <li
                        key={j.nombre}
                        className="flex items-center gap-3 bg-white rounded-xl px-4 py-3 shadow-sm"
                      >
                        <div className="w-9 h-9 rounded-full bg-[#7b8fa8] flex items-center justify-center text-white font-bold text-sm">
                          {j.nombre.charAt(0).toUpperCase()}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-stone-800 truncate">{j.nombre}</p>
                          <p className="text-xs text-stone-400">{j.puntos} puntos</p>
                        </div>
                        {!esMismoUsuario && (
                          esAmigo ? (
                            <span className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-green-100 text-green-700">
                              Ya sois amigos
                            </span>
                          ) : (
                            <button
                              type="button"
                              onClick={() => onEnviarSolicitud(j.nombre)}
                              disabled={yaEnviado}
                              className={`text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors
                                ${yaEnviado
                                  ? "bg-stone-200 text-stone-400 cursor-default"
                                  : "bg-[#1a2d4a] text-white hover:bg-[#203a60]"
                                }`}
                            >
                              {yaEnviado ? "Enviada ✓" : "Solicitar"}
                            </button>
                          )
                        )}
                      </li>
                    );
              })}
            </ul>
          )}

          {textoBusqueda.trim().length > 0 && textoBusqueda.trim().length < 2 && (
            <p className="text-xs text-stone-400 text-center">
              Escribe al menos 2 caracteres para buscar.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────

interface PanelNotificacionesProps {
  notificaciones: Notificacion[];
  onAceptar: (notif: Notificacion) => void;
  onRechazar: (notif: Notificacion) => void;
}

function PanelNotificaciones({
  notificaciones,
  onAceptar,
  onRechazar,
}: PanelNotificacionesProps) {
  const solicitudes = notificaciones.filter((n) => n.tipo === "SOLICITUD_AMISTAD");

  return (
    <div className="max-w-xl mx-auto px-6 py-8">
      <h2 className="text-xl font-bold text-stone-800 uppercase tracking-widest mb-6">
        Notificaciones
      </h2>

      {solicitudes.length === 0 ? (
        <div className="text-center py-16 text-stone-400">
          <p className="text-4xl mb-3">🔔</p>
          <p className="font-semibold">Sin notificaciones pendientes.</p>
          <p className="text-sm mt-1">Aquí aparecerán las solicitudes de amistad.</p>
        </div>
      ) : (
        <ul className="space-y-3">
          {solicitudes.map((notif) => (
            <li
              key={notif.idNotificacion}
              className="bg-white rounded-xl px-4 py-4 shadow-sm flex items-center gap-3"
            >
              {/* Avatar */}
              <div className="w-10 h-10 rounded-full bg-[#1a2d4a] flex items-center justify-center text-white font-bold text-sm shrink-0">
                {notif.remitente.charAt(0).toUpperCase()}
              </div>

              {/* Texto */}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-stone-800 truncate">
                  {notif.remitente}
                </p>
                <p className="text-xs text-stone-400">
                  Te ha enviado una solicitud de amistad
                </p>
              </div>

              {/* Acciones */}
              <div className="flex gap-2 shrink-0">
                <button
                  type="button"
                  onClick={() => onAceptar(notif)}
                  className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-[#1a2d4a] text-white hover:bg-[#203a60] transition-colors"
                >
                  Aceptar
                </button>
                <button
                  type="button"
                  onClick={() => onRechazar(notif)}
                  className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-stone-200 text-stone-600 hover:bg-stone-300 transition-colors"
                >
                  Rechazar
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
