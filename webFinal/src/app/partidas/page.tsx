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

import { useState, useEffect, useCallback, useRef, useMemo } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { obtenerJugadorActivo, guardarSesion, cerrarSesion, type DatosSesion } from "@/lib/sesion";
import { obtenerPerfil, cambiarAvatar, cambiarContrasena } from "@/api/auth";
import { activarSkin, comprarSkin, obtenerTiendaSkins, type SkinEstado } from "@/api/skins";
import { obtenerCartas, obtenerCartasAccion, type CartaEstado } from "@/api/cartas";
import { CartaAccionFicha, getDescripcionCartaAccion } from "@/lib/cartasAccionVisual";
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
import {
  enviarSolicitarReanudar,
  enviarAceptarReanudar,
  enviarRechazarReanudar,
} from "@/api/partida";
import * as WS from "@/api/ws";
import { getSkinNombre, getPiezaSrc, getSkinPrecio, normalizarSkinId, type SkinId } from "@/lib/skins";
import { getImagenCarta, TODAS_LAS_CARTAS, type CartaMovDef } from "@/lib/cartas";
import { getAvatarSrc, AvatarCircle } from "@/lib/avatar";
import { validarContrasena, HINT_CONTRASENA } from "@/lib/validacion";
import TutorialOverlay, { type PasoTutorial } from "@/components/TutorialOverlay";

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
    descripcion: "Lucha contra el robot Iron sin cartas de acción ni casillas trampa.",
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
  { id: "cartas", label: "Mis cartas", icono: "/MisCartas.png" },
  { id: "tableros", label: "Mis tableros", icono: "/MisTableros.png" },
  { id: "amigos", label: "Mis amigos", icono: "/MisAmigos.png" },
  { id: "tienda", label: "Tienda", icono: "/Tienda.png" },
  { id: "notificaciones", label: "Notificaciones", icono: "/Notificiones.png" },
  { id: "cuenta", label: "Mi cuenta", icono: "/MiCuenta.png" },
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

const TUTORIAL_LS_KEY = "onitama_tutorial_completado";

