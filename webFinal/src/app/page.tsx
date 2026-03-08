import Header from "@/components/Header";
import Image from "next/image";

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      {/* Hero Section */}
      <main className="flex-1 relative min-h-[calc(100vh-72px)] overflow-hidden">
        {/* Fondo: imagen local del proyecto */}
        <div
          className="absolute inset-0 bg-cover bg-center bg-no-repeat"
          style={{
            backgroundImage: `linear-gradient(to bottom, rgba(26, 45, 74, 0.65) 0%, rgba(26, 45, 74, 0.45) 40%, rgba(26, 45, 74, 0.6) 100%),
              url('/fondoMainPage.png')`,
          }}
        />

        {/* Contenido */}
        <div className="relative z-10 h-full flex flex-col lg:flex-row items-center justify-between px-6 py-16 lg:px-16 lg:py-24 max-w-7xl mx-auto">
          {/* Bloque de texto */}
          <div className="max-w-xl lg:max-w-2xl space-y-8">
            <p className="text-white/95 text-lg leading-relaxed">
              En las cumbres azules, dos maestros se miden en un duelo de
              paciencia. Aquí no hay azar: el movimiento que usas decide tu
              destino. El tablero es más grande, la tensión es real y un solo
              error te deja fuera. Captura al maestro o domina su templo para
              ganar.
            </p>
            <p className="text-white font-semibold text-xl lg:text-2xl uppercase tracking-wide leading-snug">
              Tu mente es el arma, el tablero es el camino, y el honor tu única
              guía.
            </p>
          </div>

          {/* Imagen de los dos luchadores */}
          <div className="hidden lg:flex items-end mt-8 lg:mt-0">
            <Image
              src="/luchadores.png"
              alt="Dos maestros enfrentados"
              width={420}
              height={320}
              className="h-72 w-auto object-contain drop-shadow-2xl"
              priority
            />
          </div>
        </div>
      </main>
    </div>
  );
}
