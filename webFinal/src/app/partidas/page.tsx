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

import { useState, useEffect, useCallback, useRef } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { obtenerJugadorActivo, guardarSesion, cerrarSesion, type DatosSesion } from "@/lib/sesion";
import { obtenerPerfil } from "@/api/auth";
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
import { AvatarCircle } from "@/lib/avatar";

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
    idNotificacion: number | null;
  } | null>(null);
  const [mensajePrivada, setMensajePrivada] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);
  const [tiempoEsperaPrivada, setTiempoEsperaPrivada] = useState(120);
  const [tiempoEsperaReanudar, setTiempoEsperaReanudar] = useState(120);

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
      .catch(() => {})
      .finally(() => setCargandoCartas(false));

    setCargandoCartasAccion(true);
    obtenerCartasAccion()
      .then((res) => {
        setCartasAccion(res.cartas);
      })
      .catch(() => {})
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
      const esReanudacion = !!reanudarEnCursoRef.current;
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
            className={`w-11 h-11 shrink-0 rounded-full border-2 flex items-center justify-center overflow-hidden transition-colors ${panelActivo === "cuenta"
                ? "bg-white/20 border-white/60"
                : "bg-[#2a4a6a] border-white/30 hover:bg-white/10"
              }`}
            title="Mi cuenta"
            aria-label="Abrir Mi cuenta"
          >
            <AvatarCircle nombre={jugador.nombre} avatarId={jugador.avatar_id} sizeClass="w-full h-full" textClass="text-sm" />
          </button>
          <div className="flex items-center gap-2">
            <Image src="/katanas.png" alt="Katanas" width={22} height={22} className="h-5 w-auto shrink-0" />
            <span className="text-white font-semibold text-sm tabular-nums">
              {mounted ? jugador.puntos.toLocaleString() : "---"}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Image src="/core.png" alt="Cores" width={22} height={22} className="h-5 w-auto shrink-0" />
            <span className="text-white font-semibold text-sm tabular-nums">
              {mounted ? jugador.cores.toLocaleString() : "---"}
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
            <PanelMisCartas
              jugador={jugador}
              cartas={cartas}
              cargando={cargandoCartas}
              cartasAccion={cartasAccion}
              cargandoAccion={cargandoCartasAccion}
            />
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
                className={`px-5 py-3 text-sm font-semibold uppercase tracking-wider ${tabPartidaPrivada === "crear"
                    ? "border-b-2 border-[#1a2d4a] text-[#1a2d4a]"
                    : "text-stone-500 hover:text-stone-700"
                  }`}
              >
                Crear nueva partida
              </button>
              <button
                type="button"
                onClick={() => setTabPartidaPrivada("reanudar")}
                className={`px-5 py-3 text-sm font-semibold uppercase tracking-wider ${tabPartidaPrivada === "reanudar"
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
                        <AvatarCircle nombre={amigo.nombre} avatarId={amigo.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" />
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
              <div className="mt-5">
                {amigos.length === 0 ? (
                  <p className="text-stone-500 text-sm">No tienes amigos disponibles.</p>
                ) : !amigoSeleccionadoReanudar ? (
                  <>
                    <p className="text-stone-500 text-sm mb-3">Selecciona un amigo para ver sus partidas pausadas:</p>
                    <ul className="space-y-2 max-h-[320px] overflow-y-auto pr-1">
                      {amigos.map((amigo) => (
                        <li key={amigo.nombre} className="flex items-center gap-3 bg-stone-50 rounded-xl px-4 py-3">
                          <AvatarCircle nombre={amigo.nombre} avatarId={amigo.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" />
                          <div className="flex-1">
                            <p className="font-semibold text-stone-800">{amigo.nombre}</p>
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
                  <span className="font-semibold">
                    {skins.find((s) => s.skin_id === confirmacionSkin.skinId)?.precio ?? getSkinPrecio(confirmacionSkin.skinId)}
                  </span>
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
          <div className="w-full max-w-lg bg-[#1a2d4a] rounded-2xl border border-white/10 p-8 text-white text-center shadow-2xl flex flex-col items-center gap-5">
            <svg className="animate-spin h-10 w-10 text-amber-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-80" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
            </svg>
            <h3 className="text-2xl font-bold uppercase tracking-widest">Esperando respuesta</h3>
            <p className="text-white/70">
              Has invitado a <span className="font-semibold">@{invitacionPrivadaEnCurso.destinatario}</span> a una partida privada.
            </p>
            <p className="font-mono text-4xl text-yellow-300">
              {String(Math.floor(tiempoEsperaPrivada / 60)).padStart(2, "0")}:
              {String(tiempoEsperaPrivada % 60).padStart(2, "0")}
            </p>
            <button
              type="button"
              onClick={() => handleCancelarNotificacion(invitacionPrivadaEnCurso.idNotificacion)}
              className="px-5 py-2 rounded-xl font-bold uppercase tracking-widest text-sm border border-red-500/50 text-red-400 hover:bg-red-500/20 transition-colors"
            >
              Cancelar solicitud
            </button>
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
          <AvatarCircle nombre={jugador.nombre} avatarId={jugador.avatar_id} sizeClass="w-16 h-16 shrink-0" textClass="text-2xl" />
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
                    className={`flex items-center gap-3 bg-white rounded-xl px-4 py-3 shadow-sm border ${amigoSeleccionado?.nombre === amigo.nombre
                        ? "border-[#1a2d4a]"
                        : "border-transparent"
                      }`}
                  >
                    <button
                      type="button"
                      className="flex items-center gap-3 flex-1 text-left"
                      onClick={() => onSeleccionarAmigo(amigo)}
                    >
                      <AvatarCircle nombre={amigo.nombre} avatarId={amigo.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" />
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

          {!buscando && textoBusqueda.trim().length >= 1 && resultados.length === 0 && (
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
                    <AvatarCircle nombre={j.nombre} avatarId={j.avatar_id} sizeClass="w-9 h-9" textClass="text-sm" bgClass="bg-[#7b8fa8]" />
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
                <AvatarCircle
                  nombre={amigoSeleccionado.nombre}
                  avatarId={amigoSeleccionado.avatar_id}
                  sizeClass="w-28 h-28"
                  textClass="text-3xl text-stone-500"
                  bgClass="bg-stone-200"
                />
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
              <AvatarCircle nombre={notif.remitente} avatarId={notif.avatar_id} sizeClass="w-10 h-10 shrink-0" textClass="text-sm" />

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
    <div className="max-w-5xl mx-auto px-6 py-8">
      <div className="flex items-end justify-between mb-2">
        <h2 className="text-xl font-bold text-stone-800 uppercase tracking-widest">Mis cartas</h2>
      </div>

      {/* Tabs para seleccionar el tipo de carta */}
      <div className="flex gap-6 border-b border-stone-200 mb-8">
        <button
          onClick={() => setTabCartas("movimientos")}
          className={`pb-3 uppercase tracking-wider text-sm font-bold transition-all relative ${
            tabCartas === "movimientos"
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
          className={`pb-3 uppercase tracking-wider text-sm font-bold transition-all relative flex items-center gap-2 ${
            tabCartas === "poderes"
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
          <div className="flex items-start md:items-center justify-between mb-6 gap-4">
            <p className="text-stone-500 text-sm flex flex-wrap items-center gap-2">
              <span>{tabCartas === "poderes" ? "Poderes que pueden" : "Cartas que pueden"} aparecer en tus partidas según tu ELO actual (</span>
              <Image src="/katanas.png" alt="Katanas" width={16} height={16} className="object-contain" />
              <span className="font-semibold">{jugador.puntos.toLocaleString()}</span>
              <span>):</span>
            </p>
            <button 
              onClick={() => setMostrarInfoMovimientos(true)}
              className="text-stone-400 hover:text-stone-700 transition-colors bg-white border border-stone-200 rounded-full p-1.5 shadow-sm hover:shadow shrink-0"
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
              <div className="bg-white px-4 py-3 rounded-xl shadow-sm border border-stone-200">
                <div className="flex justify-between items-center mb-2">
                  <div className="flex items-center gap-2">
                    <h4 className="text-stone-800 font-bold uppercase tracking-widest text-xs">Maestría</h4>
                    <span className="text-stone-300 text-[10px] hidden sm:inline">|</span>
                    <p className="text-stone-500 text-xs">{tituloMaestria}</p>
                  </div>
                  <div className="text-right text-sm">
                    <span className="font-extrabold text-emerald-600">{cartasDesbloqueadasOriginal.length}</span>
                    <span className="text-stone-400 font-semibold text-[10px]"> / {cartasActuales.length}</span>
                  </div>
                </div>
                <div className="w-full bg-stone-100 rounded-full h-1.5 overflow-hidden shadow-inner">
                  <div 
                    className="h-full bg-gradient-to-r from-emerald-400 to-emerald-600 rounded-full transition-all duration-1000 ease-in-out relative"
                    style={{ width: `${porcentajeDesbloqueado}%` }}
                  >
                    <div className="absolute inset-0 bg-white/20 w-full animate-[shimmer_2s_infinite]"></div>
                  </div>
                </div>
              </div>

              {/* Buscador y Filtros */}
              <div className="bg-stone-50 p-4 rounded-xl border border-stone-200 flex flex-col md:flex-row gap-4 justify-between items-center">
                <div className="relative w-full md:w-64">
                  <svg xmlns="http://www.w3.org/2000/svg" className="absolute left-3 top-1/2 -translate-y-1/2 text-stone-400" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <input 
                    type="text" 
                    placeholder="Buscar carta..." 
                    value={filtroTexto}
                    onChange={(e) => setFiltroTexto(e.target.value)}
                    className="w-full pl-9 pr-4 py-2 border border-stone-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 transition-all font-semibold text-stone-700" 
                  />
                </div>
                
                <div className="flex bg-white rounded-lg border border-stone-300 p-1 shadow-sm w-full md:w-auto">
                  <button 
                    onClick={() => setFiltroEstado("todas")}
                    className={`flex-1 md:flex-none px-4 py-1.5 text-xs font-bold uppercase tracking-wider rounded-md transition-colors ${filtroEstado === 'todas' ? 'bg-stone-800 text-white shadow' : 'text-stone-500 hover:text-stone-800 hover:bg-stone-100'}`}
                  >
                    Todas
                  </button>
                  <button 
                    onClick={() => setFiltroEstado("desbloqueadas")}
                    className={`flex-1 md:flex-none px-4 py-1.5 text-xs font-bold uppercase tracking-wider rounded-md transition-colors ${filtroEstado === 'desbloqueadas' ? 'bg-emerald-600 text-white shadow' : 'text-stone-500 hover:text-emerald-700 hover:bg-emerald-50'}`}
                  >
                    Desbloqueadas
                  </button>
                  <button 
                    onClick={() => setFiltroEstado("bloqueadas")}
                    className={`flex-1 md:flex-none px-4 py-1.5 text-xs font-bold uppercase tracking-wider rounded-md transition-colors ${filtroEstado === 'bloqueadas' ? 'bg-stone-300 text-stone-800 shadow' : 'text-stone-500 hover:text-stone-800 hover:bg-stone-100'}`}
                  >
                    Bloqueadas
                  </button>
                </div>
              </div>
          
              {/* Cartas Desbloqueadas */}
              {(filtroEstado === "todas" || filtroEstado === "desbloqueadas") && (
              <div>
                <h3 className="text-lg font-bold text-stone-700 uppercase tracking-wider mb-4 border-b border-stone-300 pb-2 flex justify-between items-center">
                  <span>Disponibles</span>
                  <span className="bg-stone-200 text-stone-600 px-2 py-0.5 rounded text-xs">{desbloqueadas.length}</span>
                </h3>
                {desbloqueadas.length === 0 ? (
                  <p className="text-stone-500 text-sm">No hay cartas que coincidan con la búsqueda.</p>
                ) : (
                  <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                    {desbloqueadas.map((c) => {
                      if (tabCartas === "poderes") {
                        return (
                          <div key={c.nombre} className="relative group cursor-pointer" onClick={() => setCartaAmpliada(c)}>
                            <div className="absolute -inset-1 bg-gradient-to-r from-emerald-500 to-emerald-400 rounded-xl blur opacity-20 group-hover:opacity-40 transition duration-200"></div>
                            <div className="relative h-full flex flex-col justify-between">
                              <CartaAccionFicha 
                                nombre={c.nombre} 
                                descripcion={getDescripcionCartaAccion(c.descripcion ?? "")} 
                                variante="mano"
                                className="h-full pointer-events-none" 
                              />
                            </div>
                            <div className="absolute top-2 right-2 bg-emerald-100/90 backdrop-blur-sm text-emerald-800 text-[9px] font-bold px-2 py-1 rounded-full shadow-sm flex items-center gap-1 z-20">
                              {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} className="object-contain" />
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
                        className="rounded-2xl border border-emerald-200 bg-emerald-50/30 p-4 shadow-sm flex flex-col items-center hover:scale-[1.03] hover:shadow-md transition-all cursor-pointer text-left relative overflow-hidden group h-full"
                      >
                        <div className="absolute top-0 left-0 w-1 h-0 bg-emerald-500 transition-all duration-300 group-hover:h-full"></div>
                        <p className="text-sm font-bold text-stone-800 uppercase tracking-wider mb-3 flex items-center justify-between w-full">
                          <span>{c.nombre}</span>
                          <span className="text-[10px] text-emerald-700 font-semibold bg-emerald-100 px-1.5 py-0.5 rounded whitespace-nowrap flex items-center gap-1">
                            Desbloqueo: {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} className="object-contain" />
                          </span>
                        </p>
                        <div className="bg-white rounded-lg p-3 shadow-inner w-full flex flex-col md:flex-row items-center justify-center gap-4 flex-1">
                          <Image 
                            src={getImagenCarta(c.nombre)} 
                            alt={c.nombre} 
                            width={68} 
                            height={68} 
                            className="object-contain" 
                          />
                          {cartaDef && (
                            <div className="flex flex-col items-center md:border-l md:border-stone-200 md:pl-4">
                              <MiniGrid carta={cartaDef} size={6} colorDots="#10b981" />
                            </div>
                          )}
                        </div>
                      </button>
                    )})}
                  </div>
                )}
              </div>
              )}

          {/* Cartas Bloqueadas */}
          {(filtroEstado === "todas" || filtroEstado === "bloqueadas") && (
            <div>
              <h3 className="text-lg font-bold text-stone-700 uppercase tracking-wider mb-4 border-b border-stone-300 pb-2 flex items-center justify-between">
                <span>Bloqueadas</span>
                <span className="bg-stone-200 text-stone-600 px-2 py-0.5 rounded text-xs">{bloqueadas.length}</span>
              </h3>
              {bloqueadas.length === 0 ? (
                <p className="text-stone-500 text-sm">No hay cartas bloqueadas.</p>
              ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                {bloqueadas.map((c) => {
                  if (tabCartas === "poderes") {
                    return (
                      <div key={c.nombre} className="relative group cursor-pointer opacity-80 hover:opacity-100 grayscale hover:grayscale-0 transition-all duration-300" onClick={() => setCartaAmpliada(c)}>
                        <div className="relative h-full flex flex-col justify-between">
                          <CartaAccionFicha 
                            nombre={c.nombre} 
                            descripcion={getDescripcionCartaAccion(c.descripcion ?? "")} 
                            variante="mano"
                            className="h-full pointer-events-none"
                          />
                          <div className="absolute inset-0 flex items-center justify-center z-10 bg-stone-900/40 rounded-xl group-hover:bg-transparent transition-colors">
                            <span className="text-4xl drop-shadow-md group-hover:scale-110 transition-transform" aria-label="Bloqueada">🔒</span>
                          </div>
                        </div>
                        <div className="absolute top-2 right-2 bg-stone-200/90 backdrop-blur-sm text-stone-600 text-[9px] font-bold px-2 py-1 rounded-full shadow-sm flex items-center gap-1 z-20">
                          {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} className="object-contain opacity-60" />
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
                    className="rounded-2xl border border-stone-200 bg-stone-100 p-4 shadow-sm flex flex-col items-center opacity-80 grayscale hover:opacity-100 hover:scale-[1.03] hover:shadow-md transition-all cursor-pointer text-left relative overflow-hidden group h-full"
                  >
                    <div className="absolute top-0 left-0 w-1 h-0 bg-stone-400 transition-all duration-300 group-hover:h-full z-20"></div>
                    <p className="text-sm font-bold text-stone-600 uppercase tracking-wider mb-3 flex items-center justify-between w-full">
                      <span>{c.nombre}</span>
                      <span className="text-[10px] text-stone-500 font-semibold bg-stone-200 px-1.5 py-0.5 rounded whitespace-nowrap flex items-center gap-1">
                        Desbloqueo: {c.puntos_necesarios.toLocaleString()} <Image src="/katanas.png" alt="Katanas" width={10} height={10} className="object-contain opacity-60" />
                      </span>
                    </p>
                    <div className="bg-stone-200 rounded-lg p-3 shadow-inner w-full flex flex-col md:flex-row items-center justify-center gap-4 relative overflow-hidden flex-1">
                      <div className="absolute inset-0 flex items-center justify-center z-10 bg-stone-200/40">
                        <span className="text-4xl drop-shadow-md group-hover:scale-110 transition-transform" aria-label="Bloqueada">🔒</span>
                      </div>
                      <Image 
                        src={getImagenCarta(c.nombre)} 
                        alt={c.nombre} 
                        width={68} 
                        height={68} 
                        className="object-contain opacity-50" 
                      />
                      {cartaDef && (
                        <div className="flex flex-col items-center md:border-l md:border-stone-300 md:pl-4 opacity-50">
                          <MiniGrid carta={cartaDef} size={6} colorDots="#64748b" />
                        </div>
                      )}
                    </div>
                  </button>
                )})}
              </div>
              )}
            </div>
          )}

            </div>
          )}
        </>
      {/* Modal Información Movimientos */}
      {mostrarInfoMovimientos && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-lg bg-stone-100 rounded-3xl shadow-2xl p-6 md:p-8 relative animate-in fade-in zoom-in duration-200 border border-stone-200">
            <button
              type="button"
              onClick={() => setMostrarInfoMovimientos(false)}
              className="absolute right-6 top-6 text-2xl leading-none text-stone-400 hover:text-stone-800 transition-colors focus:outline-none"
              aria-label="Cerrar"
            >
              ×
            </button>
            <div className="flex items-center gap-3 mb-6">
              <div className="p-2 bg-stone-800 rounded-lg text-white">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 className="text-2xl font-extrabold text-stone-800 uppercase tracking-widest">
                El Destino y los Maestros
              </h3>
            </div>
            
            <div className="space-y-4 text-base text-stone-600 leading-relaxed bg-white p-6 rounded-2xl shadow-sm border border-stone-200">
              <p>
                {tabCartas === "poderes" 
                  ? <>En el templo de Onitama, cada maestro empuña <strong>dos poderes</strong> únicos que desafían las normas, pero las leyes ancestrales imponen una regla sagrada: <strong>jamás recibirás un poder que aún no hayas logrado dominar en tu camino.</strong></>
                  : <>En el templo de Onitama, el azar reparte cinco cartas entre ambos contendientes, pero las leyes de los maestros imponen una restricción sagrada: <strong>jamás recibirás una carta que aún no hayas logrado desbloquear en tu camino.</strong></>
                }
              </p>
              <div className="bg-emerald-50 border-emerald-500 border rounded-xl p-4 mt-2">
                <h4 className="font-bold text-emerald-900 uppercase tracking-wider text-sm flex items-center gap-2 mb-2">
                  La Regla del Oponente
                </h4>
                <p className="text-emerald-800 text-sm">
                  {tabCartas === "poderes" 
                    ? <>Los poderes disponibles en una batalla están limitados por el maestro con menor rango. Puesto que <strong>ambos</strong> contendientes deben poseer el poder para que este pueda formar parte del nivel de la partida, <strong>aquellos con mayores requisitos de Katanas serán más raros de ver</strong>, requiriendo que te enfrentes a oponentes igual de experimentados.</>
                    : <>Las cartas disponibles en una batalla están limitadas por el maestro con menor rango. Puesto que <strong>ambos</strong> contendientes deben poseer la carta para que esta pueda formar parte del reparto, <strong>aquellas con mayores requisitos de Katanas serán más raras de ver</strong>, requiriendo que te enfrentes a oponentes igual de experimentados.</>
                  }
                </p>
              </div>
            </div>

            <div className="mt-8 flex justify-end">
              <button 
                type="button"
                onClick={() => setMostrarInfoMovimientos(false)}
                className="px-6 py-3 bg-stone-800 text-stone-100 font-bold uppercase tracking-widest text-sm rounded-xl hover:bg-stone-700 hover:scale-[1.02] transition-all shadow-md"
              >
                Comprendido
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Carta Ampliada */}
      {cartaAmpliada && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-sm sm:max-w-md md:max-w-lg bg-white rounded-3xl shadow-2xl p-6 md:p-8 relative flex flex-col items-center animate-in fade-in zoom-in duration-200">
            <button
              type="button"
              onClick={() => setCartaAmpliada(null)}
              className="absolute right-5 top-4 text-3xl leading-none text-stone-400 hover:text-stone-700 focus:outline-none"
              aria-label="Cerrar"
            >
              ×
            </button>
            <h3 className="text-3xl font-extrabold text-stone-800 uppercase tracking-wider mb-2 mt-4">
              {cartaAmpliada.nombre}
            </h3>
            <p className="text-sm text-stone-500 font-semibold mb-6 italic text-center px-4">
              "{FRASES_EPICAS[cartaAmpliada.nombre] ?? "Una carta misteriosa que esconde un poder oculto."}"
            </p>
            
            <div className="w-full bg-stone-100 rounded-2xl p-6 flex flex-col items-center justify-center gap-8 shadow-inner mb-6">
              <div className="flex flex-col md:flex-row items-center justify-center gap-8 w-full">
                {tabCartas === "poderes" ? (
                  <div className="w-full max-w-[200px] sm:max-w-xs shadow-2xl rounded-2xl overflow-hidden ring-4 ring-white">
                    <CartaAccionFicha 
                      nombre={cartaAmpliada.nombre} 
                      descripcion={getDescripcionCartaAccion(cartaAmpliada.descripcion ?? "")} 
                      variante="elegir" 
                      className="w-full min-h-[250px] sm:min-h-[300px] pointer-events-none" 
                    />
                  </div>
                ) : (
                  <>
                    <Image 
                      src={getImagenCarta(cartaAmpliada.nombre)} 
                      alt={cartaAmpliada.nombre} 
                      width={160} 
                      height={160} 
                      className="object-contain drop-shadow-2xl" 
                    />
                    {TODAS_LAS_CARTAS.find(cd => cd.nombre === cartaAmpliada.nombre) && (
                      <div className="bg-white p-4 rounded-xl shadow-md border border-stone-200">
                        <MiniGrid carta={TODAS_LAS_CARTAS.find(cd => cd.nombre === cartaAmpliada.nombre)!} size={10} colorDots="#1a2d4a" />
                      </div>
                    )}
                  </>
                )}
              </div>
              
              {/* Estadísticas / Lore Generado */}
              {ENFOQUES_CARTAS[cartaAmpliada.nombre] && (
                <div className="flex flex-wrap gap-3 w-full justify-center">
                  <div className="bg-white px-4 py-2 border border-stone-200 rounded-lg flex items-center gap-2 shadow-sm text-xs font-bold text-stone-600 uppercase tracking-widest">
                    <span>Enfoque:</span>
                    <span className="text-stone-800">{ENFOQUES_CARTAS[cartaAmpliada.nombre].enfoque}</span>
                    <span>{ENFOQUES_CARTAS[cartaAmpliada.nombre].icon}</span>
                  </div>
                  <div className="bg-white px-4 py-2 border border-stone-200 rounded-lg flex items-center gap-2 shadow-sm text-xs font-bold text-stone-600 uppercase tracking-widest">
                    <span>Alcance:</span>
                    <span className="text-stone-800">{ENFOQUES_CARTAS[cartaAmpliada.nombre].alcance}</span>
                  </div>
                </div>
              )}
            </div>

            <div className="text-xs text-stone-400 uppercase tracking-widest font-bold flex items-center justify-between w-full">
              <div className="flex items-center gap-2 bg-stone-100 px-3 py-2 rounded-lg border border-stone-200">
                <span className="text-stone-500">Desbloqueo:</span> 
                <Image src="/katanas.png" alt="Katanas" width={14} height={14} /> 
                <span className="text-stone-800">{cartaAmpliada.puntos_necesarios.toLocaleString()}</span>
              </div>
              
              {jugador.puntos >= cartaAmpliada.puntos_necesarios ? (
                <div className="flex items-center gap-1.5 text-emerald-600 bg-emerald-50 px-3 py-2 rounded-lg border border-emerald-200">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                  Disponible
                </div>
              ) : (
                <div className="flex items-center gap-1.5 text-stone-500 bg-stone-100 px-3 py-2 rounded-lg border border-stone-200">
                  <span className="text-sm">🔒</span>
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
