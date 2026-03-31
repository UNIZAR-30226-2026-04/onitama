-- Skin base gratuita para todos los jugadores
INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES ('Skin0', 0, 'DEFAULT', 'DEFAULT', 'DEFAULT');
-- 'Ajedrez'
INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES ('Skin1', 20, 'DEFAULT', 'DEFAULT', 'DEFAULT');
-- 'El Clasico'
INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES ('Skin2', 20, 'DEFAULT', 'DEFAULT', 'DEFAULT');
-- 'Medieval'
INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES ('Skin3', 10, 'DEFAULT', 'DEFAULT', 'DEFAULT');
--'Minimalista'
INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES ('Skin4', 10, 'DEFAULT', 'DEFAULT', 'DEFAULT');
--'Pradera Solar'
INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES ('Skin5', 5, 'DEFAULT', 'DEFAULT', 'DEFAULT');
-- 'Hechizo de Calabaza'
INSERT INTO Skin (Nombre, Precio, Color_tablero, Color_Fichas_Aliadas, Color_Fichas_Enemigas) VALUES ('Skin6', 5, 'DEFAULT', 'DEFAULT', 'DEFAULT');

INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Tigre', '(0,-1),(0,2)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Dragon', '(-2,1),(-1,-1),(1,1),(2,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Rana', '(-1,1),(1,-1),(-2,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Conejo', '(1,1),(-1,1),(2,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Cangrejo', '(-2,0),(2,0),(0,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Elefante', '(1,1),(-1,1),(1,0),(-1,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Ganso', '(-1,1),(-1,0),(1,0),(1,-1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Gallo', '(1,0),(-1,0),(-1,-1),(1,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Mono', '(1,1),(-1,-1),(-1,1),(1,-1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Mantis', '(0,-1),(-1,-1),(1,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Caballo', '(0,1),(0,-1),(-1,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Buey', '(0,1),(0,-1),(1,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Grulla', '(1,-1),(-1,-1),(0,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Oso', '(1,0),(-1,0),(0,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Aguila', '(-1,0),(-1,1),(-1,-1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min) VALUES ('Cobra', '(-1,0),(1,1),(1,-1)',0);


INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Pensatorium', 'ESPEJO', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Santo Grial', 'REVIVIR', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Illusia', 'SALVAR_REY', 500);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Requiem', 'SACRIFICIO', 1000); --Se sacrifica un peon tuyo para matar a otro del enemigo
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('La Dama del Mar', 'SOLO_PARA_ADELANTE', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Kelpie', 'VENGANZA', 5000); --Si te matan al rey antes de los primeros 5 minutos, puedes jugar esta carta para quedar empate
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Atrapasueños', 'ROBAR', 1000);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Brujeria', 'CEGAR', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Finisterra', 'SOLO_PARA_ATRAS', 0);