export default function PartidasPage() {
  const router = useRouter();
  const [jugador, setJugador] = useState<DatosSesion>(obtenerJugadorActivo);
  const [mostrarModalDificultad, setMostrarModalDificultad] = useState(false);
  const [mostrarModalPartidaPrivada, setMostrarModalPartidaPrivada] = useState(false);
  const [tabPartidaPrivada, setTabPartidaPrivada] = useState<"crear" | "reanudar">("crear");
  const [invitacionPrivadaEnCurso, setInvitacionPrivadaEnCurso] = useState<{
    destinatario: string;
    idNotificacion: number | null;
  } | null>(null);
  const [mensajePrivada, setMensajePrivada] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);
  const [tiempoEsperaPrivada, setTiempoEsperaPrivada] = useState(120);
  const [tiempoEsperaReanudar, setTiempoEsperaReanudar] = useState(120);

  // ── Tutorial ─────────────────────────────────────────────────────────────────
  const [tutorialActivo, setTutorialActivo] = useState(false);
  const [mostrarModalTutorial, setMostrarModalTutorial] = useState(false);

  // ── Reanudar partida pausada ─────────────────────────────────────────────────
  /** Solicitud de reanudar entrante desde el amigo */
  const [solicitudReanudarEntrante, setSolicitudReanudarEntrante] = useState<{
    remitente: string;
    idNotificacion: number;
    idPartida: number;
  } | null>(null);
  /** Reanudar en curso (yo lo solicité, esperando respuesta del amigo) */
  const [reanudarEnCurso, setReanudarEnCurso] = useState<{
    amigo: string;
    idPartida: number;
    idNotificacion: number | null;
  } | null>(null);

  /** Refs para ERROR_NO_UNIDO: el servidor usa el mismo tipo para invitación privada y reanudar. */
  const invitacionPrivadaRef = useRef(invitacionPrivadaEnCurso);
  const reanudarEnCursoRef = useRef(reanudarEnCurso);
  invitacionPrivadaRef.current = invitacionPrivadaEnCurso;
  reanudarEnCursoRef.current = reanudarEnCurso;

  /** Amigo seleccionado en la pestaña Reanudar */
  const [amigoSeleccionadoReanudar, setAmigoSeleccionadoReanudar] = useState<InfoAmigo | null>(null);
  /** Partidas pausadas con el amigo seleccionado */
  const [partidasPausadas, setPartidasPausadas] = useState<ResumenPartidaAmigo[]>([]);
  const [cargandoPartidasPausadas, setCargandoPartidasPausadas] = useState(false);

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

  const [cartas, setCartas] = useState<CartaEstado[]>([]);
  const [cargandoCartas, setCargandoCartas] = useState(false);
  const [cartasAccion, setCartasAccion] = useState<CartaEstado[]>([]);
  const [cargandoCartasAccion, setCargandoCartasAccion] = useState(false);


  // ── Efectos ─────────────────────────────────────────────────────────────────

  /** Mostrar modal de bienvenida al tutorial si es la primera visita. */
  useEffect(() => {
    try {
      const completado = localStorage.getItem(TUTORIAL_LS_KEY);
      if (!completado) {
        // Pequeño delay para que la pantalla cargue antes de mostrar el modal
        const t = setTimeout(() => setMostrarModalTutorial(true), 1200);
        return () => clearTimeout(t);
      }
    } catch {
      // localStorage no disponible (modo privado, etc.)
    }
  }, []);

  /** Cargar notificaciones del sessionStorage y suscribirse a nuevas por WS. */
  useEffect(() => {
    setMounted(true);
    setNotificaciones(leerNotificaciones());

    // Nuevas notificaciones en tiempo real
    const unsubSolicitud = WS.suscribir("SOLICITUD_AMISTAD", (msg) => {
      const nueva: Notificacion = {
        idNotificacion: msg.idNotificacion as number,
        tipo: "SOLICITUD_AMISTAD",
        remitente: msg.remitente as string,
        avatar_id: (msg.avatar_id as string | undefined) ?? null,
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
        avatar_id: (msg.avatar_id as string | undefined) ?? null,
      };
      setNotificaciones((prev) =>
        prev.find((n) => n.idNotificacion === nueva.idNotificacion)
          ? prev
          : [...prev, nueva]
      );
    });

    // El remitente canceló la notificación antes de que respondiésemos
    const unsubNotifCancelada = WS.suscribir("NOTIFICACION_CANCELADA", (msg) => {
      const id = msg.idNotificacion as number;
      eliminarNotificacion(id);
      setNotificaciones((prev) => prev.filter((n) => n.idNotificacion !== id));
      setSolicitudReanudarEntrante((prev) => prev?.idNotificacion === id ? null : prev);
    });

    // Cuando se acepta una amistad, añadirla a la lista local
    const unsubAmistad = WS.suscribir("AMISTAD_ACEPTADA", (msg) => {
      const amigo = msg.amigo as string;
      const avatarId = (msg.avatar_id as string | undefined) ?? null;
      setAmigos((prev) =>
        prev.some((a) => a.nombre === amigo)
          ? prev
          : [...prev, { nombre: amigo, puntos: 0, avatar_id: avatarId }]
      );
    });

    return () => {
      unsubSolicitud();
      unsubInvitacionPartida();
      unsubNotifCancelada();
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
      .catch(() => { });
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
      .catch(() => { });
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
      .catch(() => { })
      .finally(() => setCargandoSkins(false));
  }, [panelActivo, jugador.nombre]);

  /** Cargar cartas desde el backend / mock al abrir el panel Mis cartas */
  useEffect(() => {
    if (panelActivo !== "cartas") return;
    setCargandoCartas(true);
    obtenerCartas()
      .then((res) => {
        setCartas(res.cartas);
      })
      .catch(() => { })
      .finally(() => setCargandoCartas(false));

    setCargandoCartasAccion(true);
    obtenerCartasAccion()
      .then((res) => {
        setCartasAccion(res.cartas);
      })
      .catch(() => { })
      .finally(() => setCargandoCartasAccion(false));
  }, [panelActivo]);

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

  /** Cargar amigos al abrir la pestaña Reanudar. */
  useEffect(() => {
    if (!mostrarModalPartidaPrivada || tabPartidaPrivada !== "reanudar" || !jugador.nombre) return;
    obtenerAmigos(jugador.nombre)
      .then((lista) => setAmigos(lista))
      .catch(() => setAmigos([]));
    setAmigoSeleccionadoReanudar(null);
    setPartidasPausadas([]);
  }, [mostrarModalPartidaPrivada, tabPartidaPrivada, jugador.nombre]);

  /** Cargar partidas pausadas con el amigo seleccionado en la pestaña Reanudar. */
  useEffect(() => {
    if (!amigoSeleccionadoReanudar || !jugador.nombre) return;
    setCargandoPartidasPausadas(true);
    obtenerPartidasConAmigo(jugador.nombre, amigoSeleccionadoReanudar.nombre)
      .then((lista) => setPartidasPausadas(lista.filter((p) => p.estado === "PAUSADA")))
      .catch(() => setPartidasPausadas([]))
      .finally(() => setCargandoPartidasPausadas(false));
  }, [amigoSeleccionadoReanudar, jugador.nombre]);

  /** Mensajes WS para flujo de invitación a partida privada. */
  useEffect(() => {
    const unsubEncontrada = WS.suscribir("PARTIDA_PRIVADA_ENCONTRADA", (msg) => {
      // partida_nueva: true solo en partidas nuevas (no en reanudaciones)
      const esReanudacion =
        !!reanudarEnCursoRef.current ||
        Boolean(
          msg.tablero_eq1 &&
          msg.tablero_eq2 &&
          (msg.trampa_j1_pos ||
            msg.trampa_j2_pos ||
            msg.cartas_accion_jugador ||
            msg.cartas_accion_oponente)
        );
      sessionStorage.setItem("datosPartida", JSON.stringify({ ...msg, partida_nueva: !esReanudacion }));
      setInvitacionPrivadaEnCurso(null);
      setMostrarModalPartidaPrivada(false);
      router.push("/presentacion-partida");
    });

    const unsubRechazada = WS.suscribir("INVITACION_RECHAZADA", () => {
      setInvitacionPrivadaEnCurso(null);
      setMostrarModalPartidaPrivada(false);
      setMensajePrivada("Tu amigo rechazó la solicitud de partida privada.");
    });

    /** Mismo tipo de mensaje del servidor para invitación privada y reanudar (timeout / rechazo). */
    const unsubErrorNoUnido = WS.suscribir("ERROR_NO_UNIDO", () => {
      const esperandoInvitacion = !!invitacionPrivadaRef.current;
      const esperandoReanudar = !!reanudarEnCursoRef.current;
      setInvitacionPrivadaEnCurso(null);
      setReanudarEnCurso(null);
      setMostrarModalPartidaPrivada(false);
      if (esperandoInvitacion) {
        setMensajePrivada("Demasiado tarde: la invitación a partida privada ya no es válida.");
      } else if (esperandoReanudar) {
        setMensajePrivada("Tu amigo no aceptó reanudar la partida a tiempo.");
      } else {
        setMensajePrivada("La solicitud ha expirado o ya no es válida.");
      }
    });

    const unsubDesconectado = WS.suscribir("ERROR_DESCONECTADO", () => {
      setInvitacionPrivadaEnCurso(null);
      setMensajePrivada("Tu amigo no está conectado en este momento.");
    });

    // El servidor confirma el id de notificación creada (invitación o reanudar)
    const unsubNotifEnviada = WS.suscribir("NOTIFICACION_ENVIADA", (msg) => {
      const id = msg.idNotificacion as number;
      setInvitacionPrivadaEnCurso((prev) => prev ? { ...prev, idNotificacion: id } : prev);
      setReanudarEnCurso((prev) => prev ? { ...prev, idNotificacion: id } : prev);
    });

    // Solicitud de reanudar recibida de un amigo
    const unsubSolicitudReanudar = WS.suscribir("SOLICITUD_REANUDAR", (msg) => {
      setSolicitudReanudarEntrante({
        remitente: msg.remitente as string,
        idNotificacion: msg.idNotificacion as number,
        idPartida: msg.idPartida as number,
      });
    });

    return () => {
      unsubEncontrada();
      unsubRechazada();
      unsubErrorNoUnido();
      unsubDesconectado();
      unsubNotifEnviada();
      unsubSolicitudReanudar();
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

  /** Countdown visual de espera de reanudar (2 minutos). */
  useEffect(() => {
    if (!reanudarEnCurso) return;
    setTiempoEsperaReanudar(120);
    const interval = setInterval(() => {
      setTiempoEsperaReanudar((t) => {
        if (t <= 1) { clearInterval(interval); return 0; }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [reanudarEnCurso]);

  /** Debounce: enviar búsqueda al servidor 400 ms después de que el usuario deje de escribir. */
  useEffect(() => {
    const texto = textoBusqueda.trim();
    if (texto.length < 1) {
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
      setInvitacionPrivadaEnCurso({ destinatario: amigo.nombre, idNotificacion: null });
      setMostrarModalPartidaPrivada(false);
    },
    [jugador.nombre]
  );

  /** Envía SOLICITAR_REANUDAR para una partida pausada. */
  const handleReanudarPartida = useCallback(
    (amigo: InfoAmigo, idPartida: number) => {
      const ok = enviarSolicitarReanudar(jugador.nombre, amigo.nombre, idPartida);
      if (!ok) {
        setMensajePrivada("No se pudo enviar la solicitud. Revisa la conexión.");
        return;
      }
      setReanudarEnCurso({ amigo: amigo.nombre, idPartida, idNotificacion: null });
      setMostrarModalPartidaPrivada(false);
    },
    [jugador.nombre]
  );

  /** Cancela una notificación pendiente (invitación o reanudar) enviada por nosotros. */
  const handleCancelarNotificacion = useCallback((idNotificacion: number | null) => {
    if (idNotificacion !== null) {
      WS.enviar({ tipo: "CANCELAR_NOTIFICACION", idNotificacion });
    }
    setInvitacionPrivadaEnCurso(null);
    setReanudarEnCurso(null);
  }, []);

  /** Acepta la solicitud de reanudar del amigo. */
  const handleAceptarReanudar = useCallback(() => {
    if (!solicitudReanudarEntrante) return;
    enviarAceptarReanudar(solicitudReanudarEntrante.idNotificacion, jugador.nombre);
    setSolicitudReanudarEntrante(null);
    // El servidor responderá con PARTIDA_PRIVADA_ENCONTRADA → listener navega a /presentacion-partida
  }, [solicitudReanudarEntrante, jugador.nombre]);

  /** Rechaza la solicitud de reanudar del amigo. */
  const handleRechazarReanudar = useCallback(() => {
    if (!solicitudReanudarEntrante) return;
    enviarRechazarReanudar(solicitudReanudarEntrante.idNotificacion, jugador.nombre);
    setSolicitudReanudarEntrante(null);
  }, [solicitudReanudarEntrante, jugador.nombre]);

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

  // ── Handlers tutorial ────────────────────────────────────────────────────────

  const handleIniciarTutorial = useCallback(() => {
    setMostrarModalTutorial(false);
    setPanelActivo(null);
    setTutorialActivo(true);
  }, []);

  const handleFinalizarTutorial = useCallback((completado?: boolean) => {
    setTutorialActivo(false);
    setPanelActivo(null);
    try { localStorage.setItem(TUTORIAL_LS_KEY, "1"); } catch { /* ignorar */ }
    if (completado) {
      router.push("/partida?modo=entrenamiento&dificultad=Principiante&tutorial=true");
    }
  }, [router]);

  // Array con todos los pasos explicativos del menú.
  // CUIDADO: en algunos pasos meto un setTimeout con promesa (el await new Promise) 
  // porque si el panel lateral no le da tiempo a abrirse en el DOM, el targetId no existe 
  // y el tutorial peta por debajo dejando la pantalla congelada. 
  const pasosTutorial = useMemo<PasoTutorial[]>(() => [
    // ── Pantalla principal ─────────────────────────────────────────────────
    {
      targetId: "tutorial-header-info",
      titulo: "Tu perfil y recursos",
      descripcion: "Tu identidad en Onitama. El avatar te representa. Las Katanas son tu puntuación de ranking: suben al ganar partidas públicas y bajan al perder. Los Cores son la moneda premium para comprar skins en la Tienda.",
      icono: "👤", preferencia: "abajo",
      antesDeIr: () => setPanelActivo(null),
    },
    {
      targetId: "tutorial-btn-tutorial",
      titulo: "Botón de ayuda (?)",
      descripcion: "Si en cualquier momento necesitas repasar algo, pulsa este botón para volver a lanzar este tutorial desde el principio. Siempre estará visible en el encabezado.",
      icono: "❓", preferencia: "abajo",
      antesDeIr: () => setPanelActivo(null),
    },
    {
      targetId: "tutorial-btn-logout",
      titulo: "Cerrar sesión",
      descripcion: "Cuando termines de jugar usa este botón. Se cerrará la conexión con el servidor de forma segura y volverás a la pantalla de inicio de sesión.",
      icono: "🚪", preferencia: "abajo",
      antesDeIr: () => setPanelActivo(null),
    },
    {
      targetId: "tutorial-btn-jugar",
      titulo: "¡A jugar! — Inicio",
      descripcion: "El botón de regreso al menú principal. Desde cualquier panel del menú lateral (cartas, amigos, tienda…) pulsa aquí para volver a esta pantalla con las tres modalidades de partida.",
      icono: "⚔️", preferencia: "derecha",
      antesDeIr: () => setPanelActivo(null),
    },
    {
      targetId: "tutorial-tarjeta-publica",
      titulo: "Partida Pública",
      descripcion: "El matchmaking global. El sistema busca un rival de nivel similar y os conecta automáticamente. Las partidas públicas afectan a tu ranking (±Katanas) e incluyen trampas ocultas en el tablero y cartas de acción (poderes especiales).",
      icono: "🌐", preferencia: "abajo",
      antesDeIr: () => setPanelActivo(null),
    },
    {
      targetId: "tutorial-tarjeta-entrenamiento",
      titulo: "Partida Entrenamiento",
      descripcion: "Juega contra Iron Bot, la IA del juego. Tiene tres niveles: Principiante (bueno para aprender), Guerrero (desafío real) y Maestro (muy difícil). El entrenamiento NO afecta al ranking y NO incluye trampas ni cartas de acción.",
      icono: "🤖", preferencia: "abajo",
      antesDeIr: () => setPanelActivo(null),
    },
    {
      targetId: "tutorial-tarjeta-privada",
      titulo: "Partida Privada",
      descripcion: "Reta a un amigo de tu lista. Le llegará una invitación y cuando la acepte la partida arrancará. También puedes reanudar partidas privadas pausadas desde la pestaña «Reanudar» del mismo modal.",
      icono: "🤝", preferencia: "abajo",
      antesDeIr: () => setPanelActivo(null),
    },
    // ── Mis Cartas (sidebar — siempre visible, sin cerrar panel) ───────────
    {
      targetId: "tutorial-menu-cartas",
      titulo: "Mis Cartas",
      descripcion: "Tu colección de cartas. Hay dos tipos: las de Movimiento definen cómo pueden moverse tus piezas cada turno, y las de Acción son poderes especiales de un solo uso por partida que pueden cambiar el rumbo del juego.",
      icono: "🃏", preferencia: "derecha",
      // Sin antesDeIr: el sidebar siempre es visible, no necesitamos cerrar el panel actual
    },
    {
      targetId: "tutorial-panel-cartas",
      titulo: "Cartas de Movimiento",
      descripcion: "La pestaña «Movimientos» muestra las cartas que determinan los saltos posibles de tus piezas. Cada carta tiene un patrón de casillas en las que puedes mover. Haz clic en cualquier carta para ver su descripción completa, el diagrama de movimiento y los Katanas necesarios para desbloquearla.",
      icono: "🃏", preferencia: "izquierda",
      antesDeIr: () => setPanelActivo("cartas"),
      esperaMs: 600,
    },
    {
      targetId: "tutorial-panel-cartas",
      titulo: "Cartas de Acción (Poderes)",
      descripcion: "Pulsa la pestaña «Poderes» para ver las cartas de acción. Cada una tiene un efecto especial: revivir un peón, mover al rey a una casilla segura, cegar al rival para que no vea tus cartas, sacrificar piezas, robar una carta del rival… Se usan una sola vez por partida.",
      icono: "✨", preferencia: "izquierda",
      antesDeIr: async () => {
        setPanelActivo("cartas");
        await new Promise(r => setTimeout(r, 100));
        const tabs = document.querySelectorAll('button');
        const tabPoderes = Array.from(tabs).find(b => b.textContent?.includes('Poderes'));
        if (tabPoderes) (tabPoderes as HTMLElement).click();
      },
      esperaMs: 300,
    },
    // ── Mis Tableros (sin cerrar panel) ───────────────────────────────────
    {
      targetId: "tutorial-menu-tableros",
      titulo: "Mis Tableros",
      descripcion: "Tu colección de skins visuales. Cada skin cambia la apariencia de tus piezas (maestro y peones) en el tablero. Puramente estético: no afecta al juego.",
      icono: "🎨", preferencia: "derecha",
    },
    {
      targetId: "tutorial-panel-tableros",
      titulo: "Panel — Skins de piezas",
      descripcion: "Visualiza las skins que tienes. La marcada con borde dorado es la activa. Pulsa «Usar» en otra para cambiarla. Si no tienes skins, visita la Tienda para comprarlas con Cores.",
      icono: "🎨", preferencia: "izquierda",
      antesDeIr: () => setPanelActivo("tableros"),
      esperaMs: 600,
    },
    // ── Tienda ─────────────────────────────────────────────────────────────
    {
      targetId: "tutorial-menu-tienda",
      titulo: "Tienda",
      descripcion: "Aquí puedes comprar skins con tus Cores. Las Cores se ganan jugando partidas públicas y ganando combates. Cuantas más Katanas tengas, más Cores ganarás por partida.",
      icono: "🛒", preferencia: "derecha",
    },
    {
      targetId: "tutorial-panel-tienda",
      titulo: "Panel — Tienda de skins",
      descripcion: "Cada skin tiene su precio en Cores y una vista previa de las piezas. Si tienes Cores suficientes pulsa «Comprar». Después actívala desde «Mis Tableros» para usarla en tus partidas.",
      icono: "🛒", preferencia: "izquierda",
      antesDeIr: () => setPanelActivo("tienda"),
      esperaMs: 600,
    },
    // ── Mis Amigos ─────────────────────────────────────────────────────────
    {
      targetId: "tutorial-menu-amigos",
      titulo: "Mis Amigos",
      descripcion: "Tu red social en Onitama. Gestiona tus amigos, búscalos por nombre y reta a partidas privadas. Tener amigos es imprescindible para jugar partidas privadas.",
      icono: "👥", preferencia: "derecha",
    },
    {
      targetId: "tutorial-panel-amigos",
      titulo: "Lista de amigos",
      descripcion: "Aquí ves todos tus amigos. Haz clic en uno para ver vuestro historial de partidas juntos, retarle a una partida privada o eliminarle de tu lista.",
      icono: "👥", preferencia: "izquierda",
      antesDeIr: () => { setPanelActivo("amigos"); setTabAmigos("lista"); },
      esperaMs: 600,
    },
    {
      targetId: "tutorial-panel-amigos",
      titulo: "Buscar jugadores",
      descripcion: "La pestaña «Buscar» (que puedes ver arriba en el panel) te permite encontrar a cualquier jugador por su nombre de usuario y enviarle una solicitud de amistad. Cuando la acepte aparecerá en tu lista.",
      icono: "🔍", preferencia: "izquierda",
      antesDeIr: () => { setPanelActivo("amigos"); setTabAmigos("buscar"); },
      esperaMs: 400,
    },
    // ── Notificaciones ─────────────────────────────────────────────────────
    {
      targetId: "tutorial-menu-notificaciones",
      titulo: "Notificaciones",
      descripcion: "El badge rojo indica notificaciones pendientes. Aquí llegan solicitudes de amistad de otros jugadores e invitaciones a partidas privadas que te manden.",
      icono: "🔔", preferencia: "derecha",
    },
    {
      targetId: "tutorial-panel-notificaciones",
      titulo: "Panel — Notificaciones",
      descripcion: "Acepta o rechaza cada notificación. Las invitaciones a partida tienen temporizador: si no respondes a tiempo caducan. Las solicitudes de amistad permanecen hasta que decidas.",
      icono: "🔔", preferencia: "izquierda",
      antesDeIr: () => setPanelActivo("notificaciones"),
      esperaMs: 600,
    },
    // ── Mi Cuenta ──────────────────────────────────────────────────────────
    {
      targetId: "tutorial-menu-cuenta",
      titulo: "Mi Cuenta",
      descripcion: "Tu perfil completo: estadísticas, historial de partidas, personalización del avatar y seguridad de la cuenta.",
      icono: "⚙️", preferencia: "derecha",
    },
    {
      targetId: "tutorial-panel-cuenta",
      titulo: "Panel — Mi Cuenta",
      descripcion: "Consulta tus victorias, derrotas y Katanas actuales. Cambia tu avatar eligiendo entre los disponibles y actualiza tu contraseña si lo necesitas. También puedes revisar el historial completo de partidas públicas.",
      icono: "⚙️", preferencia: "izquierda",
      antesDeIr: () => setPanelActivo("cuenta"),
      esperaMs: 600,
    },
    // ── Final: ir a practicar ──────────────────────────────────────────────
    {
      targetId: "tutorial-tarjeta-entrenamiento",
      titulo: "¡Ahora, a practicar!",
      descripcion: "Ya conoces todo el sistema. Ahora vamos a aprender a jugar una partida real. Al pulsar «¡Listo!» se abrirá una partida de entrenamiento contra Iron Bot (Principiante) donde se te explicará paso a paso: el tablero y el uso básico de las cartas. ¡Cuando termine el tutorial puedes jugar libremente!",
      icono: "🥋", preferencia: "abajo",
      antesDeIr: () => { setPanelActivo(null); },
    },
  ], [setPanelActivo, setTabAmigos]);


  // ── Render ───────────────────────────────────────────────────────────────────

  const notifPendientes = notificaciones.length;

  return (
    <div translate="no" style={{ minHeight: "100vh", display: "flex", flexDirection: "column", backgroundColor: "#0a1520" }}>
      {/* ─── Header ─────────────────────────────────────────────────────── */}
      <header style={{
        background: "rgba(10,21,32,0.97)",
        backdropFilter: "blur(12px)",
        WebkitBackdropFilter: "blur(12px)",
        borderBottom: "1px solid rgba(0,200,255,0.08)",
        padding: "0 28px",
        height: 64,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        flexShrink: 0,
        position: "relative",
        gap: 16,
      }}>
        {/* Accent line top */}
        <div aria-hidden style={{ position:"absolute", top:0, left:0, right:0, height:2, background:"linear-gradient(to right, transparent, rgba(184,92,56,0.7) 30%, rgba(201,168,76,0.5) 60%, transparent)", pointerEvents:"none" }} />

        <div style={{ display:"flex", alignItems:"center" }} aria-label="Onitama">
          <Image
            src="/nombre.png"
            alt="Onitama"
            width={130}
            height={36}
            priority
            style={{ height: 34, width: "auto", objectFit: "contain" }}
          />
        </div>

        <div id="tutorial-header-info" style={{ display:"flex", alignItems:"center", gap: 20, flex: 1, justifyContent: "flex-end" }}>
          {/* Avatar */}
          <button
            type="button"
            onClick={() => handleMenuClick("cuenta")}
            style={{
              width: 40, height: 40, flexShrink: 0, borderRadius: "50%",
              border: panelActivo === "cuenta" ? "2px solid #b85c38" : "2px solid rgba(196,181,160,0.25)",
              overflow: "hidden", background: "rgba(26,45,74,0.6)",
              cursor: "pointer", transition: "border-color 0.2s ease",
              boxShadow: panelActivo === "cuenta" ? "0 0 12px rgba(184,92,56,0.4)" : "none",
            }}
            title="Mi cuenta"
            aria-label="Abrir Mi cuenta"
          >
            <AvatarCircle nombre={jugador.nombre} avatarId={jugador.avatar_id} sizeClass="w-full h-full" textClass="text-sm" />
          </button>
          {/* Puntos */}
          <div style={{ display:"flex", alignItems:"center", gap: 7 }}>
            <Image src="/katanas.png" alt="Katanas" width={20} height={20} style={{ height: 18, width: "auto", flexShrink: 0 }} />
            <span style={{ color: "#c4b5a0", fontWeight: 600, fontSize: 14, fontVariantNumeric: "tabular-nums" }}>
              {mounted ? jugador.puntos.toLocaleString() : "---"}
            </span>
          </div>
          {/* Cores */}
          <div style={{ display:"flex", alignItems:"center", gap: 7 }}>
            <Image src="/core.png" alt="Cores" width={20} height={20} style={{ height: 18, width: "auto", flexShrink: 0 }} />
            <span style={{ color: "#c9a84c", fontWeight: 600, fontSize: 14, fontVariantNumeric: "tabular-nums" }}>
              {mounted ? jugador.cores.toLocaleString() : "---"}
            </span>
          </div>
          <div style={{ width: 1, height: 28, background: "rgba(196,181,160,0.15)", flexShrink: 0 }} aria-hidden />
          {/* Tutorial */}
          <button
            id="tutorial-btn-tutorial"
            type="button"
            onClick={() => setMostrarModalTutorial(true)}
            style={{
              flexShrink: 0, width: 30, height: 30, borderRadius: "50%",
              border: "1px solid rgba(0,200,255,0.3)", background: "transparent",
              color: "rgba(0,200,255,0.7)", cursor: "pointer", fontSize: 13,
              fontWeight: 700, display: "flex", alignItems: "center", justifyContent: "center",
              transition: "border-color 0.2s ease, color 0.2s ease",
            }}
            title="Tutorial de la aplicación"
            aria-label="Abrir tutorial"
            onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.borderColor="rgba(0,200,255,0.7)"; (e.currentTarget as HTMLButtonElement).style.color="#00c8ff"; }}
            onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.borderColor="rgba(0,200,255,0.3)"; (e.currentTarget as HTMLButtonElement).style.color="rgba(0,200,255,0.7)"; }}
          >
            ?
          </button>
          {/* Logout */}
          <button
            id="tutorial-btn-logout"
            type="button"
            onClick={() => setMostrarModalCerrarSesion(true)}
            style={{
              flexShrink: 0, padding: 8, background: "transparent", border: "none",
              color: "rgba(196,181,160,0.6)", cursor: "pointer",
              transition: "color 0.2s ease",
            }}
            title="Salir"
            aria-label="Cerrar sesión"
            onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.color="#f87171"; }}
            onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.color="rgba(196,181,160,0.6)"; }}
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ width: 22, height: 22 }}>
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
        <aside style={{
          width: 220, flexShrink: 0, display: "flex", flexDirection: "column",
          background: "rgba(10,21,32,0.98)",
          borderRight: "1px solid rgba(0,200,255,0.07)",
        }}>
          {/* Accent line top */}
          <div aria-hidden style={{ height: 2, background: "linear-gradient(to right, transparent, rgba(184,92,56,0.5), transparent)", opacity: 0.7 }} />

          <button
            id="tutorial-btn-jugar"
            type="button"
            onClick={() => setPanelActivo(null)}
            style={{
              padding: "20px 20px 14px",
              textAlign: "left", width: "100%", background: "transparent",
              border: "none", cursor: "pointer", borderBottom: "1px solid rgba(196,181,160,0.06)",
              transition: "background 0.2s ease",
            }}
            onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.background="rgba(184,92,56,0.06)"; }}
            onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.background="transparent"; }}
          >
            <span style={{ color: "#b85c38", fontWeight: 700, fontSize: 12, letterSpacing: "0.25em", textTransform: "uppercase", display: "flex", alignItems: "center", gap: 10 }}>
              <Image src="/luchadores.png" alt="" width={20} height={20} style={{ width: 18, height: 18, objectFit: "contain" }} />
              ¡A jugar!
            </span>
          </button>

          <nav style={{ display: "flex", flexDirection: "column", marginTop: 4, flex: 1 }}>
            {MENU_LATERAL.map((item) => {
              const activo = panelActivo === item.id;
              return (
                <button
                  key={item.id}
                  id={`tutorial-menu-${item.id}`}
                  type="button"
                  onClick={() => handleMenuClick(item.id)}
                  style={{
                    display: "flex", alignItems: "center",
                    fontSize: 11, fontWeight: 600, letterSpacing: "0.2em", textTransform: "uppercase",
                    padding: "14px 18px 14px 18px", textAlign: "left", cursor: "pointer",
                    borderTop: "none", borderRight: "none", borderBottom: "1px solid rgba(196,181,160,0.05)",
                    borderLeft: activo ? "3px solid #b85c38" : "3px solid transparent",
                    background: activo ? "rgba(184,92,56,0.12)" : "transparent",
                    color: activo ? "#f0ebe1" : "rgba(196,181,160,0.65)",
                    transition: "background 0.15s ease, color 0.15s ease",
                    width: "100%",
                  }}
                  onMouseEnter={e => { if (!activo) { (e.currentTarget as HTMLButtonElement).style.background="rgba(184,92,56,0.06)"; (e.currentTarget as HTMLButtonElement).style.color="rgba(196,181,160,0.9)"; } }}
                  onMouseLeave={e => { if (!activo) { (e.currentTarget as HTMLButtonElement).style.background="transparent"; (e.currentTarget as HTMLButtonElement).style.color="rgba(196,181,160,0.65)"; } }}
                >
                  <span style={{ position: "relative", flexShrink: 0, marginRight: 12 }}>
                    <Image src={item.icono} alt="" width={20} height={20} style={{ width: 18, height: 18, objectFit: "contain", opacity: activo ? 1 : 0.6 }} />
                    {item.id === "notificaciones" && notifPendientes > 0 && (
                      <span style={{
                        position: "absolute", top: -6, right: -6,
                        background: "#ef4444", color: "#fff", fontSize: 9, fontWeight: 700,
                        borderRadius: "50%", minWidth: 16, height: 16,
                        display: "flex", alignItems: "center", justifyContent: "center", padding: "0 3px"
                      }}>
                        {notifPendientes}
                      </span>
                    )}
                  </span>
                  <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </aside>

        {/* ─── Área principal ──────────────────────────────────────────── */}
        <main style={{ flex: 1, backgroundColor: "#0c1925", overflowY: "auto", position: "relative" }}>
          {mensajePrivada && (
            <div style={{
              margin: "20px 24px 0",
              background: "rgba(201,168,76,0.1)", border: "1px solid rgba(201,168,76,0.3)",
              padding: "12px 16px", color: "#c9a84c", fontSize: 13,
              display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12,
            }}>
              <span>{mensajePrivada}</span>
              <button type="button" onClick={() => setMensajePrivada(null)} style={{ background:"none", border:"none", color:"#c9a84c", cursor:"pointer", fontWeight:700, fontSize:18, lineHeight:1 }} aria-label="Cerrar mensaje">×</button>
            </div>
          )}

          {/* Pantalla principal: tarjetas de partida */}
          {!panelActivo && (
            <div style={{ display:"flex", alignItems:"center", justifyContent:"center", minHeight:"100%", padding: "60px 32px", position:"relative" }}>
              {/* Fondo grid sutil */}
              <div className="oni-grid-drift" style={{ position:"absolute", inset:"-10%", width:"120%", height:"120%", pointerEvents:"none", overflow:"hidden" }} aria-hidden>
                <svg style={{ width:"100%", height:"100%", opacity:0.03 }} preserveAspectRatio="xMidYMid slice">
                  <defs><pattern id="partidas-grid" width={60} height={60} patternUnits="userSpaceOnUse"><path d="M 60 0 L 0 0 0 60" fill="none" stroke="#00c8ff" strokeWidth="0.5"/></pattern></defs>
                  <rect width="200%" height="200%" fill="url(#partidas-grid)"/>
                </svg>
              </div>

              <div style={{ display:"flex", flexWrap:"wrap", gap:32, alignItems:"center", justifyContent:"center", position:"relative", zIndex:1 }}>
                {TIPOS_PARTIDA.map((tipo, idx) => (
                  <div
                    key={tipo.id}
                    id={`tutorial-tarjeta-${tipo.id}`}
                    className={`oni-scale-${idx + 1}`}
                    style={{ display:"flex", flexDirection:"column", alignItems:"center", gap: 0 }}
                  >
                    <button
                      type="button"
                      aria-label={tipo.nombre}
                      onClick={() => handleIniciarPartida(tipo.id)}
                      style={{
                        background: "transparent", border: "none", padding: 0, cursor: "pointer",
                        display: "block",
                      }}
                    >
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={tipo.imagen}
                        alt={tipo.nombre}
                        style={{
                          width: 176, height: 176, borderRadius: "50%",
                          objectFit: "cover",
                          border: "2px solid rgba(184,92,56,0.3)",
                          boxShadow: "0 0 32px rgba(184,92,56,0.15), 0 8px 32px rgba(0,0,0,0.5)",
                          transition: "transform 0.25s cubic-bezier(0.22,1,0.36,1), box-shadow 0.25s ease",
                        }}
                        onMouseEnter={e => { (e.currentTarget as HTMLImageElement).style.transform="scale(1.07)"; (e.currentTarget as HTMLImageElement).style.boxShadow="0 0 48px rgba(184,92,56,0.4), 0 16px 48px rgba(0,0,0,0.6)"; }}
                        onMouseLeave={e => { (e.currentTarget as HTMLImageElement).style.transform="scale(1)"; (e.currentTarget as HTMLImageElement).style.boxShadow="0 0 32px rgba(184,92,56,0.15), 0 8px 32px rgba(0,0,0,0.5)"; }}
                      />
                    </button>
                    <div style={{
                      background: "rgba(13,26,42,0.85)", backdropFilter: "blur(8px)",
                      border: "1px solid rgba(184,92,56,0.2)",
                      padding: "16px 20px", textAlign: "center", width: 210, marginTop: 16,
                    }}>
                      <div style={{ width:"100%", height:1, background:"linear-gradient(to right, transparent, rgba(184,92,56,0.6), transparent)", marginBottom:12 }} />
                      <p style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 13, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 6 }}>
                        {tipo.nombre}
                      </p>
                      <p style={{ fontSize: 11, color: "#8a9bb0", lineHeight: 1.6, letterSpacing: "0.02em" }}>{tipo.descripcion}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Panel: Mis amigos */}
          {panelActivo === "amigos" && (
            <div id="tutorial-panel-amigos" className="h-full">
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
            </div>
          )}

          {/* Panel: Notificaciones */}
          {panelActivo === "notificaciones" && (
            <div id="tutorial-panel-notificaciones" className="h-full">
              <PanelNotificaciones
                notificaciones={notificaciones}
                onAceptarAmistad={handleAceptarSolicitud}
                onRechazarAmistad={handleRechazarSolicitud}
                onAceptarInvitacionPartida={handleAceptarInvitacionPartida}
                onRechazarInvitacionPartida={handleRechazarInvitacionPartida}
              />
            </div>
          )}

          {panelActivo === "cuenta" && (
            <div id="tutorial-panel-cuenta" className="h-full">
              <PanelMiCuenta
                jugador={jugador}
                partidasPublicas={partidasPublicas}
                cargandoPartidasPublicas={cargandoPartidasPublicas}
                onPerfilActualizado={(nuevosDatos) => {
                  setJugador((prev) => {
                    const s = { ...prev, ...nuevosDatos };
                    guardarSesion(s);
                    return s;
                  });
                }}
              />
            </div>
          )}

          {panelActivo === "cartas" && (
            <div id="tutorial-panel-cartas" className="h-full">
              <PanelMisCartas
                jugador={jugador}
                cartas={cartas}
                cargando={cargandoCartas}
                cartasAccion={cartasAccion}
                cargandoAccion={cargandoCartasAccion}
              />
            </div>
          )}

          {panelActivo === "tableros" && (
            <div id="tutorial-panel-tableros" className="h-full">
              <PanelMisTableros
                jugador={jugador}
                skins={skins}
                cargando={cargandoSkins}
                accionSkinEnCurso={accionSkinEnCurso}
                onUsarSkin={handleUsarSkin}
              />
            </div>
          )}

          {panelActivo === "tienda" && (
            <div id="tutorial-panel-tienda" className="h-full">
              <PanelTiendaSkins
                jugador={jugador}
                skins={skins}
                cargando={cargandoSkins}
                accionSkinEnCurso={accionSkinEnCurso}
                onComprarSkin={handleComprarSkin}
              />
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

      {/* ─── Modal: Partida privada (doble pestaña) ───────────────────── */}
      {mostrarModalPartidaPrivada && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="w-full max-w-4xl glass-card" style={{ padding: "32px 28px", position: "relative" }}>
            {/* Accent top line */}
            <div className="oni-copper-line" style={{ position: "absolute", top: 0, left: 0, right: 0 }} />

            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 24 }}>
              <h2 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 22, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", display: "flex", alignItems: "center", gap: 10 }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#b85c38" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                Partida privada
              </h2>
              <button
                type="button"
                onClick={() => setMostrarModalPartidaPrivada(false)}
                style={{ background: "none", border: "none", color: "#8a9bb0", cursor: "pointer", fontSize: 22, lineHeight: 1, padding: 4 }}
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>

            {/* Pestañas */}
            <div style={{ display: "flex", borderBottom: "1px solid rgba(196,181,160,0.1)", marginBottom: 24 }}>
              {(["crear", "reanudar"] as const).map((tab) => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setTabPartidaPrivada(tab)}
                  style={{
                    padding: "10px 24px", fontSize: 12, fontWeight: 700,
                    textTransform: "uppercase", letterSpacing: "0.15em",
                    background: "none", border: "none", cursor: "pointer",
                    borderBottom: tabPartidaPrivada === tab ? "2px solid #b85c38" : "2px solid transparent",
                    color: tabPartidaPrivada === tab ? "#b85c38" : "#8a9bb0",
                    transition: "color 0.2s, border-color 0.2s",
                    fontFamily: "var(--font-rajdhani), sans-serif",
                  }}
                >
                  {tab === "crear" ? "Crear nueva partida" : "Reanudar partida"}
                </button>
              ))}
            </div>

            {tabPartidaPrivada === "crear" && (
              <div>
                {amigos.length === 0 ? (
                  <div style={{ textAlign: "center", padding: "48px 0", color: "#8a9bb0" }}>
                    <p style={{ fontWeight: 600 }}>No tienes amigos disponibles para invitar.</p>
                    <p style={{ fontSize: 13, marginTop: 4, opacity: 0.7 }}>Añade amigos desde el panel «Mis amigos».</p>
                  </div>
                ) : (
                  <ul style={{ display: "flex", flexDirection: "column", gap: 8, maxHeight: 360, overflowY: "auto", paddingRight: 4 }}>
                    {amigos.map((amigo) => (
                      <li key={amigo.nombre} style={{ display: "flex", alignItems: "center", gap: 12, background: "rgba(10,21,32,0.5)", border: "1px solid rgba(196,181,160,0.1)", borderRadius: 10, padding: "10px 14px" }}>
                        <AvatarCircle nombre={amigo.nombre} avatarId={amigo.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" />
                        <div style={{ flex: 1 }}>
                          <p style={{ fontWeight: 600, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 16 }}>{amigo.nombre}</p>
                          <p style={{ fontSize: 12, color: "#8a9bb0", display: "flex", alignItems: "center", gap: 4, marginTop: 2 }}>
                            <Image src="/katanas.png" alt="Katanas" width={12} height={12} style={{ height: 12, width: "auto" }} />
                            <span>{amigo.puntos}</span>
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleInvitarPartidaPrivada(amigo)}
                          className="btn-oni-primary"
                          style={{ fontSize: 11, padding: "6px 16px", fontFamily: "var(--font-rajdhani), sans-serif" }}
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
              <div>
                {amigos.length === 0 ? (
                  <div style={{ textAlign: "center", padding: "48px 0", color: "#8a9bb0" }}>
                    <p style={{ fontWeight: 600 }}>No tienes amigos disponibles.</p>
                  </div>
                ) : !amigoSeleccionadoReanudar ? (
                  <>
                    <p style={{ color: "#8a9bb0", fontSize: 13, marginBottom: 12 }}>Selecciona un amigo para ver sus partidas pausadas:</p>
                    <ul style={{ display: "flex", flexDirection: "column", gap: 8, maxHeight: 320, overflowY: "auto", paddingRight: 4 }}>
                      {amigos.map((amigo) => (
                        <li key={amigo.nombre} style={{ display: "flex", alignItems: "center", gap: 12, background: "rgba(10,21,32,0.5)", border: "1px solid rgba(196,181,160,0.1)", borderRadius: 10, padding: "10px 14px" }}>
                          <AvatarCircle nombre={amigo.nombre} avatarId={amigo.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" />
                          <div style={{ flex: 1 }}>
                            <p style={{ fontWeight: 600, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 16 }}>{amigo.nombre}</p>
                          </div>
                          <button
                            type="button"
                            onClick={() => setAmigoSeleccionadoReanudar(amigo)}
                            className="text-xs font-semibold px-4 py-2 rounded-lg bg-[#1a2d4a] text-white hover:bg-[#203a60]"
                          >
                            Ver partidas
                          </button>
                        </li>
                      ))}
                    </ul>
                  </>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={() => { setAmigoSeleccionadoReanudar(null); setPartidasPausadas([]); }}
                      className="text-xs text-stone-500 hover:text-stone-700 mb-3 flex items-center gap-1"
                    >
                      ← Volver a amigos
                    </button>
                    <p className="text-stone-600 text-sm font-semibold mb-2">
                      Partidas pausadas con @{amigoSeleccionadoReanudar.nombre}:
                    </p>
                    {cargandoPartidasPausadas ? (
                      <p className="text-stone-400 text-sm animate-pulse">Cargando…</p>
                    ) : partidasPausadas.length === 0 ? (
                      <p className="text-stone-400 text-sm">No hay partidas pausadas con este amigo.</p>
                    ) : (
                      <ul className="space-y-2 max-h-[300px] overflow-y-auto pr-1">
                        {partidasPausadas.map((p) => (
                          <li key={p.partida_id} className="flex items-center justify-between gap-3 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3">
                            <div>
                              <p className="text-xs font-semibold text-stone-700">Partida #{p.partida_id}</p>
                              <p className="text-xs text-amber-600 font-semibold uppercase tracking-wide">⏸ Pausada</p>
                            </div>
                            <button
                              type="button"
                              onClick={() => handleReanudarPartida(amigoSeleccionadoReanudar, p.partida_id!)}
                              className="text-xs font-semibold px-4 py-2 rounded-lg bg-amber-600 text-white hover:bg-amber-500"
                            >
                              Reanudar
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ─── Modal: solicitud de reanudar entrante ─────────────────────── */}
      {solicitudReanudarEntrante && (
        <div className="fixed inset-0 z-[65] flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-8 flex flex-col items-center gap-5 shadow-2xl max-w-sm w-full mx-4">
            <span className="text-4xl">⏯️</span>
            <h2 className="text-xl font-bold text-white uppercase tracking-widest text-center">
              Solicitud de reanudar
            </h2>
            <p className="text-white/60 text-sm text-center">
              <span className="text-white font-semibold">@{solicitudReanudarEntrante.remitente}</span> quiere reanudar la partida #{solicitudReanudarEntrante.idPartida}. ¿Aceptas?
            </p>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={handleRechazarReanudar}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm border border-white/20 text-white/70 hover:bg-white/10 transition-colors"
              >
                Rechazar
              </button>
              <button
                type="button"
                onClick={handleAceptarReanudar}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-amber-700 text-white hover:bg-amber-600 transition-colors"
              >
                Aceptar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── Pantalla de espera: reanudar en curso ──────────────────────── */}
      {reanudarEnCurso && (
        <div className="fixed inset-0 z-[65] flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-8 flex flex-col items-center gap-6 shadow-2xl max-w-sm w-full mx-4">
            <svg className="animate-spin h-12 w-12 text-amber-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
            </svg>
            <p className="text-white font-bold uppercase tracking-widest text-center">Esperando a @{reanudarEnCurso.amigo}…</p>
            <p className="font-mono text-3xl text-yellow-300">
              {String(Math.floor(tiempoEsperaReanudar / 60)).padStart(2, "0")}:
              {String(tiempoEsperaReanudar % 60).padStart(2, "0")}
            </p>
            <button
              type="button"
              onClick={() => handleCancelarNotificacion(reanudarEnCurso.idNotificacion)}
              className="px-5 py-2 rounded-xl font-bold uppercase tracking-widest text-sm border border-red-500/50 text-red-400 hover:bg-red-500/20 transition-colors"
            >
              Cancelar solicitud
            </button>
          </div>
        </div>
      )}

      {/* ─── Pantalla de espera: invitación privada en curso ───────────── */}
      {mostrarModalCerrarSesion && (
        <div style={{ position:"fixed", inset:0, zIndex:60, display:"flex", alignItems:"center", justifyContent:"center", background:"rgba(5,13,21,0.88)", backdropFilter:"blur(8px)", padding:16 }}>
          <div className="glass-card" style={{ maxWidth:380, width:"100%", padding:"44px 36px", display:"flex", flexDirection:"column", alignItems:"center", gap:20, position:"relative" }}>
            <div className="oni-copper-line" style={{ position:"absolute", top:0, left:0, right:0 }} />
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#f87171" strokeWidth="2" style={{ width:44, height:44 }}>
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <h2 style={{ fontFamily:"var(--font-rajdhani), sans-serif", fontSize:22, fontWeight:700, color:"#f0ebe1", letterSpacing:"0.1em", textTransform:"uppercase", textAlign:"center" }}>
              ¿Cerrar sesión?
            </h2>
            <p style={{ color:"#8a9bb0", fontSize:13, textAlign:"center", lineHeight:1.6 }}>
              Se cerrará la conexión con el servidor y volverás a la página de inicio.
            </p>
            <div style={{ display:"flex", gap:10, width:"100%" }}>
              <button type="button" onClick={() => setMostrarModalCerrarSesion(false)} className="btn-oni-ghost" style={{ flex:1, fontFamily:"var(--font-rajdhani), sans-serif" }}>
                Cancelar
              </button>
              <button type="button" onClick={handleConfirmarCerrarSesion} className="btn-oni-danger" style={{ flex:1, fontFamily:"var(--font-rajdhani), sans-serif" }}>
                Salir
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmacionSkin && (
        <div style={{ position:"fixed", inset:0, zIndex:70, display:"flex", alignItems:"center", justifyContent:"center", background:"rgba(5,13,21,0.88)", backdropFilter:"blur(8px)", padding:16 }}>
          <div className="glass-card" style={{ maxWidth:380, width:"100%", padding:"40px 32px", display:"flex", flexDirection:"column", alignItems:"center", gap:18, position:"relative" }}>
            <div className="oni-copper-line" style={{ position:"absolute", top:0, left:0, right:0 }} />
            <h3 style={{ fontFamily:"var(--font-rajdhani), sans-serif", fontSize:20, fontWeight:700, color:"#f0ebe1", letterSpacing:"0.1em", textTransform:"uppercase", textAlign:"center" }}>
              {confirmacionSkin.tipo === "comprar" ? "Confirmar compra" : "Confirmar selección"}
            </h3>
            <p style={{ color:"#8a9bb0", fontSize:13, textAlign:"center", lineHeight:1.6 }}>
              {confirmacionSkin.tipo === "comprar"
                ? `¿Quieres comprar la skin ${getSkinNombre(confirmacionSkin.skinId)}?`
                : `¿Quieres activar la skin ${getSkinNombre(confirmacionSkin.skinId)}?`}
            </p>
            {confirmacionSkin.tipo === "comprar" && (
              <div style={{ display:"flex", flexDirection:"column", gap:8, width:"100%", background:"rgba(10,21,32,0.6)", border:"1px solid rgba(196,181,160,0.1)", padding:"14px 16px" }}>
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <span style={{ color:"#8a9bb0", fontSize:12, letterSpacing:"0.1em", textTransform:"uppercase" }}>Tus cores</span>
                  <div style={{ display:"flex", alignItems:"center", gap:6 }}>
                    <Image src="/core.png" alt="Cores" width={14} height={14} />
                    <span style={{ color:"#c9a84c", fontWeight:700, fontSize:14 }}>{jugador.cores.toLocaleString()}</span>
                  </div>
                </div>
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center" }}>
                  <span style={{ color:"#8a9bb0", fontSize:12, letterSpacing:"0.1em", textTransform:"uppercase" }}>Coste</span>
                  <div style={{ display:"flex", alignItems:"center", gap:6 }}>
                    <Image src="/core.png" alt="Cores" width={14} height={14} />
                    <span style={{ color:"#b85c38", fontWeight:700, fontSize:14 }}>
                      {skins.find((s) => s.skin_id === confirmacionSkin.skinId)?.precio ?? getSkinPrecio(confirmacionSkin.skinId)}
                    </span>
                  </div>
                </div>
              </div>
            )}
            <div style={{ display:"flex", gap:10, width:"100%" }}>
              <button type="button" onClick={() => setConfirmacionSkin(null)} className="btn-oni-ghost" style={{ flex:1, fontFamily:"var(--font-rajdhani), sans-serif" }}>
                Cancelar
              </button>
              <button type="button" onClick={handleConfirmarAccionSkin} className="btn-oni-primary" style={{ flex:1, fontFamily:"var(--font-rajdhani), sans-serif" }}>
                Confirmar
              </button>
            </div>
          </div>
        </div>
      )}

      {invitacionPrivadaEnCurso && (
        <div style={{ position:"fixed", inset:0, zIndex:50, display:"flex", alignItems:"center", justifyContent:"center", background:"rgba(5,13,21,0.92)", backdropFilter:"blur(8px)", padding:16 }}>
          <div className="glass-card" style={{ maxWidth:480, width:"100%", padding:"52px 44px", textAlign:"center", display:"flex", flexDirection:"column", alignItems:"center", gap:20, position:"relative" }}>
            <div className="oni-copper-line" style={{ position:"absolute", top:0, left:0, right:0 }} />
            {/* Spinner hex */}
            <svg style={{ animation:"spin 1.6s linear infinite", width:52, height:52 }} viewBox="0 0 52 52" fill="none" aria-hidden>
              <style>{"@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }"}</style>
              <polygon points="26,2 50,14 50,38 26,50 2,38 2,14" stroke="#b85c38" strokeWidth="1.5" opacity="0.4"/>
              <polygon points="26,8 44,18 44,34 26,44 8,34 8,18" stroke="#c9a84c" strokeWidth="1" opacity="0.7"/>
            </svg>
            <h3 style={{ fontFamily:"var(--font-rajdhani), sans-serif", fontSize:22, fontWeight:700, color:"#f0ebe1", letterSpacing:"0.1em", textTransform:"uppercase" }}>Esperando respuesta</h3>
            <p style={{ color:"#8a9bb0", fontSize:14, lineHeight:1.6 }}>
              Has invitado a <span style={{ color:"#f0ebe1", fontWeight:600 }}>@{invitacionPrivadaEnCurso.destinatario}</span> a una partida privada.
            </p>
            <p style={{ fontFamily:"monospace", fontSize:42, color:"#c9a84c", fontWeight:700, letterSpacing:"0.05em", lineHeight:1 }}>
              {String(Math.floor(tiempoEsperaPrivada / 60)).padStart(2, "0")}:
              {String(tiempoEsperaPrivada % 60).padStart(2, "0")}
            </p>
            <button
              type="button"
              onClick={() => handleCancelarNotificacion(invitacionPrivadaEnCurso.idNotificacion)}
              className="btn-oni-danger"
              style={{ fontFamily:"var(--font-rajdhani), sans-serif", padding:"10px 28px" }}
            >
              Cancelar solicitud
            </button>
          </div>
        </div>
      )}

      {/* ─── Modal de bienvenida al tutorial ──────────────────────────────── */}
      {mostrarModalTutorial && (
        <div className="fixed inset-0 z-[10000] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div
            style={{
              background: "#1a2d4a",
              border: "1px solid rgba(255,255,255,0.12)",
              borderRadius: 18,
              padding: "40px 36px 32px",
              maxWidth: 440,
              width: "100%",
              boxShadow: "0 32px 80px rgba(0,0,0,0.7)",
              textAlign: "center",
              position: "relative",
            }}
          >
            {/* Línea accent top */}
            <div style={{
              position: "absolute", top: 0, left: 32, right: 32, height: 2,
              background: "linear-gradient(to right, transparent, #b85c38 30%, #c9a84c 70%, transparent)",
              borderRadius: 99,
            }} />

            {/* Icono */}
            <div style={{
              width: 64, height: 64,
              background: "rgba(184,92,56,0.12)",
              borderRadius: "50%",
              display: "flex", alignItems: "center", justifyContent: "center",
              margin: "0 auto 20px",
              fontSize: 28,
              border: "1px solid rgba(184,92,56,0.3)",
            }}>
              🥋
            </div>

            <h2 style={{
              fontFamily: "var(--font-rajdhani), sans-serif",
              fontSize: 22,
              fontWeight: 700,
              color: "#f0ebe1",
              letterSpacing: "0.06em",
              textTransform: "uppercase",
              marginBottom: 12,
            }}>
              ¡Bienvenido al Dojo!
            </h2>

            <p style={{
              color: "#8a9bb0",
              fontSize: 14,
              lineHeight: 1.7,
              marginBottom: 28,
              fontFamily: "var(--font-geist-sans), sans-serif",
            }}>
              Parece que es tu primera vez aquí. ¿Quieres que te guiemos por todas las secciones de la aplicación? El tutorial solo dura un minuto.
            </p>

            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <button
                type="button"
                onClick={handleIniciarTutorial}
                style={{
                  background: "#b85c38",
                  border: "none",
                  borderRadius: 10,
                  color: "#f0ebe1",
                  cursor: "pointer",
                  fontSize: 14,
                  fontWeight: 700,
                  letterSpacing: "0.12em",
                  padding: "13px 24px",
                  textTransform: "uppercase",
                  fontFamily: "var(--font-rajdhani), sans-serif",
                  transition: "background 0.2s",
                  width: "100%",
                }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.background = "#a04e2e"; }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.background = "#b85c38"; }}
              >
                Sí, mostrar tutorial →
              </button>
              <button
                type="button"
                onClick={() => {
                  setMostrarModalTutorial(false);
                  try { localStorage.setItem(TUTORIAL_LS_KEY, "1"); } catch { /* ignorar */ }
                }}
                style={{
                  background: "transparent",
                  border: "1px solid rgba(255,255,255,0.15)",
                  borderRadius: 10,
                  color: "rgba(255,255,255,0.5)",
                  cursor: "pointer",
                  fontSize: 13,
                  fontWeight: 500,
                  padding: "11px 24px",
                  fontFamily: "var(--font-geist-sans), sans-serif",
                  transition: "background 0.2s, color 0.2s",
                  width: "100%",
                }}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLButtonElement).style.background = "rgba(255,255,255,0.06)";
                  (e.currentTarget as HTMLButtonElement).style.color = "rgba(255,255,255,0.75)";
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLButtonElement).style.background = "transparent";
                  (e.currentTarget as HTMLButtonElement).style.color = "rgba(255,255,255,0.5)";
                }}
              >
                No, ya sé cómo funciona
              </button>
            </div>

            <p style={{
              marginTop: 18,
              color: "rgba(255,255,255,0.2)",
              fontSize: 11,
              fontFamily: "var(--font-geist-sans), sans-serif",
            }}>
              Siempre puedes volver a verlo pulsando el botón <strong style={{ color: "rgba(255,255,255,0.35)" }}>?</strong> del encabezado
            </p>
          </div>
        </div>
      )}

      {/* ─── Tutorial interactivo ─────────────────────────────────────────── */}
      <TutorialOverlay
        pasos={pasosTutorial}
        activo={tutorialActivo}
        onFinish={handleFinalizarTutorial}
      />
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

const AVATARES_DISPONIBLES = Array.from({ length: 12 }, (_, i) => `avatar_${(i + 1).toString().padStart(2, "0")}`);

function PanelMiCuenta({
  jugador,
  partidasPublicas,
  cargandoPartidasPublicas,
  onPerfilActualizado,
}: {
  jugador: DatosSesion;
  partidasPublicas: ResumenPartidaPublica[];
  cargandoPartidasPublicas: boolean;
  onPerfilActualizado: (nuevosDatos: Partial<DatosSesion>) => void;
}) {
  const ultimas = [...partidasPublicas].slice(-10).reverse();

  const [modalAvatar, setModalAvatar] = useState(false);
  const [modalPass, setModalPass] = useState(false);

  const [avatarSeleccionado, setAvatarSeleccionado] = useState(jugador.avatar_id || "avatar_01");
  const [guardandoAvatar, setGuardandoAvatar] = useState(false);

  const [passForm, setPassForm] = useState({ actual: "", nueva: "", confirmar: "" });
  const [guardandoPass, setGuardandoPass] = useState(false);
  const [passError, setPassError] = useState("");
  const [passExito, setPassExito] = useState(false);
  const [mostrarPassModal, setMostrarPassModal] = useState(false);

  const handleGuardarAvatar = async () => {
    setGuardandoAvatar(true);
    try {
      const res = await cambiarAvatar(jugador.nombre, avatarSeleccionado);
      if (res.ok) {
        onPerfilActualizado({ avatar_id: avatarSeleccionado });
        setModalAvatar(false);
      } else {
        alert("Error al guardar avatar: " + (res.codigo || "Desconocido"));
      }
    } catch (error: any) {
      alert("Error de conexión: " + (error.message || "No se pudo contactar con el servidor."));
    } finally {
      setGuardandoAvatar(false);
    }
  };

  const handleGuardarPass = async (e: React.FormEvent) => {
    e.preventDefault();
    setPassError("");
    setPassExito(false);
    if (!passForm.actual) {
      setPassError("Introduce tu contraseña actual.");
      return;
    }
    if (passForm.nueva !== passForm.confirmar) {
      setPassError("Las contraseñas no coinciden.");
      return;
    }
    if (!validarContrasena(passForm.nueva)) {
      setPassError(HINT_CONTRASENA);
      return;
    }
    setGuardandoPass(true);
    try {
      const res = await cambiarContrasena(jugador.nombre, passForm.actual, passForm.nueva);
      if (res.ok) {
        setPassExito(true);
        setPassForm({ actual: "", nueva: "", confirmar: "" });
        setTimeout(() => setModalPass(false), 2000);
      } else {
        setPassError("Error al cambiar contraseña: " + (res.codigo || "Desconocido"));
      }
    } catch (error: any) {
      setPassError("Error de conexión: " + (error.message || "No se pudo contactar con el servidor."));
    } finally {
      setGuardandoPass(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: "0 auto", padding: "32px 24px" }}>
      <h2 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 24, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 8, display: "flex", alignItems: "center", gap: 12 }}>
        <Image src="/MiCuenta.png" alt="" width={24} height={24} style={{ width: 24, height: 24, objectFit: "contain", filter: "invert(1) sepia(1) saturate(5) hue-rotate(340deg)" }} />
        Mi cuenta
      </h2>
      <p style={{ color: "#8a9bb0", fontSize: 14, marginBottom: 32 }}>
        Datos de tu perfil y preferencias de la cuenta.
      </p>

      <div className="glass-card" style={{ padding: "32px 24px", marginBottom: 32, display: "flex", flexDirection: "column", gap: 24 }}>
        <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", justifyContent: "space-between", gap: 16 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
            <div style={{ position: "relative", width: 80, height: 80, borderRadius: "50%", border: "2px solid rgba(184,92,56,0.3)", boxShadow: "0 0 20px rgba(184,92,56,0.15)", overflow: "hidden" }}>
              <AvatarCircle nombre={jugador.nombre} avatarId={jugador.avatar_id} sizeClass="w-full h-full" textClass="text-3xl" />
              <button
                onClick={() => { setAvatarSeleccionado(jugador.avatar_id || "avatar_01"); setModalAvatar(true); }}
                style={{ position: "absolute", inset: 0, background: "rgba(10,21,32,0.7)", color: "#f0ebe1", fontSize: 11, fontWeight: 700, letterSpacing: "0.1em", textTransform: "uppercase", display: "flex", alignItems: "center", justifyContent: "center", opacity: 0, transition: "opacity 0.2s" }}
                onMouseEnter={e => e.currentTarget.style.opacity = "1"}
                onMouseLeave={e => e.currentTarget.style.opacity = "0"}
              >
                Cambiar
              </button>
            </div>
            <div>
              <p style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 22, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.05em" }}>@{jugador.nombre}</p>
              <p style={{ color: "rgba(138,155,176,0.8)", fontSize: 13 }}>{jugador.correo}</p>
            </div>
          </div>
          <button
            onClick={() => { setPassError(""); setPassExito(false); setPassForm({ actual: "", nueva: "", confirmar: "" }); setModalPass(true); }}
            className="btn-oni-ghost"
            style={{ fontFamily: "var(--font-rajdhani), sans-serif", padding: "10px 20px" }}
          >
            Cambiar contraseña
          </button>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))", gap: 16 }}>
          <div style={{ background: "rgba(10,21,32,0.6)", border: "1px solid rgba(196,181,160,0.1)", borderRadius: 8, padding: "12px 16px" }}>
            <p style={{ fontSize: 10, color: "#8a9bb0", letterSpacing: "0.2em", textTransform: "uppercase", marginBottom: 6 }}>Katanas</p>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <Image src="/katanas.png" alt="" width={18} height={18} />
              <span style={{ fontFamily: "monospace", fontWeight: 700, fontSize: 18, color: "#c9a84c" }}>{jugador.puntos.toLocaleString()}</span>
            </div>
          </div>
          <div style={{ background: "rgba(10,21,32,0.6)", border: "1px solid rgba(196,181,160,0.1)", borderRadius: 8, padding: "12px 16px" }}>
            <p style={{ fontSize: 10, color: "#8a9bb0", letterSpacing: "0.2em", textTransform: "uppercase", marginBottom: 6 }}>Cores</p>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <Image src="/core.png" alt="" width={18} height={18} />
              <span style={{ fontFamily: "monospace", fontWeight: 700, fontSize: 18, color: "#b85c38" }}>{jugador.cores.toLocaleString()}</span>
            </div>
          </div>
          <div style={{ background: "rgba(10,21,32,0.6)", border: "1px solid rgba(196,181,160,0.1)", borderRadius: 8, padding: "12px 16px" }}>
            <p style={{ fontSize: 10, color: "#8a9bb0", letterSpacing: "0.2em", textTransform: "uppercase", marginBottom: 6 }}>Jugadas</p>
            <span style={{ fontFamily: "monospace", fontWeight: 700, fontSize: 18, color: "#f0ebe1" }}>{jugador.partidas_jugadas}</span>
          </div>
          <div style={{ background: "rgba(10,21,32,0.6)", border: "1px solid rgba(196,181,160,0.1)", borderRadius: 8, padding: "12px 16px" }}>
            <p style={{ fontSize: 10, color: "#8a9bb0", letterSpacing: "0.2em", textTransform: "uppercase", marginBottom: 6 }}>Ganadas</p>
            <span style={{ fontFamily: "monospace", fontWeight: 700, fontSize: 18, color: "#00c8ff" }}>{jugador.partidas_ganadas}</span>
          </div>
        </div>
      </div>

      <div>
        <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 16, fontWeight: 700, color: "#c4b5a0", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 16 }}>
          Últimas partidas públicas
        </h3>
        {cargandoPartidasPublicas ? (
          <p style={{ color: "#8a9bb0", fontSize: 14, animation: "pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite" }}>Cargando historial…</p>
        ) : ultimas.length === 0 ? (
          <div className="glass-card" style={{ padding: "32px", textAlign: "center", borderStyle: "dashed" }}>
            <p style={{ color: "rgba(138,155,176,0.6)", fontSize: 14 }}>No hay partidas públicas registradas o no hay conexión con el servidor.</p>
          </div>
        ) : (
          <ul style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {ultimas.map((p, idx) => (
              <FilaHistorialPartidaCard key={`${p.oponente}-${p.tiempo}-${idx}`} rivalNombre={p.oponente} jugadorNombre={jugador.nombre} estado={p.estado} ganador={p.ganador} tiempo={p.tiempo} />
            ))}
          </ul>
        )}
      </div>

      {/* Modales de Mi Cuenta (Avatar y Pass) actualizados a estética oscura */}
      {modalAvatar && (
        <div style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(5,13,21,0.88)", backdropFilter: "blur(8px)", padding: 16 }}>
          <div className="glass-card" style={{ maxWidth: 500, width: "100%", padding: "32px", position: "relative" }}>
            <button onClick={() => setModalAvatar(false)} style={{ position: "absolute", top: 16, right: 16, background: "none", border: "none", color: "#8a9bb0", cursor: "pointer" }}>
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
            <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 18, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 24 }}>Selecciona un Avatar</h3>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 24 }}>
              {AVATARES_DISPONIBLES.map(id => (
                <button
                  key={id}
                  onClick={() => setAvatarSeleccionado(id)}
                  style={{ position: "relative", aspectRatio: "1/1", borderRadius: "50%", border: avatarSeleccionado === id ? "3px solid #b85c38" : "3px solid transparent", padding: 0, background: "none", cursor: "pointer", transition: "transform 0.2s, border-color 0.2s", transform: avatarSeleccionado === id ? "scale(1.05)" : "scale(1)" }}
                  onMouseEnter={e => { if (avatarSeleccionado !== id) e.currentTarget.style.borderColor = "rgba(196,181,160,0.3)"; }}
                  onMouseLeave={e => { if (avatarSeleccionado !== id) e.currentTarget.style.borderColor = "transparent"; }}
                >
                  <AvatarCircle nombre={jugador.nombre} avatarId={id} sizeClass="w-full h-full" textClass="text-xl" />
                </button>
              ))}
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: 12 }}>
              <button onClick={() => setModalAvatar(false)} className="btn-oni-ghost" style={{ fontFamily: "var(--font-rajdhani), sans-serif" }}>Cancelar</button>
              <button onClick={handleGuardarAvatar} disabled={guardandoAvatar || avatarSeleccionado === jugador.avatar_id} className="btn-oni-primary" style={{ fontFamily: "var(--font-rajdhani), sans-serif", opacity: (guardandoAvatar || avatarSeleccionado === jugador.avatar_id) ? 0.5 : 1 }}>
                {guardandoAvatar ? "Guardando..." : "Guardar foto"}
              </button>
            </div>
          </div>
        </div>
      )}

      {modalPass && (
        <div style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(5,13,21,0.88)", backdropFilter: "blur(8px)", padding: 16 }}>
          <div className="glass-card" style={{ maxWidth: 400, width: "100%", padding: "32px", position: "relative" }}>
            <button onClick={() => setModalPass(false)} style={{ position: "absolute", top: 16, right: 16, background: "none", border: "none", color: "#8a9bb0", cursor: "pointer" }}>
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
            <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 18, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 20 }}>Cambiar contraseña</h3>
            {passExito ? (
              <div style={{ textAlign: "center", padding: "24px 0" }}>
                <div style={{ width: 48, height: 48, background: "rgba(16,185,129,0.1)", color: "#10b981", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 12px" }}>
                  <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth={3} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                </div>
                <p style={{ color: "#34d399", fontWeight: 600 }}>¡Contraseña actualizada!</p>
              </div>
            ) : (
              <form onSubmit={handleGuardarPass}>
                {passError && <p style={{ marginBottom: 16, background: "rgba(239,68,68,0.1)", border: "1px solid rgba(239,68,68,0.3)", color: "#f87171", padding: "8px 12px", borderRadius: 4, fontSize: 13 }}>{passError}</p>}
                
                <div style={{ marginBottom: 16 }}>
                  <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "#8a9bb0", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 6 }}>Contraseña actual</label>
                  <input type={mostrarPassModal ? "text" : "password"} value={passForm.actual} onChange={e => setPassForm(prev => ({ ...prev, actual: e.target.value }))} required className="input-oni w-full" />
                </div>
                <div style={{ marginBottom: 16 }}>
                  <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "#8a9bb0", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 6 }}>Nueva contraseña</label>
                  <input type={mostrarPassModal ? "text" : "password"} value={passForm.nueva} onChange={e => setPassForm(prev => ({ ...prev, nueva: e.target.value }))} required className="input-oni w-full" />
                  <p style={{ fontSize: 10, color: "rgba(138,155,176,0.6)", marginTop: 6, lineHeight: 1.4 }}>{HINT_CONTRASENA}</p>
                </div>
                <div style={{ marginBottom: 24 }}>
                  <label style={{ display: "block", fontSize: 11, fontWeight: 700, color: "#8a9bb0", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: 6 }}>Confirmar contraseña</label>
                  <input type={mostrarPassModal ? "text" : "password"} value={passForm.confirmar} onChange={e => setPassForm(prev => ({ ...prev, confirmar: e.target.value }))} required className="input-oni w-full" />
                </div>

                <div style={{ display: "flex", alignItems: "center", justifyContent: "flex-end", gap: 8, marginBottom: 8 }}>
                  <button type="button" onClick={() => setMostrarPassModal(!mostrarPassModal)} style={{ display: "flex", alignItems: "center", gap: 6, background: "none", border: "none", color: "#8a9bb0", cursor: "pointer", fontSize: 12 }}>
                    {mostrarPassModal ? "Ocultar" : "Mostrar"}
                  </button>
                </div>
                <div style={{ display: "flex", gap: 10, width: "100%" }}>
                  <button type="button" onClick={() => setModalPass(false)} className="btn-oni-ghost" style={{ flex: 1, padding: "10px 16px", fontSize: 12, fontFamily: "var(--font-rajdhani), sans-serif" }}>Cancelar</button>
                  <button type="submit" disabled={guardandoPass} className="btn-oni-primary" style={{ flex: 1, padding: "10px 16px", fontSize: 12, fontFamily: "var(--font-rajdhani), sans-serif", opacity: guardandoPass ? 0.5 : 1 }}>
                    {guardandoPass ? "Guardando..." : "Actualizar"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function CardPreviewSkin({ skinId }: { skinId: SkinId }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginTop: 12 }}>
      <div style={{ background: "rgba(184,92,56,0.1)", border: "1px solid rgba(184,92,56,0.2)", borderRadius: 12, padding: 12 }}>
        <p style={{ fontSize: 11, fontWeight: 700, color: "#b85c38", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Equipo rojo</p>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <Image src={getPiezaSrc("peon", 2, skinId)} alt="Peón rojo" width={36} height={36} style={{ filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.5))" }} />
          <Image src={getPiezaSrc("rey", 2, skinId)} alt="Rey rojo" width={36} height={36} style={{ filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.5))" }} />
          <Image src={getPiezaSrc("templo", 2, skinId)} alt="Templo rojo" width={36} height={36} style={{ filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.5))" }} />
        </div>
      </div>
      <div style={{ background: "rgba(0,200,255,0.1)", border: "1px solid rgba(0,200,255,0.2)", borderRadius: 12, padding: 12 }}>
        <p style={{ fontSize: 11, fontWeight: 700, color: "#00c8ff", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 8 }}>Equipo azul</p>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <Image src={getPiezaSrc("peon", 1, skinId)} alt="Peón azul" width={36} height={36} style={{ filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.5))" }} />
          <Image src={getPiezaSrc("rey", 1, skinId)} alt="Rey azul" width={36} height={36} style={{ filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.5))" }} />
          <Image src={getPiezaSrc("templo", 1, skinId)} alt="Templo azul" width={36} height={36} style={{ filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.5))" }} />
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
    <div style={{ maxWidth: 800, margin: "0 auto", padding: "32px 24px" }}>
      <h2 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 24, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 8, display: "flex", alignItems: "center", gap: 12 }}>
        <Image src="/MisTableros.png" alt="" width={24} height={24} style={{ width: 24, height: 24, objectFit: "contain", filter: "invert(1) sepia(1) saturate(5) hue-rotate(340deg)" }} />
        Mis tableros
      </h2>
      <p style={{ color: "#8a9bb0", fontSize: 14, marginBottom: 24 }}>Skin activa actual: <span style={{ fontWeight: 700, color: "#f0ebe1" }}>{getSkinNombre(normalizarSkinId(jugador.skin_activa))}</span></p>
      {cargando ? (
        <p style={{ color: "#8a9bb0", animation: "pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite" }}>Cargando skins...</p>
      ) : compradas.length === 0 ? (
        <p style={{ color: "rgba(138,155,176,0.8)" }}>Aún no tienes skins compradas.</p>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {compradas.map((s) => (
            <div key={s.skin_id} className="glass-card" style={{ padding: 24 }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, marginBottom: 12 }}>
                <div>
                  <p style={{ fontSize: 18, fontWeight: 700, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", letterSpacing: "0.05em" }}>{getSkinNombre(s.skin_id)}</p>
                </div>
                {s.es_activa ? (
                  <span style={{ background: "rgba(16,185,129,0.1)", color: "#10b981", border: "1px solid rgba(16,185,129,0.2)", padding: "4px 12px", borderRadius: 16, fontSize: 11, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em" }}>Activa</span>
                ) : (
                  <button
                    type="button"
                    onClick={() => onUsarSkin(s.skin_id)}
                    disabled={accionSkinEnCurso === `usar-${s.skin_id}`}
                    className="btn-oni-primary"
                    style={{ fontFamily: "var(--font-rajdhani), sans-serif", opacity: (accionSkinEnCurso === `usar-${s.skin_id}`) ? 0.5 : 1 }}
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
    <div style={{ maxWidth: 800, margin: "0 auto", padding: "32px 24px" }}>
      <h2 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 24, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 8, display: "flex", alignItems: "center", gap: 12 }}>
        <Image src="/Tienda.png" alt="" width={24} height={24} style={{ width: 24, height: 24, objectFit: "contain", filter: "invert(1) sepia(1) saturate(5) hue-rotate(340deg)" }} />
        Tienda
      </h2>
      <p style={{ color: "#8a9bb0", fontSize: 14, marginBottom: 24, display: "flex", alignItems: "center", gap: 8 }}>
        <span>Tus cores:</span>
        <Image src="/core.png" alt="Cores" width={16} height={16} />
        <span style={{ fontWeight: 700, color: "#b85c38" }}>{jugador.cores.toLocaleString()}</span>
      </p>
      {cargando ? (
        <p style={{ color: "#8a9bb0", animation: "pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite" }}>Cargando catálogo...</p>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {skins
            .filter((s) => s.skin_id !== "Skin0")
            .map((s) => {
              const sinCores = jugador.cores < s.precio;
              return (
                <div key={s.skin_id} className="glass-card" style={{ padding: 24 }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, marginBottom: 12 }}>
                    <div>
                      <p style={{ fontSize: 18, fontWeight: 700, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", letterSpacing: "0.05em" }}>{getSkinNombre(s.skin_id)}</p>
                      <p style={{ fontSize: 12, color: "#8a9bb0", textTransform: "uppercase", letterSpacing: "0.1em", display: "flex", alignItems: "center", gap: 4, marginTop: 4 }}>
                        <Image src="/core.png" alt="Cores" width={12} height={12} />
                        <span>{s.precio} cores</span>
                      </p>
                    </div>
                    {s.es_activa ? (
                      <span style={{ background: "rgba(16,185,129,0.1)", color: "#10b981", border: "1px solid rgba(16,185,129,0.2)", padding: "4px 12px", borderRadius: 16, fontSize: 11, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em" }}>Activa</span>
                    ) : s.owned ? (
                      <span style={{ background: "rgba(196,181,160,0.1)", color: "#8a9bb0", border: "1px solid rgba(196,181,160,0.2)", padding: "4px 12px", borderRadius: 16, fontSize: 11, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em" }}>Ya adquirida</span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => onComprarSkin(s.skin_id)}
                        disabled={sinCores || accionSkinEnCurso === `comprar-${s.skin_id}`}
                        className="btn-oni-primary"
                        style={{ fontFamily: "var(--font-rajdhani), sans-serif", opacity: (sinCores || accionSkinEnCurso === `comprar-${s.skin_id}`) ? 0.5 : 1 }}
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
    <div style={{ maxWidth: 600, margin: "0 auto", padding: "32px 24px" }}>
      <h2 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 24, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 24, display: "flex", alignItems: "center", gap: 12 }}>
        <Image src="/MisAmigos.png" alt="" width={24} height={24} style={{ width: 24, height: 24, objectFit: "contain", filter: "invert(1) sepia(1) saturate(5) hue-rotate(340deg)" }} />
        Mis amigos
      </h2>

      {/* Pestañas */}
      <div style={{ display: "flex", borderBottom: "1px solid rgba(196,181,160,0.1)", marginBottom: 24 }}>
        {(["lista", "buscar"] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => onCambiarTab(tab)}
            style={{ padding: "12px 24px", fontSize: 14, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em", transition: "color 0.2s, border-color 0.2s", borderBottom: tabActiva === tab ? "2px solid #b85c38" : "2px solid transparent", color: tabActiva === tab ? "#b85c38" : "#8a9bb0", background: "none" }}
          >
            {tab === "lista" ? "Mis amigos" : "Buscar"}
          </button>
        ))}
      </div>

      {/* Pestaña: lista de amigos */}
      {tabActiva === "lista" && (
        <div>
          {amigos.length === 0 ? (
            <div style={{ textAlign: "center", padding: "64px 0", color: "#8a9bb0" }}>
              <Image
                src="/MisAmigos.png"
                alt=""
                width={64}
                height={64}
                style={{ width: 64, height: 64, objectFit: "contain", margin: "0 auto 12px", opacity: 0.3, filter: "invert(1)" }}
              />
              <p style={{ fontWeight: 600 }}>Aún no tienes amigos añadidos.</p>
              <p style={{ fontSize: 14, marginTop: 4 }}>
                Usa la pestaña &ldquo;Buscar&rdquo; para encontrar jugadores.
              </p>
            </div>
          ) : (
            <>
              <ul style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {amigos.map((amigo) => (
                  <li
                    key={amigo.nombre}
                    className="glass-card"
                    style={{ padding: "12px 16px", display: "flex", alignItems: "center", gap: 12, border: amigoSeleccionado?.nombre === amigo.nombre ? "1px solid #b85c38" : "1px solid rgba(196,181,160,0.1)" }}
                  >
                    <button
                      type="button"
                      style={{ display: "flex", alignItems: "center", gap: 12, flex: 1, textAlign: "left", background: "none", border: "none", color: "inherit", cursor: "pointer" }}
                      onClick={() => onSeleccionarAmigo(amigo)}
                    >
                      <AvatarCircle nombre={amigo.nombre} avatarId={amigo.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" />
                      <div>
                        <p style={{ fontWeight: 600, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 18 }}>{amigo.nombre}</p>
                        <p style={{ fontSize: 12, color: "#8a9bb0", display: "flex", alignItems: "center", gap: 4, marginTop: 2 }}>
                          <Image src="/katanas.png" alt="Katanas" width={12} height={12} style={{ height: 12, width: "auto" }} />
                          <span>{amigo.puntos}</span>
                        </p>
                      </div>
                    </button>
                    <button
                      type="button"
                      onClick={() => onBorrarAmigo(amigo)}
                      className="btn-oni-ghost"
                      style={{ color: "#ef4444", border: "1px solid rgba(239,68,68,0.2)" }}
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
            className="input-oni"
            style={{ width: "100%", marginBottom: 16 }}
          />

          {buscando && (
            <p style={{ fontSize: 14, color: "#8a9bb0", textAlign: "center", padding: "16px 0", animation: "pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite" }}>Buscando…</p>
          )}

          {!buscando && textoBusqueda.trim().length >= 1 && resultados.length === 0 && (
            <p style={{ fontSize: 14, color: "#8a9bb0", textAlign: "center", padding: "16px 0" }}>
              No se encontraron jugadores.
            </p>
          )}

          {!buscando && resultados.length > 0 && (
            <ul style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {resultados.map((j) => {
                const esMismoUsuario = j.nombre === jugador.nombre;
                const esAmigo = amigos.some((a) => a.nombre === j.nombre);
                const yaEnviado = solicitudesEnviadas.has(j.nombre);

                return (
                  <li
                    key={j.nombre}
                    className="glass-card"
                    style={{ padding: "12px 16px", display: "flex", alignItems: "center", gap: 12 }}
                  >
                    <AvatarCircle nombre={j.nombre} avatarId={j.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontWeight: 600, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 18, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{j.nombre}</p>
                      <p style={{ fontSize: 12, color: "#8a9bb0", display: "flex", alignItems: "center", gap: 4, marginTop: 2 }}>
                        <Image src="/katanas.png" alt="Katanas" width={12} height={12} style={{ height: 12, width: "auto" }} />
                        <span>{j.puntos}</span>
                      </p>
                    </div>
                    {!esMismoUsuario && (
                      esAmigo ? (
                        <span style={{ fontSize: 11, fontWeight: 600, padding: "6px 12px", borderRadius: 8, background: "rgba(16,185,129,0.1)", color: "#10b981", border: "1px solid rgba(16,185,129,0.2)" }}>
                          Ya sois amigos
                        </span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => onEnviarSolicitud(j.nombre)}
                          disabled={yaEnviado}
                          className={yaEnviado ? "btn-oni-ghost" : "btn-oni-primary"}
                          style={{ fontSize: 11, padding: "6px 12px", opacity: yaEnviado ? 0.5 : 1 }}
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

        </div>
      )}

      {/* Modal grande: historial con amigo */}
      {mostrarModalPartidasAmigo && amigoSeleccionado && (
        <div style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyItems: "center", justifyContent: "center", background: "rgba(5,13,21,0.88)", backdropFilter: "blur(8px)", padding: 16 }}>
          <div className="glass-card" style={{ maxWidth: 800, width: "100%", padding: "32px", position: "relative" }}>
            <button
              type="button"
              onClick={onCerrarModalPartidas}
              style={{ position: "absolute", top: 16, right: 16, background: "none", border: "none", color: "#8a9bb0", cursor: "pointer", fontSize: 24, lineHeight: 1 }}
              aria-label="Cerrar"
            >
              ×
            </button>

            <div style={{ display: "grid", gridTemplateColumns: "220px 1fr", gap: 24, alignItems: "start" }}>
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                <AvatarCircle
                  nombre={amigoSeleccionado.nombre}
                  avatarId={amigoSeleccionado.avatar_id}
                  sizeClass="w-28 h-28"
                  textClass="text-3xl"
                />
                <p style={{ marginTop: 12, fontSize: 20, fontWeight: 700, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", letterSpacing: "0.05em" }}>@{amigoSeleccionado.nombre}</p>
                <p style={{ marginTop: 8, fontSize: 14, color: "#8a9bb0", display: "flex", alignItems: "center", gap: 4 }}>
                  <Image src="/katanas.png" alt="Katanas" width={16} height={16} style={{ height: 16, width: "auto" }} />
                  <span>{amigoSeleccionado.puntos}</span>
                </p>
              </div>

              <div>
                <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 24, fontWeight: 800, textTransform: "uppercase", letterSpacing: "0.05em", color: "#f0ebe1", marginBottom: 16 }}>
                  Tus últimas partidas contra @{amigoSeleccionado.nombre}
                </h3>

                {cargandoPartidasAmigo ? (
                  <div className="glass-card" style={{ padding: 24, textAlign: "center", color: "#8a9bb0", animation: "pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite" }}>
                    Cargando historial...
                  </div>
                ) : partidasConAmigo.length === 0 ? (
                  <div className="glass-card" style={{ padding: 24, textAlign: "center", color: "#8a9bb0" }}>
                    No tiene partidas jugadas.
                  </div>
                ) : (
                  <ul style={{ display: "flex", flexDirection: "column", gap: 12, maxHeight: 360, overflowY: "auto", paddingRight: 4 }}>
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
    <div style={{ maxWidth: 600, margin: "0 auto", padding: "32px 24px" }}>
      <h2 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 24, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 24, display: "flex", alignItems: "center", gap: 12 }}>
        <Image src="/Notificiones.png" alt="" width={24} height={24} style={{ width: 24, height: 24, objectFit: "contain", filter: "invert(1) sepia(1) saturate(5) hue-rotate(340deg)" }} />
        Notificaciones
      </h2>

      {items.length === 0 ? (
        <div style={{ textAlign: "center", padding: "64px 0", color: "#8a9bb0" }}>
          <Image
            src="/Notificiones.png"
            alt=""
            width={64}
            height={64}
            style={{ width: 64, height: 64, objectFit: "contain", margin: "0 auto 12px", opacity: 0.3, filter: "invert(1)" }}
          />
          <p style={{ fontWeight: 600 }}>Sin notificaciones pendientes.</p>
          <p style={{ fontSize: 14, marginTop: 4 }}>Aquí aparecerán solicitudes de amistad e invitaciones privadas.</p>
        </div>
      ) : (
        <ul style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {items.map((notif) => (
            <li
              key={notif.idNotificacion}
              className="glass-card"
              style={{ padding: "16px", display: "flex", alignItems: "center", gap: 16 }}
            >
              {/* Avatar */}
              <AvatarCircle nombre={notif.remitente} avatarId={notif.avatar_id} sizeClass="w-10 h-10 shrink-0" textClass="text-sm" />

              {/* Texto */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{ fontWeight: 600, color: "#f0ebe1", fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 18, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                  {notif.remitente}
                </p>
                <p style={{ fontSize: 13, color: "#8a9bb0", marginTop: 2 }}>
                  {notif.tipo === "SOLICITUD_AMISTAD"
                    ? "Te ha enviado una solicitud de amistad"
                    : "Te ha invitado a una partida privada"}
                </p>
              </div>

              {/* Acciones */}
              <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
                <button
                  type="button"
                  onClick={() =>
                    notif.tipo === "SOLICITUD_AMISTAD"
                      ? onAceptarAmistad(notif)
                      : onAceptarInvitacionPartida(notif)
                  }
                  className="btn-oni-primary"
                  style={{ fontSize: 11, padding: "6px 12px" }}
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
                  className="btn-oni-ghost"
                  style={{ fontSize: 11, padding: "6px 12px" }}
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

// ─── Mini cuadrícula de la carta ─────────────────────────────────────────────

function MiniGrid({
  carta, colorDots = "#3b82f6", size = 5,
}: {
  carta: CartaMovDef; colorDots?: string; size?: number;
}) {
  const DIM = 7;
  const CENTRO = 3;
  const activas = new Set<string>();
  // Para la vista de Mis cartas, usamos la perspectiva del jugador 2 (signo = 1)
  for (const { dc, df } of carta.movimientos) {
    const gf = CENTRO - df;
    const gc = CENTRO + dc;
    if (gf >= 0 && gf < DIM && gc >= 0 && gc < DIM) activas.add(`${gf},${gc}`);
  }
  return (
    <div
      className="grid shrink-0"
      style={{
        gridTemplateColumns: `repeat(${DIM}, 1fr)`,
        gap: "1px",
        width: size * DIM + (DIM - 1),
        height: size * DIM + (DIM - 1),
      }}
      aria-hidden
    >
      {Array.from({ length: DIM }, (_, f) =>
        Array.from({ length: DIM }, (_, c) => {
          const esC = f === CENTRO && c === CENTRO;
          const esA = activas.has(`${f},${c}`);
          return (
            <div
              key={`${f}-${c}`}
              className={`rounded-[1px] ${esC ? "bg-[#9a8a72]" : "bg-[#c8bba8]"}`}
              style={{
                width: size,
                height: size,
                ...(esA
                  ? {
                    background: colorDots,
                    boxShadow:
                      colorDots === "#f8fafc"
                        ? "inset 0 0 0 1px rgba(15,23,42,0.45)"
                        : undefined,
                  }
                  : {}),
              }}
            />
          );
        })
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────

const FRASES_EPICAS: Record<string, string> = {
  Tigre: "Feroz como la bestia, salta sobre su presa sin piedad.",
  Dragon: "El señor de los cielos, desata su furia con movimientos legendarios.",
  Rana: "Ágil e impredecible, esquiva los ataques saltando en el loto.",
  Conejo: "Veloz como el viento, cambia de posición en un parpadeo.",
  Cangrejo: "Defensa inquebrantable, avanza de lado cortando con sus pinzas.",
  Elefante: "Imparable y gigantesco, aplasta las defensas enemigas con su peso.",
  Ganso: "Elegante y silencioso, cruza el estanque en el momento perfecto.",
  Gallo: "Orgulloso y certero, ataca con espolones antes del amanecer.",
  Mono: "Juguetón e incansable, ataca desde ángulos inesperados.",
  Mantis: "Paciencia mortal, ataca con una velocidad que la vista no persigue.",
  Caballo: "Rápido y contundente, carga en línea rompiendo las filas.",
  Buey: "Fuerte como la roca, no retrocede ante ninguna embestida.",
  Grulla: "Equilibrio perfecto, se eleva con gracia y pica al descender.",
  Oso: "Fuerza abrumadora, protege su territorio con zarpazos brutales.",
  Aguila: "Desde los cielos domina todo, cae en picado y no deja escapatoria.",
  Cobra: "Letal y sigilosa, un solo toque basta para acabar el combate.",

  // Cartas de Acción / Poderes
  "Pensatorium": "Un reflejo etéreo distorsiona la realidad de la batalla.",
  "Santo Grial": "El cáliz vital devuelve la esperanza a los caídos del templo.",
  "Illusia": "El engaño mágico aparta a tu líder de las garras de la muerte.",
  "Requiem": "Un canto fúnebre arrastra un tributo igualitario de sangre.",
  "La Dama del Mar": "Las olas tempestuosas empujan sin piedad hacia el este.",
  "Kelpie": "El espíritu de las aguas emerge cobrándose su justa venganza.",
  "Atrapasueños": "Las intenciones del adversario quedan tejidas en tu propia mente.",
  "Brujeria": "Una niebla ponzoñosa oculta los hilos del destino al forastero.",
  "Finisterra": "El borde del abismo fuerza al enemigo hacia la oscuridad."
};

const ENFOQUES_CARTAS: Record<string, { enfoque: string; alcance: string; icon: string }> = {
  Tigre: { enfoque: "Simétrico", alcance: "Largo", icon: "⚖️" }, // df: 2
  Dragon: { enfoque: "Simétrico", alcance: "Largo", icon: "⚖️" }, // dc: 2, df: 1
  Rana: { enfoque: "Flanco Izquierdo", alcance: "Largo", icon: "⬅️" }, // dc: -2
  Conejo: { enfoque: "Flanco Derecho", alcance: "Largo", icon: "➡️" }, // dc: 2
  Cangrejo: { enfoque: "Simétrico", alcance: "Largo", icon: "⚖️" }, // dc: 2 / -2
  Elefante: { enfoque: "Simétrico", alcance: "Corto", icon: "⚖️" },
  Ganso: { enfoque: "Flanco Izquierdo", alcance: "Corto", icon: "⬅️" },
  Gallo: { enfoque: "Flanco Derecho", alcance: "Corto", icon: "➡️" },
  Mono: { enfoque: "Simétrico", alcance: "Corto", icon: "⚖️" },
  Mantis: { enfoque: "Flanco Derecho", alcance: "Corto", icon: "➡️" },
  Caballo: { enfoque: "Flanco Izquierdo", alcance: "Corto", icon: "⬅️" },
  Buey: { enfoque: "Flanco Derecho", alcance: "Corto", icon: "➡️" },
  Grulla: { enfoque: "Simétrico", alcance: "Corto", icon: "⚖️" },
  Oso: { enfoque: "Simétrico", alcance: "Corto", icon: "⚖️" },
  Aguila: { enfoque: "Flanco Izquierdo", alcance: "Corto", icon: "⬅️" },
  Cobra: { enfoque: "Flanco Derecho", alcance: "Corto", icon: "➡️" },
};

interface PanelMisCartasProps {
  jugador: DatosSesion;
  cartas: CartaEstado[];
  cargando: boolean;
  cartasAccion: CartaEstado[];
  cargandoAccion: boolean;
}

function PanelMisCartas({
  jugador,
  cartas,
  cargando,
  cartasAccion,
  cargandoAccion,
}: PanelMisCartasProps) {
  const [cartaAmpliada, setCartaAmpliada] = useState<CartaEstado | null>(null);
  const [tabCartas, setTabCartas] = useState<"movimientos" | "poderes">("movimientos");
  const [mostrarInfoMovimientos, setMostrarInfoMovimientos] = useState(false);
  const [filtroTexto, setFiltroTexto] = useState("");
  const [filtroEstado, setFiltroEstado] = useState<"todas" | "desbloqueadas" | "bloqueadas">("todas");

  const cartasActuales = tabCartas === "poderes" ? cartasAccion : cartas;
  const cargandoActual = tabCartas === "poderes" ? cargandoAccion : cargando;

  // Maestría
  const cartasDesbloqueadasOriginal = cartasActuales.filter((c) => jugador.puntos >= c.puntos_necesarios);
  const porcentajeDesbloqueado = cartasActuales.length > 0 ? (cartasDesbloqueadasOriginal.length / cartasActuales.length) * 100 : 0;

  const tituloMaestria = (() => {
    const p = porcentajeDesbloqueado;
    if (p === 100) return "Gran Maestro del Templo";
    if (p >= 75) return "Maestro de las Artes";
    if (p >= 50) return "Discípulo Aventajado";
    if (p >= 25) return "Estudiante Prometedor";
    return "Aprendiz de la Arena";
  })();

  const cartasFiltradasGlobal = cartasActuales.filter(c => c.nombre.toLowerCase().includes(filtroTexto.toLowerCase()));

  // Las cartas disponibles limitadas al ELO del jugador (ordenadas de mayor exigencia a menor)
  const desbloqueadas = cartasFiltradasGlobal
    .filter((c) => jugador.puntos >= c.puntos_necesarios)
    .sort((a, b) => b.puntos_necesarios - a.puntos_necesarios);

  // Las cartas bloqueadas (ordenadas para ver cuáles serán las siguientes en conseguirse)
  const bloqueadas = cartasFiltradasGlobal
    .filter((c) => jugador.puntos < c.puntos_necesarios)
    .sort((a, b) => a.puntos_necesarios - b.puntos_necesarios);

  return (
    <div style={{ maxWidth: 1000, margin: "0 auto", padding: "32px 24px" }}>
      <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", marginBottom: 8 }}>
        <h2 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 24, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", display: "flex", alignItems: "center", gap: 12 }}>
          <Image src="/MisCartas.png" alt="" width={24} height={24} style={{ width: 24, height: 24, objectFit: "contain", filter: "invert(1) sepia(1) saturate(5) hue-rotate(340deg)" }} />
          Mis cartas
        </h2>
      </div>

      {/* Tabs para seleccionar el tipo de carta */}
      <div className="flex gap-6 border-b border-stone-200 mb-8">
        <button
          onClick={() => setTabCartas("movimientos")}
          className={`pb-3 uppercase tracking-wider text-sm font-bold transition-all relative ${tabCartas === "movimientos"
            ? "text-stone-800"
            : "text-stone-400 hover:text-stone-600"
            }`}
        >
          Movimientos
          {tabCartas === "movimientos" && (
            <span className="absolute bottom-0 left-0 w-full h-0.5 bg-stone-800 rounded-t-full" />
          )}
        </button>
        <button
          onClick={() => setTabCartas("poderes")}
          className={`pb-3 uppercase tracking-wider text-sm font-bold transition-all relative flex items-center gap-2 ${tabCartas === "poderes"
            ? "text-stone-800"
            : "text-stone-400 hover:text-stone-600"
            }`}
        >
          <span>Poderes</span>
          {tabCartas === "poderes" && (
            <span className="absolute bottom-0 left-0 w-full h-0.5 bg-stone-800 rounded-t-full" />
          )}
        </button>
      </div>

      <>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 24, gap: 16 }}>
          <p style={{ color: "#8a9bb0", fontSize: 14, display: "flex", flexWrap: "wrap", alignItems: "center", gap: 8 }}>
            <span>{tabCartas === "poderes" ? "Poderes que pueden" : "Cartas que pueden"} aparecer en tus partidas según tu ELO actual (</span>
            <Image src="/katanas.png" alt="Katanas" width={16} height={16} style={{ objectFit: "contain" }} />
            <span style={{ fontWeight: 700, color: "#c9a84c" }}>{jugador.puntos.toLocaleString()}</span>
            <span>):</span>
          </p>
          <button
            onClick={() => setMostrarInfoMovimientos(true)}
            style={{ background: "rgba(184,92,56,0.15)", border: "1px solid rgba(184,92,56,0.4)", borderRadius: "50%", padding: 7, cursor: "pointer", color: "#b85c38", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center", transition: "background 0.2s, border-color 0.2s" }}
            onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.background = "rgba(184,92,56,0.3)"; (e.currentTarget as HTMLButtonElement).style.borderColor = "rgba(184,92,56,0.7)"; }}
            onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.background = "rgba(184,92,56,0.15)"; (e.currentTarget as HTMLButtonElement).style.borderColor = "rgba(184,92,56,0.4)"; }}
            title="Información sobre la aparición de cartas"
            aria-label="Información sobre la aparición de cartas"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
        </div>

        {cargandoActual ? (
          <p className="text-stone-500 animate-pulse">Cargando catálogo...</p>
        ) : (
          <div className="space-y-8 animate-in fade-in duration-300">

            {/* Barra de Progreso de Maestría */}
            <div className="glass-card" style={{ padding: "16px 20px", marginBottom: 24 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <h4 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 14, fontWeight: 700, color: "#f0ebe1", textTransform: "uppercase", letterSpacing: "0.15em" }}>Maestría</h4>
                  <span style={{ color: "rgba(196,181,160,0.3)" }}>|</span>
                  <p style={{ color: "#8a9bb0", fontSize: 13 }}>{tituloMaestria}</p>
                </div>
                <div style={{ textAlign: "right", fontSize: 14 }}>
                  <span style={{ fontWeight: 800, color: "#00c8ff" }}>{cartasDesbloqueadasOriginal.length}</span>
                  <span style={{ color: "rgba(138,155,176,0.6)", fontWeight: 600, fontSize: 11 }}> / {cartasActuales.length}</span>
                </div>
              </div>
              <div style={{ width: "100%", background: "rgba(10,21,32,0.8)", borderRadius: 8, height: 6, overflow: "hidden", border: "1px solid rgba(0,200,255,0.15)" }}>
                <div
                  style={{ height: "100%", background: "linear-gradient(90deg, #00c8ff 0%, #0077ff 100%)", borderRadius: 8, transition: "width 1s ease-in-out", position: "relative", width: `${porcentajeDesbloqueado}%` }}
                >
                  <div style={{ position: "absolute", inset: 0, background: "rgba(255,255,255,0.2)", animation: "shimmer 2s infinite" }}></div>
                </div>
              </div>
            </div>

            {/* Buscador y Filtros */}
            <div className="glass-card" style={{ padding: 16, display: "flex", flexDirection: "column", gap: 16 }}>
              <div style={{ position: "relative", width: "100%" }}>
                <svg xmlns="http://www.w3.org/2000/svg" style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "#8a9bb0" }} width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  type="text"
                  placeholder="Buscar carta..."
                  value={filtroTexto}
                  onChange={(e) => setFiltroTexto(e.target.value)}
                  className="input-oni w-full"
                  style={{ paddingLeft: 36 }}
                />
              </div>

              <div style={{ display: "flex", background: "rgba(10,21,32,0.8)", border: "1px solid rgba(196,181,160,0.1)", padding: 4, borderRadius: 8 }}>
                <button onClick={() => setFiltroEstado("todas")} className={`btn-oni-${filtroEstado === 'todas' ? 'primary' : 'ghost'}`} style={{ flex: 1, padding: "8px 0", fontSize: 11, fontFamily: "var(--font-rajdhani), sans-serif" }}>
                  Todas
                </button>
                <button onClick={() => setFiltroEstado("desbloqueadas")} className={`btn-oni-${filtroEstado === 'desbloqueadas' ? 'primary' : 'ghost'}`} style={{ flex: 1, padding: "8px 0", fontSize: 11, fontFamily: "var(--font-rajdhani), sans-serif" }}>
                  Desbloqueadas
                </button>
                <button onClick={() => setFiltroEstado("bloqueadas")} className={`btn-oni-${filtroEstado === 'bloqueadas' ? 'primary' : 'ghost'}`} style={{ flex: 1, padding: "8px 0", fontSize: 11, fontFamily: "var(--font-rajdhani), sans-serif" }}>
                  Bloqueadas
                </button>
              </div>
            </div>

            {/* Cartas Desbloqueadas */}
            {(filtroEstado === "todas" || filtroEstado === "desbloqueadas") && (
              <div>
                <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 16, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 16, borderBottom: "1px solid rgba(196,181,160,0.1)", paddingBottom: 8, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span>Disponibles</span>
                  <span style={{ background: "rgba(0,200,255,0.1)", color: "#00c8ff", padding: "2px 8px", borderRadius: 4, fontSize: 12 }}>{desbloqueadas.length}</span>
                </h3>
                {desbloqueadas.length === 0 ? (
                  <p style={{ color: "#8a9bb0", fontSize: 14 }}>No hay cartas que coincidan con la búsqueda.</p>
                ) : (
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: 16 }}>
                    {desbloqueadas.map((c) => {
                      if (tabCartas === "poderes") {
                        return (
                          <div key={c.nombre} style={{ position: "relative", cursor: "pointer" }} onClick={() => setCartaAmpliada(c)} className="group">
                            <div style={{ position: "absolute", inset: -4, background: "linear-gradient(to right, rgba(0,200,255,0.4), rgba(0,119,255,0.4))", borderRadius: 16, filter: "blur(8px)", opacity: 0, transition: "opacity 0.3s" }} className="group-hover:opacity-100"></div>
                            <div style={{ position: "relative", height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                              <CartaAccionFicha nombre={c.nombre} descripcion={getDescripcionCartaAccion(c.descripcion ?? "")} variante="mano" className="h-full pointer-events-none" />
                            </div>
                            <div style={{ position: "absolute", top: 8, right: 8, background: "rgba(10,21,32,0.8)", backdropFilter: "blur(4px)", border: "1px solid rgba(0,200,255,0.3)", color: "#00c8ff", fontSize: 10, fontWeight: 700, padding: "4px 8px", borderRadius: 12, display: "flex", alignItems: "center", gap: 4, zIndex: 20 }}>
                              {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} style={{ objectFit: "contain" }} />
                            </div>
                          </div>
                        );
                      }

                      const cartaDef = TODAS_LAS_CARTAS.find(cd => cd.nombre === c.nombre);
                      return (
                        <button
                          key={c.nombre}
                          type="button"
                          onClick={() => setCartaAmpliada(c)}
                          className="glass-card flex flex-col items-center cursor-pointer text-left relative overflow-hidden group h-full"
                          style={{ padding: 16, transition: "transform 0.2s", display: "flex", flexDirection: "column" }}
                          onMouseEnter={e => e.currentTarget.style.transform = "scale(1.03)"}
                          onMouseLeave={e => e.currentTarget.style.transform = "scale(1)"}
                        >
                          <div style={{ position: "absolute", top: 0, left: 0, width: 4, height: 0, background: "#00c8ff", transition: "height 0.3s" }} className="group-hover:h-full"></div>
                          <p style={{ fontSize: 13, fontWeight: 700, color: "#f0ebe1", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 12, display: "flex", alignItems: "center", justifyContent: "space-between", width: "100%" }}>
                            <span>{c.nombre}</span>
                            <span style={{ fontSize: 10, color: "#00c8ff", fontWeight: 600, background: "rgba(0,200,255,0.1)", padding: "2px 6px", borderRadius: 4, display: "flex", alignItems: "center", gap: 4 }}>
                              {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} style={{ objectFit: "contain" }} />
                            </span>
                          </p>
                          <div style={{ background: "rgba(10,21,32,0.6)", borderRadius: 8, padding: 12, width: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 16, flex: 1, border: "1px solid rgba(196,181,160,0.05)" }}>
                            <Image src={getImagenCarta(c.nombre)} alt={c.nombre} width={68} height={68} style={{ objectFit: "contain" }} />
                            {cartaDef && (
                              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", paddingTop: 12, borderTop: "1px solid rgba(196,181,160,0.1)", width: "100%" }}>
                                <MiniGrid carta={cartaDef} size={6} colorDots="#00c8ff" />
                              </div>
                            )}
                          </div>
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>
            )}

            {/* Cartas Bloqueadas */}
            {(filtroEstado === "todas" || filtroEstado === "bloqueadas") && (
              <div>
                <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 16, fontWeight: 700, color: "#8a9bb0", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 16, borderBottom: "1px solid rgba(196,181,160,0.1)", paddingBottom: 8, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span>Bloqueadas</span>
                  <span style={{ background: "rgba(196,181,160,0.1)", color: "#c4b5a0", padding: "2px 8px", borderRadius: 4, fontSize: 12 }}>{bloqueadas.length}</span>
                </h3>
                {bloqueadas.length === 0 ? (
                  <p style={{ color: "#8a9bb0", fontSize: 14 }}>No hay cartas bloqueadas.</p>
                ) : (
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: 16 }}>
                    {bloqueadas.map((c) => {
                      if (tabCartas === "poderes") {
                        return (
                          <div key={c.nombre} style={{ position: "relative", cursor: "pointer", opacity: 0.6, filter: "grayscale(100%)", transition: "all 0.3s" }} onMouseEnter={e => { e.currentTarget.style.opacity = "1"; e.currentTarget.style.filter = "grayscale(0%)"; }} onMouseLeave={e => { e.currentTarget.style.opacity = "0.6"; e.currentTarget.style.filter = "grayscale(100%)"; }} onClick={() => setCartaAmpliada(c)}>
                            <div style={{ position: "relative", height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                              <CartaAccionFicha nombre={c.nombre} descripcion={getDescripcionCartaAccion(c.descripcion ?? "")} variante="mano" className="h-full pointer-events-none" />
                              <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", zIndex: 10, background: "rgba(10,21,32,0.5)", borderRadius: 12 }} className="group-hover:bg-transparent transition-colors">
                                <span style={{ fontSize: 32, filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.8))" }} aria-label="Bloqueada">🔒</span>
                              </div>
                            </div>
                            <div style={{ position: "absolute", top: 8, right: 8, background: "rgba(10,21,32,0.8)", backdropFilter: "blur(4px)", border: "1px solid rgba(196,181,160,0.3)", color: "#c4b5a0", fontSize: 10, fontWeight: 700, padding: "4px 8px", borderRadius: 12, display: "flex", alignItems: "center", gap: 4, zIndex: 20 }}>
                              {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} style={{ objectFit: "contain", opacity: 0.5 }} />
                            </div>
                          </div>
                        );
                      }

                      const cartaDef = TODAS_LAS_CARTAS.find(cd => cd.nombre === c.nombre);
                      return (
                        <button
                          key={c.nombre}
                          type="button"
                          onClick={() => setCartaAmpliada(c)}
                          className="glass-card flex flex-col items-center cursor-pointer text-left relative overflow-hidden group h-full"
                          style={{ padding: 16, opacity: 0.6, filter: "grayscale(100%)", transition: "all 0.3s", display: "flex", flexDirection: "column" }}
                          onMouseEnter={e => { e.currentTarget.style.transform = "scale(1.03)"; e.currentTarget.style.opacity = "1"; e.currentTarget.style.filter = "grayscale(0%)"; }}
                          onMouseLeave={e => { e.currentTarget.style.transform = "scale(1)"; e.currentTarget.style.opacity = "0.6"; e.currentTarget.style.filter = "grayscale(100%)"; }}
                        >
                          <div style={{ position: "absolute", top: 0, left: 0, width: 4, height: 0, background: "#c4b5a0", transition: "height 0.3s" }} className="group-hover:h-full z-20"></div>
                          <p style={{ fontSize: 13, fontWeight: 700, color: "#c4b5a0", textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 12, display: "flex", alignItems: "center", justifyContent: "space-between", width: "100%" }}>
                            <span>{c.nombre}</span>
                            <span style={{ fontSize: 10, color: "#8a9bb0", fontWeight: 600, background: "rgba(196,181,160,0.1)", padding: "2px 6px", borderRadius: 4, display: "flex", alignItems: "center", gap: 4 }}>
                              {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} style={{ objectFit: "contain", opacity: 0.6 }} />
                            </span>
                          </p>
                          <div style={{ background: "rgba(10,21,32,0.4)", borderRadius: 8, padding: 12, width: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 16, flex: 1, border: "1px solid rgba(196,181,160,0.05)", position: "relative" }}>
                            <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", zIndex: 10, background: "rgba(10,21,32,0.5)" }} className="group-hover:bg-transparent">
                               <span style={{ fontSize: 32, filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.8))" }} aria-label="Bloqueada">🔒</span>
                            </div>
                            <Image src={getImagenCarta(c.nombre)} alt={c.nombre} width={68} height={68} style={{ objectFit: "contain", opacity: 0.5 }} />
                            {cartaDef && (
                              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", paddingTop: 12, borderTop: "1px solid rgba(196,181,160,0.1)", width: "100%", opacity: 0.5 }}>
                                <MiniGrid carta={cartaDef} size={6} colorDots="#8a9bb0" />
                              </div>
                            )}
                          </div>
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>
            )}

          </div>
        )}
      </>
      {/* Modal Información Movimientos */}
      {mostrarInfoMovimientos && (
        <div style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(5,13,21,0.88)", backdropFilter: "blur(8px)", padding: 16 }}>
          <div className="glass-card" style={{ maxWidth: 500, width: "100%", padding: "32px", position: "relative" }}>
            <button onClick={() => setMostrarInfoMovimientos(false)} style={{ position: "absolute", top: 16, right: 16, background: "none", border: "none", color: "#8a9bb0", cursor: "pointer" }}>
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 24 }}>
              <div style={{ background: "rgba(184,92,56,0.1)", color: "#b85c38", padding: 8, borderRadius: 8 }}>
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 22, fontWeight: 800, color: "#f0ebe1", textTransform: "uppercase", letterSpacing: "0.08em" }}>
                El Destino y los Maestros
              </h3>
            </div>

            <div style={{ background: "rgba(10,21,32,0.6)", padding: 24, borderRadius: 16, border: "1px solid rgba(196,181,160,0.05)", display: "flex", flexDirection: "column", gap: 16, color: "rgba(196,181,160,0.8)", fontSize: 15, lineHeight: 1.6 }}>
              <p>
                {tabCartas === "poderes"
                  ? <>En el templo de Onitama, cada maestro empuña <strong style={{ color: "#f0ebe1" }}>dos poderes</strong> únicos que desafían las normas, pero las leyes ancestrales imponen una regla sagrada: <strong style={{ color: "#f0ebe1" }}>jamás recibirás un poder que aún no hayas logrado dominar en tu camino.</strong></>
                  : <>En el templo de Onitama, el azar reparte cinco cartas entre ambos contendientes, pero las leyes de los maestros imponen una restricción sagrada: <strong style={{ color: "#f0ebe1" }}>jamás recibirás una carta que aún no hayas logrado desbloquear en tu camino.</strong></>
                }
              </p>
              <div style={{ background: "rgba(16,185,129,0.1)", border: "1px solid rgba(16,185,129,0.3)", borderRadius: 12, padding: 16, marginTop: 8 }}>
                <h4 style={{ fontWeight: 700, color: "#10b981", textTransform: "uppercase", letterSpacing: "0.1em", fontSize: 14, display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
                  La Regla del Oponente
                </h4>
                <p style={{ color: "rgba(16,185,129,0.8)", fontSize: 14, lineHeight: 1.5 }}>
                  {tabCartas === "poderes"
                    ? <>Los poderes disponibles en una batalla están limitados por el maestro con menor rango. Puesto que <strong style={{ color: "#10b981" }}>ambos</strong> contendientes deben poseer el poder para que este pueda formar parte del nivel de la partida, <strong style={{ color: "#10b981" }}>aquellos con mayores requisitos de Katanas serán más raros de ver</strong>, requiriendo que te enfrentes a oponentes igual de experimentados.</>
                    : <>Las cartas disponibles en una batalla están limitadas por el maestro con menor rango. Puesto que <strong style={{ color: "#10b981" }}>ambos</strong> contendientes deben poseer la carta para que esta pueda formar parte del reparto, <strong style={{ color: "#10b981" }}>aquellas con mayores requisitos de Katanas serán más raras de ver</strong>, requiriendo que te enfrentes a oponentes igual de experimentados.</>
                  }
                </p>
              </div>
            </div>

            <div style={{ marginTop: 32, display: "flex", justifyContent: "flex-end" }}>
              <button
                type="button"
                onClick={() => setMostrarInfoMovimientos(false)}
                className="btn-oni-primary"
                style={{ fontFamily: "var(--font-rajdhani), sans-serif", padding: "12px 24px" }}
              >
                Comprendido
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Carta Ampliada */}
      {cartaAmpliada && (
        <div style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(5,13,21,0.88)", backdropFilter: "blur(8px)", padding: 16 }}>
          <div className="glass-card" style={{ maxWidth: 500, width: "100%", padding: "32px", position: "relative", display: "flex", flexDirection: "column", alignItems: "center" }}>
            <button onClick={() => setCartaAmpliada(null)} style={{ position: "absolute", top: 16, right: 16, background: "none", border: "none", color: "#8a9bb0", cursor: "pointer", fontSize: 24, lineHeight: 1 }}>
              ×
            </button>
            <h3 style={{ fontFamily: "var(--font-rajdhani), sans-serif", fontSize: 32, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 8, marginTop: 16 }}>
              {cartaAmpliada.nombre}
            </h3>
            <p style={{ color: "rgba(196,181,160,0.6)", fontSize: 14, fontStyle: "italic", textAlign: "center", marginBottom: 24, padding: "0 16px" }}>
              "{FRASES_EPICAS[cartaAmpliada.nombre] ?? "Una carta misteriosa que esconde un poder oculto."}"
            </p>

            <div style={{ width: "100%", background: "rgba(10,21,32,0.6)", borderRadius: 16, padding: 16, display: "flex", flexDirection: "column", alignItems: "center", gap: 24, border: "1px solid rgba(196,181,160,0.05)", marginBottom: 24 }}>
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 24, width: "100%" }}>
                {tabCartas === "poderes" ? (
                  <div style={{ width: "100%", maxWidth: 160, borderRadius: 16, overflow: "hidden", border: "2px solid rgba(0,200,255,0.3)", boxShadow: "0 0 20px rgba(0,200,255,0.15)" }}>
                    <CartaAccionFicha nombre={cartaAmpliada.nombre} descripcion={getDescripcionCartaAccion(cartaAmpliada.descripcion ?? "")} variante="elegir" className="w-full min-h-[220px] pointer-events-none" />
                  </div>
                ) : (
                  <>
                    <Image src={getImagenCarta(cartaAmpliada.nombre)} alt={cartaAmpliada.nombre} width={120} height={120} style={{ objectFit: "contain", filter: "drop-shadow(0 0 20px rgba(0,200,255,0.2))" }} />
                    {TODAS_LAS_CARTAS.find(cd => cd.nombre === cartaAmpliada.nombre) && (
                      <div style={{ background: "rgba(10,21,32,0.8)", padding: 12, borderRadius: 12, border: "1px solid rgba(0,200,255,0.15)" }}>
                        <MiniGrid carta={TODAS_LAS_CARTAS.find(cd => cd.nombre === cartaAmpliada.nombre)!} size={8} colorDots="#00c8ff" />
                      </div>
                    )}
                  </>
                )}
              </div>

              {/* Estadísticas / Lore Generado */}
              {ENFOQUES_CARTAS[cartaAmpliada.nombre] && (
                <div style={{ display: "flex", flexWrap: "wrap", gap: 12, justifyContent: "center", width: "100%" }}>
                  <div style={{ background: "rgba(196,181,160,0.1)", padding: "8px 16px", border: "1px solid rgba(196,181,160,0.2)", borderRadius: 8, display: "flex", alignItems: "center", gap: 8, fontSize: 12, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em" }}>
                    <span style={{ color: "#8a9bb0" }}>Enfoque:</span>
                    <span style={{ color: "#f0ebe1" }}>{ENFOQUES_CARTAS[cartaAmpliada.nombre].enfoque}</span>
                    <span>{ENFOQUES_CARTAS[cartaAmpliada.nombre].icon}</span>
                  </div>
                  <div style={{ background: "rgba(196,181,160,0.1)", padding: "8px 16px", border: "1px solid rgba(196,181,160,0.2)", borderRadius: 8, display: "flex", alignItems: "center", gap: 8, fontSize: 12, fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.1em" }}>
                    <span style={{ color: "#8a9bb0" }}>Alcance:</span>
                    <span style={{ color: "#f0ebe1" }}>{ENFOQUES_CARTAS[cartaAmpliada.nombre].alcance}</span>
                  </div>
                </div>
              )}
            </div>

            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "100%", fontSize: 12, textTransform: "uppercase", letterSpacing: "0.1em", fontWeight: 700 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8, background: "rgba(10,21,32,0.8)", padding: "8px 12px", borderRadius: 8, border: "1px solid rgba(196,181,160,0.2)" }}>
                <span style={{ color: "#8a9bb0" }}>Desbloqueo:</span>
                <Image src="/katanas.png" alt="Katanas" width={14} height={14} />
                <span style={{ color: "#f0ebe1" }}>{cartaAmpliada.puntos_necesarios.toLocaleString()}</span>
              </div>

              {jugador.puntos >= cartaAmpliada.puntos_necesarios ? (
                <div style={{ display: "flex", alignItems: "center", gap: 6, color: "#10b981", background: "rgba(16,185,129,0.1)", padding: "8px 12px", borderRadius: 8, border: "1px solid rgba(16,185,129,0.3)" }}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                  Disponible
                </div>
              ) : (
                <div style={{ display: "flex", alignItems: "center", gap: 6, color: "#8a9bb0", background: "rgba(196,181,160,0.1)", padding: "8px 12px", borderRadius: 8, border: "1px solid rgba(196,181,160,0.2)" }}>
                  <span style={{ fontSize: 14 }}>🔒</span>
                  Bloqueada
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
