import Header from "@/components/Header";

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      {/* Hero Section */}
      <main className="flex-1 relative min-h-[calc(100vh-72px)] overflow-hidden">
        {/* Background */}
        <div
          className="absolute inset-0 bg-cover bg-center bg-no-repeat"
          style={{
            backgroundImage: `linear-gradient(to bottom, rgba(26, 45, 74, 0.7) 0%, rgba(26, 45, 74, 0.5) 40%, rgba(26, 45, 74, 0.6) 100%),
              url('https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=1920')`,
          }}
        />

        {/* Content */}
        <div className="relative z-10 h-full flex flex-col lg:flex-row items-center justify-between px-6 py-16 lg:px-16 lg:py-24 max-w-7xl mx-auto">
          {/* Text block */}
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

          {/* Character placeholders */}
          <div className="hidden lg:flex items-end gap-4 mt-8 lg:mt-0">
            <div
              className="w-32 h-48 rounded-lg bg-blue-900/60 border-2 border-blue-400/50 flex items-end justify-center pb-4"
              aria-hidden
            >
              <span className="text-blue-200/80 text-xs uppercase tracking-wider">
                Maestro azul
              </span>
            </div>
            <div
              className="w-32 h-48 rounded-lg bg-red-900/60 border-2 border-red-400/50 flex items-end justify-center pb-4"
              aria-hidden
            >
              <span className="text-red-200/80 text-xs uppercase tracking-wider">
                Maestro rojo
              </span>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
