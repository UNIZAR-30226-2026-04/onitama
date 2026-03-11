CREATE TABLE Jugador (
    Correo VARCHAR(255) UNIQUE,
    Nombre_US VARCHAR(100) PRIMARY KEY,
    Contrasena_Hash VARCHAR(255),
    Puntos INTEGER DEFAULT 0,
    Cores INTEGER DEFAULT 0,
    Partidas_Ganadas INTEGER DEFAULT 0,
    Partidas_Jugadas INTEGER DEFAULT 0
);

CREATE TABLE Skin (
    Nombre VARCHAR(50) PRIMARY KEY,
    Precio INTEGER,
    Color_tablero VARCHAR(20),
    Color_Fichas_Aliadas VARCHAR(20),
    Color_Fichas_Enemigas VARCHAR(20)
);

CREATE TABLE Cartas_Mov (
    Nombre VARCHAR(50) PRIMARY KEY,
    Movimientos TEXT,
    Puntos_min INTEGER,
    img VARCHAR(30) DEFAULT 'SIN_IMG'
);

CREATE TABLE Cartas_Accion (
    Nombre VARCHAR(50) PRIMARY KEY,
    Accion TEXT,
    Puntos_min INTEGER,
    img VARCHAR(30) DEFAULT 'SIN_IMG' --La idea es que haya una imagen de error para todas que no tengan img
);

CREATE TABLE Partida (
    ID_Partida SERIAL PRIMARY KEY, --Asignacion automatica del id
    Estado VARCHAR(20),
    Tiempo INTEGER,
    Tipo VARCHAR(20),
    Pos_Fichas_Eq1 TEXT,
    Pos_Fichas_Eq2 TEXT,
    FichasMuertas1 INTEGER,
    FichasMuertas2 INTEGER,
    J1 VARCHAR(255),
    J2 VARCHAR(255) CHECK (J2 <> J1),
    Es_Ganador_J1 BOOLEAN,
    Es_Ganador_J2 BOOLEAN,
    Turno INTEGER,
    FOREIGN KEY (J1) REFERENCES Jugador(Nombre_US),
    FOREIGN KEY (J2) REFERENCES Jugador(Nombre_US)
);

CREATE TABLE Jugador_Skins (
    Jugador VARCHAR(255),
    Skin VARCHAR(50),
    PRIMARY KEY (Jugador, Skin),
    FOREIGN KEY (Jugador) REFERENCES Jugador(Nombre_US),
    FOREIGN KEY (Skin) REFERENCES Skin(Nombre)
);

-- Amistades: solo amistades aceptadas. Las solicitudes pendientes van en Notificaciones.
CREATE TABLE Amistades (
    Jugador_1 VARCHAR(100),
    Jugador_2 VARCHAR(100),
    PRIMARY KEY (Jugador_1, Jugador_2),
    FOREIGN KEY (Jugador_1) REFERENCES Jugador(Nombre_US),
    FOREIGN KEY (Jugador_2) REFERENCES Jugador(Nombre_US),
    CONSTRAINT orden_amistad CHECK (Jugador_1 < Jugador_2)
);

-- Notificaciones unificadas: amistad, invitación partida, pausa, reanudar
CREATE TABLE Notificaciones (
    ID_Notificacion SERIAL PRIMARY KEY,
    Tipo VARCHAR(30) NOT NULL,
    Remitente VARCHAR(100) NOT NULL,
    Destinatario VARCHAR(100) NOT NULL,
    Estado VARCHAR(20) DEFAULT 'PENDIENTE',
    Fecha_Creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    Fecha_Expiracion TIMESTAMP,
    ID_Partida INTEGER,
    FOREIGN KEY (Remitente) REFERENCES Jugador(Nombre_US),
    FOREIGN KEY (Destinatario) REFERENCES Jugador(Nombre_US),
    FOREIGN KEY (ID_Partida) REFERENCES Partida(ID_Partida),
    CONSTRAINT tipo_valido CHECK (Tipo IN ('SOLICITUD_AMISTAD', 'INVITACION_PARTIDA', 'SOLICITUD_PAUSA', 'REANUDAR_PARTIDA')),
    CONSTRAINT estado_valido CHECK (Estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA'))
);

CREATE TABLE Partida_Cartas_Mov (
    ID_Partida INTEGER,
    ID_Carta_Mov VARCHAR(50),
    Estado VARCHAR(20),
    PRIMARY KEY (ID_Partida, ID_Carta_Mov),
    FOREIGN KEY (ID_Partida) REFERENCES Partida(ID_Partida),
    FOREIGN KEY (ID_Carta_Mov) REFERENCES Cartas_Mov(Nombre)
);

CREATE TABLE Partida_Cartas_Accion (
    ID_Partida INTEGER,
    ID_Carta_Accion VARCHAR(50),
    Estado VARCHAR(20),
    Equipo INTEGER,
    PRIMARY KEY (ID_Partida, ID_Carta_Accion),
    FOREIGN KEY (ID_Partida) REFERENCES Partida(ID_Partida),
    FOREIGN KEY (ID_Carta_Accion) REFERENCES Cartas_Accion(Nombre)
);

--Indices que se encargan que solo haya una partida en marcha por jugador
CREATE UNIQUE INDEX solo1J1 
ON PARTIDA (J1, Estado) 
WHERE Estado = 'JUGANDOSE';

CREATE UNIQUE INDEX solo1J2 
ON PARTIDA (J2, Estado) 
WHERE Estado = 'JUGANDOSE';

-- Índice para notificaciones pendientes por destinatario
CREATE INDEX idx_notif_dest_pendiente 
ON Notificaciones (Destinatario, Estado) 
WHERE Estado = 'PENDIENTE';