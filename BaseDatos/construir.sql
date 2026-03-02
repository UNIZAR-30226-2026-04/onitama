INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Tigre', '(0,-1),(0,2)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Dragon', '(-2,1),(-1,-1),(1,1),(2,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Rana', '(-1,1),(1,-1),(-2,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Conejo', '(1,1),(-1,1),(2,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Cangrejo', '(-2,0),(2,0),(0,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Elefante', '(1,1),(-1,1),(1,0),(-1,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Ganso', '(-1,1),(-1,0),(1,0),(1,-1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Gallo', '(1,0),(-1,0),(-1,-1),(1,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Mono', '(1,1),(-1,-1),(-1,1),(1,-1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Mantis', '(0,-1),(-1,-1),(1,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Caballo', '(0,1),(0,-1),(-1,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Buey', '(0,1),(0,-1),(1,0)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Grulla', '(1,-1),(-1,-1),(0,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Oso', '(1,0),(-1,0),(0,1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Aguila', '(-1,0),(-1,1),(-1,-1)',0);
INSERT INTO Cartas_Mov (Nombre, Movimientos) VALUES ('Cobra', '(-1,0),(1,1),(1,-1)',0);

INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Pensatorium', 'ESPEJO', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Santo Grial', 'REVIVIR', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Illusia', 'SALVAR_REY', 500);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Requiem', 'SACRIFICIO', 1000); --Se sacrifica un peon tuyo para matar a otro del enemigo
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('La Dama del Mar', 'SOLO_PARA_ADELANTE', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Kelpie', 'VENGANZA', 5000); --Si te matan al rey antes de los primeros 5 minutos, puedes jugar esta carta para quedar empate
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Atrapasueños', 'ROBAR', 1000);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Brujeria', 'CEGAR', 0);
INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES ('Finisterra', 'SOLO_PARA_ATRAS', 0);