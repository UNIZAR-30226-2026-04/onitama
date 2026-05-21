import { guardarSesion, obtenerJugadorActivo } from "@/lib/sesion";
import {
  getSkinPrecio,
  normalizarSkinId,
  obtenerCatalogoSkins,
  type SkinId,
} from "@/lib/skins";
import * as WS from "./ws";

export interface SkinEstado {
  skin_id: SkinId;
  precio: number;
  owned: boolean;
  es_activa: boolean;
}

export interface RespuestaTiendaSkins {
  tipo: "TIENDA_SKINS";
  usuario: string;
  cores: number;
  skin_activa: SkinId;
  skins: SkinEstado[];
}

type CodigoCompra = "YA_COMPRADA" | "CORES_INSUFICIENTES" | "SKIN_NO_EXISTE" | "ERROR_BD";
type CodigoActivar = "NO_POSEIDA" | "SKIN_NO_EXISTE" | "ERROR_BD";

const CLAVE_MOCK = "skinsMockState";

function cargarMock(): { owned: SkinId[]; activa: SkinId } {
  if (typeof window === "undefined") return { owned: ["Skin0"], activa: "Skin0" };
  try {
    const raw = sessionStorage.getItem(CLAVE_MOCK);
    if (!raw) return { owned: ["Skin0"], activa: "Skin0" };
    const data = JSON.parse(raw) as { owned?: SkinId[]; activa?: SkinId };
    const owned = Array.from(new Set((data.owned ?? ["Skin0"]).map(normalizarSkinId)));
    if (!owned.includes("Skin0")) owned.unshift("Skin0");
    const activa = owned.includes(normalizarSkinId(data.activa)) ? normalizarSkinId(data.activa) : "Skin0";
    return { owned, activa };
  } catch {
    return { owned: ["Skin0"], activa: "Skin0" };
  }
}

function guardarMock(state: { owned: SkinId[]; activa: SkinId }): void {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(CLAVE_MOCK, JSON.stringify(state));
}

function mockTienda(usuario: string): RespuestaTiendaSkins {
  const sesion = obtenerJugadorActivo();
  const state = cargarMock();
  const skins: SkinEstado[] = obtenerCatalogoSkins().map((s) => ({
    skin_id: s.skin_id,
    precio: s.precio,
    owned: state.owned.includes(s.skin_id),
    es_activa: state.activa === s.skin_id,
  }));
  return {
    tipo: "TIENDA_SKINS",
    usuario,
    cores: sesion.cores,
    skin_activa: state.activa,
    skins,
  };
}

export async function obtenerTiendaSkins(usuario: string): Promise<RespuestaTiendaSkins> {
  if (!WS.usarServidor || !WS.estaConectado()) {
    return mockTienda(usuario);
  }

  return new Promise<RespuestaTiendaSkins>((resolve) => {
    const timeout = setTimeout(() => {
      unsubOk();
      resolve(mockTienda(usuario));
    }, 5000);

    const unsubOk = WS.suscribir("TIENDA_SKINS", (msg) => {
      clearTimeout(timeout);
      unsubOk();
      resolve(msg as unknown as RespuestaTiendaSkins);
    });

    WS.enviar({ tipo: "OBTENER_TIENDA_SKINS", usuario });
  });
}

export async function comprarSkin(usuario: string, skinId: SkinId): Promise<{ ok: true; cores: number } | { ok: false; codigo: CodigoCompra }> {
  const skin = normalizarSkinId(skinId);
  if (!WS.usarServidor || !WS.estaConectado()) {
    const sesion = obtenerJugadorActivo();
    const state = cargarMock();
    if (state.owned.includes(skin)) return { ok: false, codigo: "YA_COMPRADA" };
    const precio = getSkinPrecio(skin);
    if (sesion.cores < precio) return { ok: false, codigo: "CORES_INSUFICIENTES" };

    const nuevosCores = sesion.cores - precio;
    guardarMock({ owned: [...state.owned, skin], activa: state.activa });
    guardarSesion({ ...sesion, cores: nuevosCores });
    return { ok: true, cores: nuevosCores };
  }

  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      unsubOk();
      unsubErr();
      resolve({ ok: false, codigo: "ERROR_BD" });
    }, 6000);

    const unsubOk = WS.suscribir("COMPRA_SKIN_OK", (msg) => {
      clearTimeout(timeout);
      unsubOk();
      unsubErr();
      resolve({ ok: true, cores: Number(msg.cores ?? 0) });
    });
    const unsubErr = WS.suscribir("COMPRA_SKIN_ERROR", (msg) => {
      clearTimeout(timeout);
      unsubOk();
      unsubErr();
      resolve({ ok: false, codigo: String(msg.codigo ?? "ERROR_BD") as CodigoCompra });
    });

    WS.enviar({ tipo: "COMPRAR_SKIN", usuario, skin_id: skin });
  });
}

export async function activarSkin(usuario: string, skinId: SkinId): Promise<{ ok: true; skin_activa: SkinId } | { ok: false; codigo: CodigoActivar }> {
  const skin = normalizarSkinId(skinId);
  if (!WS.usarServidor || !WS.estaConectado()) {
    const state = cargarMock();
    if (!state.owned.includes(skin)) return { ok: false, codigo: "NO_POSEIDA" };
    guardarMock({ owned: state.owned, activa: skin });
    const sesion = obtenerJugadorActivo();
    guardarSesion({ ...sesion, skin_activa: skin });
    return { ok: true, skin_activa: skin };
  }

  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      unsubOk();
      unsubErr();
      resolve({ ok: false, codigo: "ERROR_BD" });
    }, 6000);

    const unsubOk = WS.suscribir("SKIN_ACTIVADA", (msg) => {
      clearTimeout(timeout);
      unsubOk();
      unsubErr();
      resolve({ ok: true, skin_activa: normalizarSkinId(msg.skin_activa) });
    });
    const unsubErr = WS.suscribir("ACTIVAR_SKIN_ERROR", (msg) => {
      clearTimeout(timeout);
      unsubOk();
      unsubErr();
      resolve({ ok: false, codigo: String(msg.codigo ?? "ERROR_BD") as CodigoActivar });
    });

    WS.enviar({ tipo: "ACTIVAR_SKIN", usuario, skin_id: skin });
  });
}
