export type SkinId =
  | "Skin0"
  | "Skin1"
  | "Skin2"
  | "Skin3"
  | "Skin4"
  | "Skin5"
  | "Skin6";

export interface SkinCatalogo {
  skin_id: SkinId;
  nombre: string;
  precio: number;
}

export type BoardStyle = "default" | "ajedrez" | "clasico-futbol";

const CATALOGO: SkinCatalogo[] = [
  { skin_id: "Skin0", nombre: "Original", precio: 0 },
  { skin_id: "Skin1", nombre: "Ajedrez", precio: 45 },
  { skin_id: "Skin2", nombre: "El Clásico", precio: 45 },
  { skin_id: "Skin3", nombre: "Medieval", precio: 25 },
  { skin_id: "Skin4", nombre: "Minimalista", precio: 25 },
  { skin_id: "Skin5", nombre: "Pradera Solar", precio: 15 },
  { skin_id: "Skin6", nombre: "Hechizo de Calabaza", precio: 15 },
];

const VALIDAS = new Set<SkinId>(CATALOGO.map((s) => s.skin_id));

export function obtenerCatalogoSkins(): SkinCatalogo[] {
  return CATALOGO;
}

export function normalizarSkinId(valor: unknown): SkinId {
  if (typeof valor === "string" && VALIDAS.has(valor as SkinId)) {
    return valor as SkinId;
  }
  return "Skin0";
}

export function getSkinNombre(skinId: SkinId): string {
  return CATALOGO.find((s) => s.skin_id === skinId)?.nombre ?? "Original";
}

export function getSkinPrecio(skinId: SkinId): number {
  return CATALOGO.find((s) => s.skin_id === skinId)?.precio ?? 0;
}

export function getBoardStyle(skinId: SkinId): BoardStyle {
  if (skinId === "Skin1") return "ajedrez";
  if (skinId === "Skin2") return "clasico-futbol";
  return "default";
}

export function getEquipoNombre(skinId: SkinId, equipo: 1 | 2): string {
  switch (skinId) {
    case "Skin1":
      return equipo === 1 ? "Blancas" : "Negras";
    case "Skin2":
      return equipo === 1 ? "Azulgrana" : "Blanco";
    case "Skin5":
      return equipo === 1 ? "Verde" : "Amarillo";
    case "Skin6":
      return equipo === 1 ? "Morado" : "Naranja";
    default:
      return equipo === 1 ? "Azul" : "Rojo";
  }
}

export function getEquipoClaseTexto(skinId: SkinId, equipo: 1 | 2): string {
  switch (skinId) {
    case "Skin1":
      // Blancas → gris claro visible en fondos oscuros; Negras → gris neutro (no negro puro)
      return equipo === 1 ? "text-stone-200" : "text-zinc-300";
    case "Skin2":
      return equipo === 1 ? "text-blue-200" : "text-slate-100";
    case "Skin5":
      return equipo === 1 ? "text-emerald-300" : "text-yellow-300";
    case "Skin6":
      return equipo === 1 ? "text-violet-300" : "text-orange-300";
    default:
      return equipo === 1 ? "text-blue-300" : "text-red-300";
  }
}

export function getEquipoGlow(skinId: SkinId, equipo: 1 | 2): string {
  switch (skinId) {
    case "Skin1":
      return equipo === 1 ? "rgba(226,232,240,0.45)" : "rgba(15,23,42,0.45)";
    case "Skin2":
      // Azulgrana → azul royal; Blanco → blanco suave
      return equipo === 1 ? "rgba(30,58,138,0.45)" : "rgba(226,232,240,0.45)";
    case "Skin5":
      return equipo === 1 ? "rgba(16,185,129,0.45)" : "rgba(250,204,21,0.45)";
    case "Skin6":
      return equipo === 1 ? "rgba(168,85,247,0.45)" : "rgba(249,115,22,0.45)";
    default:
      return equipo === 1 ? "rgba(59,130,246,0.45)" : "rgba(239,68,68,0.45)";
  }
}

function getSufijo(skinId: SkinId): string {
  return skinId === "Skin0" ? "" : skinId;
}

export function getPiezaSrc(
  tipo: "peon" | "rey" | "templo",
  equipo: 1 | 2,
  skinId: SkinId
): string {
  const color = equipo === 1 ? "Azul" : "Rojo";
  const sufijo = getSufijo(skinId);

  if (tipo === "peon") {
    if (skinId === "Skin0" && equipo === 2) return "/peonRojo.PNG";
    return `/peon${color}${sufijo}.png`;
  }
  return `/${tipo}${color}${sufijo}.png`;
}

/** Color CSS principal del equipo para bordes, fondos y sombras temáticas. */
export function getEquipoColorBase(skinId: SkinId, equipo: 1 | 2): string {
  switch (skinId) {
    case "Skin1": return equipo === 1 ? "#e2e8f0" : "#1e293b";
    case "Skin2": return equipo === 1 ? "#1e3a8a" : "#f8fafc";
    case "Skin5": return equipo === 1 ? "#10b981" : "#facc15";
    case "Skin6": return equipo === 1 ? "#a855f7" : "#f97316";
    default:      return equipo === 1 ? "#3b82f6" : "#ef4444";
  }
}

/**
 * Devuelve true cuando el fondo del banner de ese equipo es suficientemente claro
 * como para necesitar texto oscuro. Solo aplica a Skin2-Blanco en la práctica,
 * ya que el resto de banners siguen siendo visualmente oscuros aunque tengan un tinte claro.
 */
export function equipoFondoEsClaro(skinId: SkinId, equipo: 1 | 2): boolean {
  return skinId === "Skin2" && equipo === 2;  // Solo Blanco
}

/** Fondo CSS del banner (soporta gradiente para Azulgrana). */
export function getEquipoBannerBg(skinId: SkinId, equipo: 1 | 2): string {
  if (skinId === "Skin2" && equipo === 1) {
    return "linear-gradient(to right, rgba(30,58,138,0.45) 50%, rgba(127,29,29,0.38) 50%)";
  }
  return getEquipoColorBase(skinId, equipo) + "33";
}

/** Box-shadow del banner (doble color para Azulgrana). */
export function getEquipoBannerSombra(skinId: SkinId, equipo: 1 | 2): string {
  if (skinId === "Skin2" && equipo === 1) {
    return "0 0 22px rgba(30,58,138,0.45), 0 0 14px rgba(127,29,29,0.35)";
  }
  return `0 0 20px ${getEquipoColorBase(skinId, equipo)}4D`;
}

export function getColorMovimiento(skinId: SkinId, equipo: 1 | 2): string {
  switch (skinId) {
    case "Skin1":
      // Blancas / Negras
      return equipo === 1 ? "#f8fafc" : "#0f172a";
    case "Skin2":
      // Azulgrana / Blanco
      return equipo === 1
        ? "linear-gradient(90deg, #1e3a8a 0%, #1e3a8a 50%, #7f1d1d 50%, #7f1d1d 100%)"
        : "#f8fafc";
    case "Skin5":
      // Verde / Amarillo
      return equipo === 1 ? "#10b981" : "#facc15";
    case "Skin6":
      // Morado / Naranja
      return equipo === 1 ? "#a855f7" : "#f97316";
    default:
      // Azul / Rojo
      return equipo === 1 ? "#3b82f6" : "#ef4444";
  }
}
