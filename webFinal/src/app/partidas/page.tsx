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
import { useRouter } from "next/navigation";
import { obtenerJugadorActivo, guardarSesion, cerrarSesion, type DatosSesion } from "@/lib/sesion";
import { obtenerPerfil } from "@/api/auth";
import { activarSkin, comprarSkin, obtenerTiendaSkins, type SkinEstado } from "@/api/skins";
import {
  leerNotificaciones,
  eliminarNotificacion,
  limpiarNotificaciones,
  type Notificacion,
} from "@/lib/notificaciones";
import {
  buscarJugadores,
  obtenerAmigos,
  borrarAmigo,
  obtenerPartidasConAmigo,
  obtenerPartidasPublicas,
  enviarInvitacionPartidaPrivada,
  aceptarInvitacionPartidaPrivada,
  rechazarInvitacionPartidaPrivada,
  enviarSolicitudAmistad,
  aceptarSolicitudAmistad,
  rechazarSolicitudAmistad,
  type InfoJugadorBusqueda,
  type InfoAmigo,
  type ResumenPartidaAmigo,
  type ResumenPartidaPublica,
} from "@/api/social";
import * as WS from "@/api/ws";
import { getSkinNombre, getPiezaSrc, getSkinPrecio, normalizarSkinId, type SkinId } from "@/lib/skins";

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
  { id: "cuenta", label: "Mi cuenta" },
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
  const [mostrarModalPartidaPrivada, setMostrarModalPartidaPrivada] = useState(false);
  const [tabPartidaPrivada, setTabPartidaPrivada] = useState<"crear" | "reanudar">("crear");
  const [invitacionPrivadaEnCurso, setInvitacionPrivadaEnCurso] = useState<{
    destinatario: string;
  } | null>(null);
  const [mensajePrivada, setMensajePrivada] = useState<string | null>(null);
  const [tiempoEsperaPrivada, setTiempoEsperaPrivada] = useState(120);

  // Panel lateral activo (null = pantalla principal con las tarjetas de partida)
  const [panelActivo, setPanelActivo] = useState<string | null>(null);

  // ── Notificaciones ──────────────────────────────────────────────────────────
  const [notificaciones, setNotificaciones] = useState<Notificacion[]>([]);

  // ── Amigos ──────────────────────────────────────────────────────────────────
  // Lista real de amigos desde backend (OBTENER_AMIGOS)
  const [amigos, setAmigos] = useState<InfoAmigo[]>([]);
  const [tabAmigos, setTabAmigos] = useState<"lista" | "buscar">("lista");
  const [amigoSeleccionado, setAmigoSeleccionado] = useState<InfoAmigo | null>(null);
  const [partidasConAmigo, setPartidasConAmigo] = useState<ResumenPartidaAmigo[]>([]);
  const [cargandoPartidasAmigo, setCargandoPartidasAmigo] = useState(false);
  const [mostrarModalPartidasAmigo, setMostrarModalPartidasAmigo] = useState(false);

  // ── Búsqueda de jugadores ───────────────────────────────────────────────────
  const [textoBusqueda, setTextoBusqueda] = useState("");
  const [resultados, setResultados] = useState<InfoJugadorBusqueda[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [solicitudesEnviadas, setSolicitudesEnviadas] = useState<Set<string>>(
    new Set()
  );

  const [mostrarModalCerrarSesion, setMostrarModalCerrarSesion] = useState(false);
  const [partidasPublicas, setPartidasPublicas] = useState<ResumenPartidaPublica[]>([]);
  const [cargandoPartidasPublicas, setCargandoPartidasPublicas] = useState(false);
  const [skins, setSkins] = useState<SkinEstado[]>([]);
  const [cargandoSkins, setCargandoSkins] = useState(false);
  const [accionSkinEnCurso, setAccionSkinEnCurso] = useState<string | null>(null);
  const [confirmacionSkin, setConfirmacionSkin] = useState<{ tipo: "comprar" | "usar"; skinId: SkinId } | null>(null);

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

    const unsubInvitacionPartida = WS.suscribir("INVITACION_PARTIDA", (msg) => {
      const nueva: Notificacion = {
        idNotificacion: msg.idNotificacion as number,
        tipo: "INVITACION_PARTIDA",
        remitente: msg.remitente as string,
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
      setAmigos((prev) =>
        prev.some((a) => a.nombre === amigo) ? prev : [...prev, { nombre: amigo, puntos: 0 }]
      );
    });

    return () => {
      unsubSolicitud();
      unsubInvitacionPartida();
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

  /** Panel Mi cuenta: refrescar perfil e historial de partidas públicas. */
  useEffect(() => {
    if (panelActivo !== "cuenta" || !jugador.nombre) return;
    setCargandoPartidasPublicas(true);
    obtenerPerfil(jugador.nombre)
      .then((datos) => {
        guardarSesion(datos);
        setJugador(datos);
      })
      .catch(() => {});
    obtenerPartidasPublicas(jugador.nombre)
      .then((lista) => setPartidasPublicas(lista))
      .catch(() => setPartidasPublicas([]))
      .finally(() => setCargandoPartidasPublicas(false));
  }, [panelActivo, jugador.nombre]);

  /** Paneles de skins: tienda y mis tableros comparten la misma carga. */
  useEffect(() => {
    if ((panelActivo !== "tableros" && panelActivo !== "tienda") || !jugador.nombre) return;
    setCargandoSkins(true);
    obtenerTiendaSkins(jugador.nombre)
      .then((res) => {
        setSkins(res.skins);
        const skinActiva = normalizarSkinId(res.skin_activa);
        setJugador((prev) => {
          const siguiente: DatosSesion = { ...prev, cores: res.cores, skin_activa: skinActiva };
          guardarSesion(siguiente);
          return siguiente;
        });
      })
      .catch(() => {})
      .finally(() => setCargandoSkins(false));
  }, [panelActivo, jugador.nombre]);

  /** Cargar amigos desde backend al abrir el panel de amigos. */
  useEffect(() => {
    if (panelActivo !== "amigos" || !jugador.nombre) return;
    obtenerAmigos(jugador.nombre)
      .then((lista) => setAmigos(lista))
      .catch(() => setAmigos([]));
  }, [panelActivo, jugador.nombre]);

  /** Cargar amigos también al abrir el popup de partida privada (pestaña crear). */
  useEffect(() => {
    if (!mostrarModalPartidaPrivada || tabPartidaPrivada !== "crear" || !jugador.nombre) return;
    obtenerAmigos(jugador.nombre)
      .then((lista) => setAmigos(lista))
      .catch(() => setAmigos([]));
  }, [mostrarModalPartidaPrivada, tabPartidaPrivada, jugador.nombre]);

  /** Mensajes WS para flujo de invitación a partida privada. */
  useEffect(() => {
    const unsubEncontrada = WS.suscribir("PARTIDA_PRIVADA_ENCONTRADA", (msg) => {
      sessionStorage.setItem("datosPartida", JSON.stringify(msg));
      setInvitacionPrivadaEnCurso(null);
      setMostrarModalPartidaPrivada(false);
      router.push("/presentacion-partida");
    });

    const unsubRechazada = WS.suscribir("INVITACION_RECHAZADA", () => {
      setInvitacionPrivadaEnCurso(null);
      setMostrarModalPartidaPrivada(false);
      setMensajePrivada("Tu amigo rechazó la solicitud de partida privada.");
    });

    const unsubTimeout = WS.suscribir("ERROR_NO_UNIDO", () => {
      setInvitacionPrivadaEnCurso(null);
      setMostrarModalPartidaPrivada(false);
      setMensajePrivada("Tu amigo no aceptó la solicitud de partida en el tiempo límite.");
    });

    const unsubDesconectado = WS.suscribir("ERROR_DESCONECTADO", () => {
      setInvitacionPrivadaEnCurso(null);
      setMensajePrivada("Tu amigo no está conectado en este momento.");
    });

    return () => {
      unsubEncontrada();
      unsubRechazada();
      unsubTimeout();
      unsubDesconectado();
    };
  }, [router]);

  /** Countdown visual de espera de invitación (2 minutos). */
  useEffect(() => {
    if (!invitacionPrivadaEnCurso) return;
    setTiempoEsperaPrivada(120);
    const interval = setInterval(() => {
      setTiempoEsperaPrivada((t) => {
        if (t <= 1) {
          clearInterval(interval);
          return 0;
        }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [invitacionPrivadaEnCurso]);

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
    else if (id === "privada") {
      setTabPartidaPrivada("crear");
      setMostrarModalPartidaPrivada(true);
    }
  };

  const handleSeleccionarDificultad = (dificultad: string) => {
    setMostrarModalDificultad(false);
    // Evitar que datosPartida de una partida online/privada anterior active modo servidor en entrenamiento
    sessionStorage.removeItem("datosPartida");
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

  const handleAceptarInvitacionPartida = useCallback((notif: Notificacion) => {
    aceptarInvitacionPartidaPrivada(notif.idNotificacion);
    eliminarNotificacion(notif.idNotificacion);
    setNotificaciones((prev) =>
      prev.filter((n) => n.idNotificacion !== notif.idNotificacion)
    );
  }, []);

  const handleRechazarInvitacionPartida = useCallback((notif: Notificacion) => {
    rechazarInvitacionPartidaPrivada(notif.idNotificacion);
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

  const handleSeleccionarAmigo = useCallback(
    async (amigo: InfoAmigo) => {
      setAmigoSeleccionado(amigo);
      setMostrarModalPartidasAmigo(true);
      setCargandoPartidasAmigo(true);
      try {
        const partidas = await obtenerPartidasConAmigo(jugador.nombre, amigo.nombre);
        setPartidasConAmigo(partidas);
      } finally {
        setCargandoPartidasAmigo(false);
      }
    },
    [jugador.nombre]
  );

  const handleBorrarAmigo = useCallback(
    async (amigo: InfoAmigo) => {
      const confirmar = window.confirm(`¿Seguro que quieres borrar a ${amigo.nombre} de tus amigos?`);
      if (!confirmar) return;
      const ok = await borrarAmigo(jugador.nombre, amigo.nombre);
      if (!ok) return;
      setAmigos((prev) => prev.filter((a) => a.nombre !== amigo.nombre));
      if (amigoSeleccionado?.nombre === amigo.nombre) {
        setAmigoSeleccionado(null);
        setPartidasConAmigo([]);
        setMostrarModalPartidasAmigo(false);
      }
    },
    [jugador.nombre, amigoSeleccionado]
  );

  const handleConfirmarCerrarSesion = useCallback(() => {
    setMostrarModalCerrarSesion(false);
    cerrarSesion();
    limpiarNotificaciones();
    sessionStorage.removeItem("datosPartida");
    WS.desconectar();
    router.push("/");
  }, [router]);

  const handleInvitarPartidaPrivada = useCallback(
    (amigo: InfoAmigo) => {
      const ok = enviarInvitacionPartidaPrivada(jugador.nombre, amigo.nombre);
      if (!ok) {
        setMensajePrivada("No se pudo enviar la invitación. Revisa la conexión.");
        return;
      }
      setInvitacionPrivadaEnCurso({ destinatario: amigo.nombre });
      setMostrarModalPartidaPrivada(false);
    },
    [jugador.nombre]
  );

  const confirmarComprarSkin = useCallback(async (skinId: SkinId) => {
    setAccionSkinEnCurso(`comprar-${skinId}`);
    const res = await comprarSkin(jugador.nombre, skinId);
    setAccionSkinEnCurso(null);
    if (!res.ok) {
      if (res.codigo === "CORES_INSUFICIENTES") setMensajePrivada("No tienes suficientes cores para comprar esta skin.");
      else if (res.codigo === "YA_COMPRADA") setMensajePrivada("Esta skin ya está comprada.");
      else setMensajePrivada("No se pudo completar la compra de la skin.");
      return;
    }
    setJugador((prev) => {
      const siguiente = { ...prev, cores: res.cores };
      guardarSesion(siguiente);
      return siguiente;
    });
    setSkins((prev) => prev.map((s) => (s.skin_id === skinId ? { ...s, owned: true } : s)));
    setMensajePrivada("Skin comprada correctamente.");
  }, [jugador.nombre]);

  const confirmarUsarSkin = useCallback(async (skinId: SkinId) => {
    setAccionSkinEnCurso(`usar-${skinId}`);
    const res = await activarSkin(jugador.nombre, skinId);
    setAccionSkinEnCurso(null);
    if (!res.ok) {
      setMensajePrivada("No se pudo activar esta skin.");
      return;
    }
    setJugador((prev) => {
      const siguiente = { ...prev, skin_activa: res.skin_activa };
      guardarSesion(siguiente);
      return siguiente;
    });
    setSkins((prev) => prev.map((s) => ({ ...s, es_activa: s.skin_id === res.skin_activa })));
  }, [jugador.nombre]);

  const handleComprarSkin = useCallback((skinId: SkinId) => {
    setConfirmacionSkin({ tipo: "comprar", skinId });
  }, []);

  const handleUsarSkin = useCallback((skinId: SkinId) => {
    setConfirmacionSkin({ tipo: "usar", skinId });
  }, []);

  const handleConfirmarAccionSkin = useCallback(async () => {
    if (!confirmacionSkin) return;
    const actual = confirmacionSkin;
    setConfirmacionSkin(null);
    if (actual.tipo === "comprar") {
      await confirmarComprarSkin(actual.skinId);
      return;
    }
    await confirmarUsarSkin(actual.skinId);
  }, [confirmacionSkin, confirmarComprarSkin, confirmarUsarSkin]);

  // ── Render ───────────────────────────────────────────────────────────────────

  const notifPendientes = notificaciones.length;

  return (
    <div className="min-h-screen flex flex-col">
      {/* ─── Header ─────────────────────────────────────────────────────── */}
      <header className="bg-[#1a2d4a] px-6 py-3 flex items-center justify-between shrink-0 gap-4">
        <div className="flex items-center" aria-label="Onitama">
          <Image
            src="/nombre.png"
            alt="Onitama"
            width={130}
            height={36}
            priority
            className="h-9 w-auto object-contain"
          />
        </div>

        <div className="flex items-center gap-4 sm:gap-6 min-w-0 flex-1 justify-end">
          <button
            type="button"
            onClick={() => handleMenuClick("cuenta")}
            className={`w-11 h-11 shrink-0 rounded-full border-2 flex items-center justify-center overflow-hidden transition-colors ${
              panelActivo === "cuenta"
                ? "bg-white/20 border-white/60"
                : "bg-[#2a4a6a] border-white/30 hover:bg-white/10"
            }`}
            title="Mi cuenta"
            aria-label="Abrir Mi cuenta"
          >
            <span className="text-white/90 text-sm font-semibold select-none">
              {jugador.nombre.charAt(0).toUpperCase()}
            </span>
          </button>
          <div className="flex items-center gap-2">
            <Image src="/katanas.png" alt="Katanas" width={22} height={22} className="h-5 w-auto shrink-0" />
            <span className="text-white font-semibold text-sm tabular-nums">
              {jugador.puntos.toLocaleString()}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Image src="/core.png" alt="Cores" width={22} height={22} className="h-5 w-auto shrink-0" />
            <span className="text-white font-semibold text-sm tabular-nums">
              {jugador.cores.toLocaleString()}
            </span>
          </div>
          <div className="hidden sm:block h-8 w-px bg-white/20 shrink-0" aria-hidden />
          <button
            type="button"
            onClick={() => setMostrarModalCerrarSesion(true)}
            className="shrink-0 p-2 rounded-lg text-white/80 hover:text-white hover:bg-white/10 transition-colors"
            title="Salir"
            aria-label="Cerrar sesión"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-6 h-6">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
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
          {mensajePrivada && (
            <div className="mx-6 mt-4 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-amber-800 text-sm flex items-center justify-between">
              <span>{mensajePrivada}</span>
              <button
                type="button"
                onClick={() => setMensajePrivada(null)}
                className="text-amber-700 hover:text-amber-900 font-bold"
                aria-label="Cerrar mensaje"
              >
                ×
              </button>
            </div>
          )}

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
              amigoSeleccionado={amigoSeleccionado}
              partidasConAmigo={partidasConAmigo}
              cargandoPartidasAmigo={cargandoPartidasAmigo}
              mostrarModalPartidasAmigo={mostrarModalPartidasAmigo}
              tabActiva={tabAmigos}
              onCambiarTab={setTabAmigos}
              textoBusqueda={textoBusqueda}
              onCambiarBusqueda={setTextoBusqueda}
              resultados={resultados}
              buscando={buscando}
              solicitudesEnviadas={solicitudesEnviadas}
              onEnviarSolicitud={handleEnviarSolicitud}
              onSeleccionarAmigo={handleSeleccionarAmigo}
              onBorrarAmigo={handleBorrarAmigo}
              onCerrarModalPartidas={() => setMostrarModalPartidasAmigo(false)}
            />
          )}

          {/* Panel: Notificaciones */}
          {panelActivo === "notificaciones" && (
            <PanelNotificaciones
              notificaciones={notificaciones}
              onAceptarAmistad={handleAceptarSolicitud}
              onRechazarAmistad={handleRechazarSolicitud}
              onAceptarInvitacionPartida={handleAceptarInvitacionPartida}
              onRechazarInvitacionPartida={handleRechazarInvitacionPartida}
            />
          )}

          {panelActivo === "cuenta" && (
            <PanelMiCuenta
              jugador={jugador}
              partidasPublicas={partidasPublicas}
              cargandoPartidasPublicas={cargandoPartidasPublicas}
            />
          )}

          {panelActivo === "cartas" && (
            <div className="flex items-center justify-center min-h-full">
              <div className="text-center text-stone-400">
                <p className="text-5xl mb-4">🚧</p>
                <p className="text-xl font-semibold uppercase tracking-widest">Próximamente</p>
                <p className="text-sm mt-2">Esta sección está en desarrollo.</p>
              </div>
            </div>
          )}

          {panelActivo === "tableros" && (
            <PanelMisTableros
              jugador={jugador}
              skins={skins}
              cargando={cargandoSkins}
              accionSkinEnCurso={accionSkinEnCurso}
              onUsarSkin={handleUsarSkin}
            />
          )}

          {panelActivo === "tienda" && (
            <PanelTiendaSkins
              jugador={jugador}
              skins={skins}
              cargando={cargandoSkins}
              accionSkinEnCurso={accionSkinEnCurso}
              onComprarSkin={handleComprarSkin}
            />
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

      {/* ─── Modal: Partida privada (doble pestaña) ───────────────────── */}
      {mostrarModalPartidaPrivada && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="w-full max-w-4xl bg-white rounded-2xl shadow-2xl p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-2xl font-bold text-stone-900 uppercase tracking-wide">Partida privada</h2>
              <button
                type="button"
                onClick={() => setMostrarModalPartidaPrivada(false)}
                className="text-3xl leading-none text-stone-500 hover:text-stone-800"
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>

            <div className="mt-5 flex border-b border-stone-300">
              <button
                type="button"
                onClick={() => setTabPartidaPrivada("crear")}
                className={`px-5 py-3 text-sm font-semibold uppercase tracking-wider ${
                  tabPartidaPrivada === "crear"
                    ? "border-b-2 border-[#1a2d4a] text-[#1a2d4a]"
                    : "text-stone-500 hover:text-stone-700"
                }`}
              >
                Crear nueva partida
              </button>
              <button
                type="button"
                onClick={() => setTabPartidaPrivada("reanudar")}
                className={`px-5 py-3 text-sm font-semibold uppercase tracking-wider ${
                  tabPartidaPrivada === "reanudar"
                    ? "border-b-2 border-[#1a2d4a] text-[#1a2d4a]"
                    : "text-stone-500 hover:text-stone-700"
                }`}
              >
                Reanudar partida
              </button>
            </div>

            {tabPartidaPrivada === "crear" && (
              <div className="mt-5">
                {amigos.length === 0 ? (
                  <p className="text-stone-500 text-sm">No tienes amigos disponibles para invitar.</p>
                ) : (
                  <ul className="space-y-2 max-h-[360px] overflow-y-auto pr-1">
                    {amigos.map((amigo) => (
                      <li key={amigo.nombre} className="flex items-center gap-3 bg-stone-50 rounded-xl px-4 py-3">
                        <div className="w-9 h-9 rounded-full bg-[#1a2d4a] text-white flex items-center justify-center font-bold text-sm">
                          {amigo.nombre.charAt(0).toUpperCase()}
                        </div>
                        <div className="flex-1">
                          <p className="font-semibold text-stone-800">{amigo.nombre}</p>
                          <p className="text-xs text-stone-500 flex items-center gap-1 mt-0.5">
                            <Image src="/katanas.png" alt="Katanas" width={12} height={12} className="h-3 w-auto" />
                            <span>{amigo.puntos}</span>
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleInvitarPartidaPrivada(amigo)}
                          className="text-xs font-semibold px-4 py-2 rounded-lg bg-[#1a2d4a] text-white hover:bg-[#203a60]"
                        >
                          Invitar
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {tabPartidaPrivada === "reanudar" && (
              <div className="mt-8 rounded-xl border border-dashed border-stone-300 bg-stone-50 p-8 text-center">
                <p className="text-stone-500 font-semibold">Pendiente de soporte backend</p>
                <p className="text-stone-400 text-sm mt-1">
                  Esta pestaña se activará cuando estén listos los mensajes de pausa y reanudar.
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ─── Pantalla de espera: invitación privada en curso ───────────── */}
      {mostrarModalCerrarSesion && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-8 flex flex-col items-center gap-5 shadow-2xl max-w-sm w-full mx-4">
            <span className="text-4xl" aria-hidden>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-12 h-12 text-white/70 mx-auto">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
            </span>
            <h2 className="text-xl font-bold text-white uppercase tracking-widest text-center">
              ¿Cerrar sesión?
            </h2>
            <p className="text-white/60 text-sm text-center">
              Se cerrará la conexión con el servidor y volverás a la página de inicio.
            </p>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={() => setMostrarModalCerrarSesion(false)}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm border border-white/20 text-white/70 hover:bg-white/10 transition-colors"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmarCerrarSesion}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-red-700 text-white hover:bg-red-600 transition-colors"
              >
                Salir
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmacionSkin && (
        <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
          <div className="bg-white border border-stone-200 rounded-2xl p-6 flex flex-col items-center gap-4 shadow-2xl max-w-sm w-full">
            <h3 className="text-lg font-bold text-stone-800 uppercase tracking-widest text-center">
              {confirmacionSkin.tipo === "comprar" ? "Confirmar compra" : "Confirmar selección"}
            </h3>
            <p className="text-sm text-stone-600 text-center">
              {confirmacionSkin.tipo === "comprar"
                ? `¿Quieres comprar la skin ${getSkinNombre(confirmacionSkin.skinId)}?`
                : `¿Quieres activar la skin ${getSkinNombre(confirmacionSkin.skinId)}?`}
            </p>
            {confirmacionSkin.tipo === "comprar" && (
              <div className="flex flex-col items-center gap-1.5 text-sm text-stone-700">
                <div className="flex items-center gap-2">
                  <span>Tus cores:</span>
                  <Image src="/core.png" alt="Cores" width={16} height={16} />
                  <span className="font-semibold">{jugador.cores.toLocaleString()}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span>Coste:</span>
                  <Image src="/core.png" alt="Cores" width={16} height={16} />
                  <span className="font-semibold">{getSkinPrecio(confirmacionSkin.skinId)}</span>
                </div>
              </div>
            )}
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={() => setConfirmacionSkin(null)}
                className="flex-1 py-2.5 rounded-xl font-semibold text-sm border border-stone-300 text-stone-600 hover:bg-stone-100"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmarAccionSkin}
                className="flex-1 py-2.5 rounded-xl font-semibold text-sm bg-[#1a2d4a] text-white hover:bg-[#203a60]"
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>
      )}

      {invitacionPrivadaEnCurso && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#0b1522]/90 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg bg-[#1a2d4a] rounded-2xl border border-white/10 p-8 text-white text-center shadow-2xl">
            <h3 className="text-2xl font-bold uppercase tracking-widest">Esperando respuesta</h3>
            <p className="text-white/70 mt-3">
              Has invitado a <span className="font-semibold">@{invitacionPrivadaEnCurso.destinatario}</span> a una partida privada.
            </p>
            <p className="text-white/60 text-sm mt-2">
              Tienes 2 minutos para que acepte la solicitud.
            </p>
            <p className="font-mono text-4xl mt-6 text-yellow-300">
              {String(Math.floor(tiempoEsperaPrivada / 60)).padStart(2, "0")}:
              {String(tiempoEsperaPrivada % 60).padStart(2, "0")}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Subcomponentes ───────────────────────────────────────────────────────────

function formatearDuracionPartida(segundos: number): string {
  if (!Number.isFinite(segundos) || segundos <= 0) return "";
  const s = Math.floor(segundos % 60);
  const m = Math.floor(segundos / 60) % 60;
  const h = Math.floor(segundos / 3600);
  if (h > 0) return `${h}h ${m}m ${s}s`;
  if (m > 0) return `${m} min ${s}s`;
  return `${s}s`;
}

function nombresCoinciden(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

/** Fila de historial (públicas o privadas): verde / rojo / amarillo (pendiente, empate, pausa…). */
function FilaHistorialPartidaCard({
  rivalNombre,
  jugadorNombre,
  estado,
  ganador,
  tiempo,
}: {
  rivalNombre: string;
  jugadorNombre: string;
  estado: string;
  ganador: string | number;
  tiempo: number;
}) {
  const ganadorStr = String(ganador ?? "").trim();
  const est = String(estado ?? "").trim();

  const soyGanador =
    ganadorStr.length > 0 &&
    ganadorStr !== "Empate" &&
    ganadorStr !== "NO_HAY" &&
    nombresCoinciden(ganadorStr, jugadorNombre);
  const soyPerdedor =
    est.toUpperCase() === "FINALIZADA" &&
    ganadorStr.length > 0 &&
    ganadorStr !== "Empate" &&
    ganadorStr !== "NO_HAY" &&
    !nombresCoinciden(ganadorStr, jugadorNombre);

  const panelVictoria = soyGanador;
  const panelDerrota = soyPerdedor;

  const duracionTxt = formatearDuracionPartida(tiempo);

  const liClass = panelVictoria
    ? "rounded-xl border border-emerald-500/90 bg-emerald-600 px-4 py-3 text-sm text-white shadow-sm"
    : panelDerrota
      ? "rounded-xl border border-red-600/90 bg-red-700 px-4 py-3 text-sm text-white shadow-sm"
      : "rounded-xl border border-amber-400/90 bg-amber-500 px-4 py-3 text-sm text-white shadow-sm";

  const textoDuracion = "text-white/80 text-xs mt-2";

  return (
    <li className={liClass}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <span className="font-medium text-white">vs @{rivalNombre}</span>
        <div className="flex items-center gap-2 min-w-0">
          {panelVictoria ? (
            <>
              <div className="relative w-9 h-9 shrink-0">
                <Image
                  src="/emoteVictoria.png"
                  alt=""
                  fill
                  className="object-contain drop-shadow-sm"
                  sizes="36px"
                />
              </div>
              <span className="font-semibold text-white">
                Ganador: <span className="font-bold">@{ganadorStr}</span>
              </span>
            </>
          ) : panelDerrota ? (
            <>
              <div className="relative w-9 h-9 shrink-0">
                <Image
                  src="/emoteDerrota.png"
                  alt=""
                  fill
                  className="object-contain drop-shadow-sm"
                  sizes="36px"
                />
              </div>
              <span className="font-semibold text-white">
                Ganador: <span className="font-bold">@{ganadorStr}</span>
              </span>
            </>
          ) : (
            <>
              <div className="relative w-9 h-9 shrink-0">
                <Image
                  src="/katanas.png"
                  alt=""
                  width={36}
                  height={36}
                  className="object-contain drop-shadow-sm"
                />
              </div>
              <span className="font-semibold text-white">Ganador: NO_HAY</span>
            </>
          )}
        </div>
      </div>
      {duracionTxt ? <p className={textoDuracion}>Duración: {duracionTxt}</p> : null}
    </li>
  );
}

function PanelMiCuenta({
  jugador,
  partidasPublicas,
  cargandoPartidasPublicas,
}: {
  jugador: DatosSesion;
  partidasPublicas: ResumenPartidaPublica[];
  cargandoPartidasPublicas: boolean;
}) {
  const ultimas = [...partidasPublicas].slice(-10).reverse();

  return (
    <div className="max-w-3xl mx-auto px-6 py-8">
      <h2 className="text-xl font-bold text-stone-800 uppercase tracking-widest mb-2">Mi cuenta</h2>
      <p className="text-stone-500 text-sm mb-8">
        Datos de tu perfil. El cambio de nombre y contraseña estará disponible cuando el servidor lo soporte.
      </p>

      <div className="rounded-2xl border border-stone-200 bg-white shadow-sm p-6 mb-8">
        <div className="flex flex-col sm:flex-row sm:items-center gap-4 mb-6">
          <div className="w-16 h-16 rounded-full bg-[#1a2d4a] text-white flex items-center justify-center text-2xl font-bold shrink-0">
            {jugador.nombre.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="text-lg font-semibold text-stone-900">@{jugador.nombre}</p>
            <p className="text-sm text-stone-500">{jugador.correo}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="rounded-xl bg-stone-50 border border-stone-100 px-4 py-3">
            <p className="text-[10px] uppercase tracking-widest text-stone-400 mb-1">Katanas</p>
            <div className="flex items-center gap-2">
              <Image src="/katanas.png" alt="" width={20} height={20} className="h-5 w-auto" />
              <span className="font-mono font-bold text-stone-800">{jugador.puntos.toLocaleString()}</span>
            </div>
          </div>
          <div className="rounded-xl bg-stone-50 border border-stone-100 px-4 py-3">
            <p className="text-[10px] uppercase tracking-widest text-stone-400 mb-1">Cores</p>
            <div className="flex items-center gap-2">
              <Image src="/core.png" alt="" width={20} height={20} className="h-5 w-auto" />
              <span className="font-mono font-bold text-stone-800">{jugador.cores.toLocaleString()}</span>
            </div>
          </div>
          <div className="rounded-xl bg-stone-50 border border-stone-100 px-4 py-3">
            <p className="text-[10px] uppercase tracking-widest text-stone-400 mb-1">Partidas jugadas</p>
            <span className="font-mono font-bold text-stone-800 text-lg">{jugador.partidas_jugadas}</span>
          </div>
          <div className="rounded-xl bg-stone-50 border border-stone-100 px-4 py-3">
            <p className="text-[10px] uppercase tracking-widest text-stone-400 mb-1">Partidas ganadas</p>
            <span className="font-mono font-bold text-stone-800 text-lg">{jugador.partidas_ganadas}</span>
          </div>
        </div>
      </div>

      <div>
        <h3 className="text-sm font-bold text-stone-700 uppercase tracking-widest mb-4">
          Últimas partidas públicas
        </h3>
        {cargandoPartidasPublicas ? (
          <p className="text-stone-400 text-sm animate-pulse">Cargando historial…</p>
        ) : ultimas.length === 0 ? (
          <p className="text-stone-500 text-sm rounded-xl border border-dashed border-stone-300 bg-stone-50 px-4 py-6 text-center">
            No hay partidas públicas registradas o no hay conexión con el servidor.
          </p>
        ) : (
          <ul className="space-y-2">
            {ultimas.map((p, idx) => (
              <FilaHistorialPartidaCard
                key={`${p.oponente}-${p.tiempo}-${idx}`}
                rivalNombre={p.oponente}
                jugadorNombre={jugador.nombre}
                estado={p.estado}
                ganador={p.ganador}
                tiempo={p.tiempo}
              />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function CardPreviewSkin({ skinId }: { skinId: SkinId }) {
  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="rounded-xl border border-stone-200 bg-stone-50 p-3">
        <p className="text-xs font-bold uppercase tracking-wider text-stone-600 mb-2">Equipo rojo</p>
        <div className="flex items-center gap-2">
          <Image src={getPiezaSrc("peon", 2, skinId)} alt="Peón rojo" width={36} height={36} />
          <Image src={getPiezaSrc("rey", 2, skinId)} alt="Rey rojo" width={36} height={36} />
          <Image src={getPiezaSrc("templo", 2, skinId)} alt="Templo rojo" width={36} height={36} />
        </div>
      </div>
      <div className="rounded-xl border border-stone-200 bg-stone-50 p-3">
        <p className="text-xs font-bold uppercase tracking-wider text-stone-600 mb-2">Equipo azul</p>
        <div className="flex items-center gap-2">
          <Image src={getPiezaSrc("peon", 1, skinId)} alt="Peón azul" width={36} height={36} />
          <Image src={getPiezaSrc("rey", 1, skinId)} alt="Rey azul" width={36} height={36} />
          <Image src={getPiezaSrc("templo", 1, skinId)} alt="Templo azul" width={36} height={36} />
        </div>
      </div>
    </div>
  );
}

function PanelMisTableros({
  jugador,
  skins,
  cargando,
  accionSkinEnCurso,
  onUsarSkin,
}: {
  jugador: DatosSesion;
  skins: SkinEstado[];
  cargando: boolean;
  accionSkinEnCurso: string | null;
  onUsarSkin: (skinId: SkinId) => void;
}) {
  const compradas = skins.filter((s) => s.owned);
  return (
    <div className="max-w-4xl mx-auto px-6 py-8">
      <h2 className="text-xl font-bold text-stone-800 uppercase tracking-widest mb-2">Mis tableros</h2>
      <p className="text-stone-500 text-sm mb-6">Skin activa actual: <span className="font-semibold">{getSkinNombre(normalizarSkinId(jugador.skin_activa))}</span></p>
      {cargando ? (
        <p className="text-stone-500 animate-pulse">Cargando skins...</p>
      ) : compradas.length === 0 ? (
        <p className="text-stone-500">Aún no tienes skins compradas.</p>
      ) : (
        <div className="space-y-4">
          {compradas.map((s) => (
            <div key={s.skin_id} className="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
              <div className="flex items-center justify-between gap-3 mb-3">
                <div>
                  <p className="text-lg font-bold text-stone-800">{getSkinNombre(s.skin_id)}</p>
                </div>
                {s.es_activa ? (
                  <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-700">Activa</span>
                ) : (
                  <button
                    type="button"
                    onClick={() => onUsarSkin(s.skin_id)}
                    disabled={accionSkinEnCurso === `usar-${s.skin_id}`}
                    className="px-4 py-2 rounded-lg text-sm font-semibold bg-[#1a2d4a] text-white hover:bg-[#203a60] disabled:opacity-50"
                  >
                    {accionSkinEnCurso === `usar-${s.skin_id}` ? "Aplicando..." : "Usar"}
                  </button>
                )}
              </div>
              <CardPreviewSkin skinId={s.skin_id} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function PanelTiendaSkins({
  jugador,
  skins,
  cargando,
  accionSkinEnCurso,
  onComprarSkin,
}: {
  jugador: DatosSesion;
  skins: SkinEstado[];
  cargando: boolean;
  accionSkinEnCurso: string | null;
  onComprarSkin: (skinId: SkinId) => void;
}) {
  return (
    <div className="max-w-4xl mx-auto px-6 py-8">
      <h2 className="text-xl font-bold text-stone-800 uppercase tracking-widest mb-2">Tienda</h2>
      <p className="text-stone-500 text-sm mb-6 flex items-center gap-2">
        <span>Tus cores:</span>
        <Image src="/core.png" alt="Cores" width={16} height={16} />
        <span className="font-semibold">{jugador.cores.toLocaleString()}</span>
      </p>
      {cargando ? (
        <p className="text-stone-500 animate-pulse">Cargando catálogo...</p>
      ) : (
        <div className="space-y-4">
          {skins
            .filter((s) => s.skin_id !== "Skin0")
            .map((s) => {
              const sinCores = jugador.cores < s.precio;
              return (
                <div key={s.skin_id} className="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
                  <div className="flex items-center justify-between gap-3 mb-3">
                    <div>
                      <p className="text-lg font-bold text-stone-800">{getSkinNombre(s.skin_id)}</p>
                      <p className="text-xs text-stone-500 uppercase tracking-wider flex items-center gap-1">
                        <Image src="/core.png" alt="Cores" width={12} height={12} />
                        <span>{s.precio} cores</span>
                      </p>
                    </div>
                    {s.es_activa ? (
                      <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-700">Activa</span>
                    ) : s.owned ? (
                      <span className="px-3 py-1 rounded-full text-xs font-bold bg-stone-200 text-stone-600">
                        Ya adquirida
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => onComprarSkin(s.skin_id)}
                        disabled={sinCores || accionSkinEnCurso === `comprar-${s.skin_id}`}
                        className="px-4 py-2 rounded-lg text-sm font-semibold bg-amber-600 text-white hover:bg-amber-500 disabled:opacity-50"
                      >
                        {accionSkinEnCurso === `comprar-${s.skin_id}` ? "Comprando..." : sinCores ? "Sin cores" : "Comprar"}
                      </button>
                    )}
                  </div>
                  <CardPreviewSkin skinId={s.skin_id} />
                </div>
              );
            })}
        </div>
      )}
    </div>
  );
}

interface PanelAmigosProps {
  jugador: DatosSesion;
  amigos: InfoAmigo[];
  amigoSeleccionado: InfoAmigo | null;
  partidasConAmigo: ResumenPartidaAmigo[];
  cargandoPartidasAmigo: boolean;
  mostrarModalPartidasAmigo: boolean;
  tabActiva: "lista" | "buscar";
  onCambiarTab: (tab: "lista" | "buscar") => void;
  textoBusqueda: string;
  onCambiarBusqueda: (texto: string) => void;
  resultados: InfoJugadorBusqueda[];
  buscando: boolean;
  solicitudesEnviadas: Set<string>;
  onEnviarSolicitud: (destinatario: string) => void;
  onSeleccionarAmigo: (amigo: InfoAmigo) => void;
  onBorrarAmigo: (amigo: InfoAmigo) => void;
  onCerrarModalPartidas: () => void;
}

function PanelAmigos({
  jugador,
  amigos,
  amigoSeleccionado,
  partidasConAmigo,
  cargandoPartidasAmigo,
  mostrarModalPartidasAmigo,
  tabActiva,
  onCambiarTab,
  textoBusqueda,
  onCambiarBusqueda,
  resultados,
  buscando,
  solicitudesEnviadas,
  onEnviarSolicitud,
  onSeleccionarAmigo,
  onBorrarAmigo,
  onCerrarModalPartidas,
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
            </div>
          ) : (
            <>
              <ul className="space-y-2">
                {amigos.map((amigo) => (
                  <li
                    key={amigo.nombre}
                    className={`flex items-center gap-3 bg-white rounded-xl px-4 py-3 shadow-sm border ${
                      amigoSeleccionado?.nombre === amigo.nombre
                        ? "border-[#1a2d4a]"
                        : "border-transparent"
                    }`}
                  >
                    <button
                      type="button"
                      className="flex items-center gap-3 flex-1 text-left"
                      onClick={() => onSeleccionarAmigo(amigo)}
                    >
                      <div className="w-9 h-9 rounded-full bg-[#1a2d4a] flex items-center justify-center text-white font-bold text-sm">
                        {amigo.nombre.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <p className="font-medium text-stone-800">{amigo.nombre}</p>
                        <p className="text-xs text-stone-500 flex items-center gap-1 mt-0.5">
                          <Image src="/katanas.png" alt="Katanas" width={12} height={12} className="h-3 w-auto" />
                          <span>{amigo.puntos}</span>
                        </p>
                      </div>
                    </button>
                    <button
                      type="button"
                      onClick={() => onBorrarAmigo(amigo)}
                      className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-red-100 text-red-700 hover:bg-red-200 transition-colors"
                    >
                      Borrar
                    </button>
                  </li>
                ))}
              </ul>

            </>
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
                    const esAmigo = amigos.some((a) => a.nombre === j.nombre);
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
                          <p className="text-xs text-stone-500 flex items-center gap-1 mt-0.5">
                            <Image src="/katanas.png" alt="Katanas" width={12} height={12} className="h-3 w-auto" />
                            <span>{j.puntos}</span>
                          </p>
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

      {/* Modal grande: historial con amigo */}
      {mostrarModalPartidasAmigo && amigoSeleccionado && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-[1px] flex items-center justify-center p-4">
          <div className="w-full max-w-3xl bg-white rounded-3xl shadow-2xl p-6 md:p-8 relative">
            <button
              type="button"
              onClick={onCerrarModalPartidas}
              className="absolute right-5 top-4 text-3xl leading-none text-black hover:text-stone-600"
              aria-label="Cerrar"
            >
              ×
            </button>

            <div className="grid grid-cols-1 md:grid-cols-[220px_1fr] gap-6 items-start">
              <div className="flex flex-col items-center">
                <div className="w-28 h-28 rounded-full bg-stone-200 flex items-center justify-center text-3xl text-stone-500">
                  {amigoSeleccionado.nombre.charAt(0).toUpperCase()}
                </div>
                <p className="mt-3 text-xl font-semibold text-stone-800">@{amigoSeleccionado.nombre}</p>
                <p className="mt-2 text-sm text-stone-600 flex items-center gap-1">
                  <Image src="/katanas.png" alt="Katanas" width={16} height={16} className="h-4 w-auto" />
                  <span>{amigoSeleccionado.puntos}</span>
                </p>
              </div>

              <div>
                <h3 className="text-2xl font-extrabold uppercase tracking-wide text-stone-800 mb-4">
                  Tus últimas partidas contra @{amigoSeleccionado.nombre}
                </h3>

                {cargandoPartidasAmigo ? (
                  <div className="rounded-2xl bg-stone-200 text-stone-600 px-6 py-6 text-center font-semibold">
                    Cargando historial...
                  </div>
                ) : partidasConAmigo.length === 0 ? (
                  <div className="rounded-2xl bg-stone-200 text-stone-700 px-6 py-6 text-center font-semibold">
                    No tiene partidas jugadas.
                  </div>
                ) : (
                  <ul className="space-y-3 max-h-[360px] overflow-y-auto pr-1">
                    {partidasConAmigo.map((p, idx) => (
                      <FilaHistorialPartidaCard
                        key={`${p.oponente}-${p.estado}-${p.tiempo}-${idx}`}
                        rivalNombre={p.oponente}
                        jugadorNombre={jugador.nombre}
                        estado={p.estado}
                        ganador={p.ganador}
                        tiempo={p.tiempo}
                      />
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────

interface PanelNotificacionesProps {
  notificaciones: Notificacion[];
  onAceptarAmistad: (notif: Notificacion) => void;
  onRechazarAmistad: (notif: Notificacion) => void;
  onAceptarInvitacionPartida: (notif: Notificacion) => void;
  onRechazarInvitacionPartida: (notif: Notificacion) => void;
}

function PanelNotificaciones({
  notificaciones,
  onAceptarAmistad,
  onRechazarAmistad,
  onAceptarInvitacionPartida,
  onRechazarInvitacionPartida,
}: PanelNotificacionesProps) {
  const items = notificaciones;

  return (
    <div className="max-w-xl mx-auto px-6 py-8">
      <h2 className="text-xl font-bold text-stone-800 uppercase tracking-widest mb-6">
        Notificaciones
      </h2>

      {items.length === 0 ? (
        <div className="text-center py-16 text-stone-400">
          <p className="text-4xl mb-3">🔔</p>
          <p className="font-semibold">Sin notificaciones pendientes.</p>
          <p className="text-sm mt-1">Aquí aparecerán solicitudes de amistad e invitaciones privadas.</p>
        </div>
      ) : (
        <ul className="space-y-3">
          {items.map((notif) => (
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
                  {notif.tipo === "SOLICITUD_AMISTAD"
                    ? "Te ha enviado una solicitud de amistad"
                    : "Te ha invitado a una partida privada"}
                </p>
              </div>

              {/* Acciones */}
              <div className="flex gap-2 shrink-0">
                <button
                  type="button"
                  onClick={() =>
                    notif.tipo === "SOLICITUD_AMISTAD"
                      ? onAceptarAmistad(notif)
                      : onAceptarInvitacionPartida(notif)
                  }
                  className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-[#1a2d4a] text-white hover:bg-[#203a60] transition-colors"
                >
                  Aceptar
                </button>
                <button
                  type="button"
                  onClick={() =>
                    notif.tipo === "SOLICITUD_AMISTAD"
                      ? onRechazarAmistad(notif)
                      : onRechazarInvitacionPartida(notif)
                  }
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
