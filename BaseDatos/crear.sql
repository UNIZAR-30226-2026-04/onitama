CREATE TABLE Jugador (
    Correo VARCHAR(255) UNIQUE,
    Nombre_US VARCHAR(100) PRIMARY KEY,
    Contrasena_Hash VARCHAR(255),
    Puntos INTEGER
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
    Movimientos TEXT
);

CREATE TABLE Cartas_Accion (
    Nombre VARCHAR(50) PRIMARY KEY,
    Accion TEXT,
    Puntos_min INTEGER
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
    J2 VARCHAR(255),
    Es_Ganador_J1 BOOLEAN,
    Es_Ganador_J2 BOOLEAN,
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

CREATE TABLE Amistades (
    Jugador_1 VARCHAR(255),
    Jugador_2 VARCHAR(255),
    PRIMARY KEY (Jugador_1, Jugador_2),
    FOREIGN KEY (Jugador_1) REFERENCES Jugador(Nombre_US),
    FOREIGN KEY (Jugador_2) REFERENCES Jugador(Nombre_US)
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
    PRIMARY KEY (ID_Partida, ID_Carta_Accion),
    FOREIGN KEY (ID_Partida) REFERENCES Partida(ID_Partida),
    FOREIGN KEY (ID_Carta_Accion) REFERENCES Cartas_Accion(Nombre)
);

--Indices que se encargan que solo haya una partida en marcha por jugador
CREATE UNIQUE INDEX solo1J1 
ON PARTIDA (J1, Estado) 
WHERE Estado = 'Jugandose';

CREATE UNIQUE INDEX solo1J2 
ON PARTIDA (J2, Estado) 
WHERE Estado = 'Jugandose';