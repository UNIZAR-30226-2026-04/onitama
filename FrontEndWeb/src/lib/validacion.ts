/**
 * Utilidades de validación para formularios.
 * Los requisitos de contraseña pueden modificarse en el futuro.
 */

/** Requisitos actuales: mínimo 8 caracteres, al menos 1 letra y 1 número */
export function validarContrasena(contrasena: string): boolean {
  if (contrasena.length < 8) return false;
  const tieneLetra = /[a-zA-Z]/.test(contrasena);
  const tieneNumero = /\d/.test(contrasena);
  return tieneLetra && tieneNumero;
}

/** Mensaje de ayuda para los requisitos de contraseña (se puede cambiar junto con validarContrasena) */
export const HINT_CONTRASENA =
  "Usa al menos 8 caracteres con letras y números";
