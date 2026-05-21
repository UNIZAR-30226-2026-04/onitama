import Image from "next/image";
import { useState, useEffect } from "react";

export function getAvatarSrc(avatarId?: string | null): string | null {
  if (!avatarId) return null;
  const limpio = avatarId.trim();
  if (!limpio) return null;
  return `/${limpio}.png`;
}

export function AvatarCircle({
  nombre,
  avatarId,
  sizeClass = "w-10 h-10",
  textClass = "text-sm",
  bgClass = "bg-[#1a2d4a]",
}: {
  nombre: string;
  avatarId?: string | null;
  sizeClass?: string;
  textClass?: string;
  bgClass?: string;
}) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    setMounted(true);
  }, []);

  const src = getAvatarSrc(avatarId);
  const inicial = mounted ? (nombre?.charAt(0) ?? "?").toUpperCase() : "?";

  return (
    <div className={`${sizeClass} rounded-full overflow-hidden ${bgClass} flex items-center justify-center`}>
      {(mounted && src) ? (
        <Image
          src={src}
          alt={`Avatar de ${nombre}`}
          width={160}
          height={160}
          className="w-full h-full object-cover"
        />
      ) : (
        <span className={`text-white font-semibold ${textClass}`}>{inicial}</span>
      )}
    </div>
  );
}
