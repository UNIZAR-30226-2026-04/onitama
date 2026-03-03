package VO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tablero {
    public Posicion[][] tablero;
    public Posicion trono1, trono2;
    private int DIM;
    private boolean[][] casillaTrampa; //Para marcar casillas trampa

    public Tablero(int DIM){
        this.DIM = DIM;
        // Solo crear tablero si la dimensión es impar
        if(DIM%2!=0){
            tablero = new Posicion[DIM][DIM];
            casillaTrampa = new boolean[DIM][DIM];
            int centro = DIM / 2; // Posición de los tronos
            
            for (int i=0; i<DIM; i++) {
                for(int j=0; j<DIM; j++) {
                    tablero[i][j] = new Posicion(j, i, null);
                    casillaTrampa[i][j] = false;
                }
            }
            
            // tronos
            trono1 = tablero[0][centro]; // Trono del Equipo 1 (fila 0, columna centro)
            trono2 = tablero[DIM-1][centro]; // Trono del Equipo 2 (última fila, columna centro)
        }
    }

    //Cargar tablero desde dos posiciones dadas. Formato: Rey [x,y], Peon (x,y)
    public void cargarTablero(String p1, String p2){
        Pattern rey = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]");
        Matcher r1 = rey.matcher(p1);
        Matcher r2 = rey.matcher(p2);
        int x, y;

        if (r1.find()) {
            x = Integer.parseInt(r1.group(1)); // Primer grupo capturado (-?\\d+)
            y = Integer.parseInt(r1.group(2)); // Segundo grupo capturado (-?\\d+)
            tablero[y][x].setFicha(new Ficha(true, 1));
        }

        if (r2.find()) {
            x = Integer.parseInt(r2.group(1));
            y = Integer.parseInt(r2.group(2));
            tablero[y][x].setFicha(new Ficha(true, 2));
        }

        // Peones: formato (x,y). No usar el genérico para no pisar las posiciones de rey [x,y]
        Pattern posiciones = Pattern.compile("\\((-?\\d+),(-?\\d+)\\)");
        Matcher m1 = posiciones.matcher(p1);
        Matcher m2 = posiciones.matcher(p2);

        while (m1.find()) {
            // m.group(1) es la X, m.group(2) es la Y
            x = Integer.parseInt(m1.group(1));
            y = Integer.parseInt(m1.group(2));
            tablero[y][x].setFicha(new Ficha(false, 1));
        }

        while (m2.find()) {
            // m.group(1) es la X, m.group(2) es la Y
            x = Integer.parseInt(m2.group(1));
            y = Integer.parseInt(m2.group(2));
            tablero[y][x].setFicha(new Ficha(false, 2));
        }
    }

    public void cargarTablero(){
        int centro = DIM / 2; 
        for (int i=0; i<DIM; i++) {
            for(int j=0; j<DIM; j++) {
                // Limpiar casilla primero por si acaso
                tablero[i][j].setFicha(null);

                if (i == 0) {
                    //  Equipo 1
                    if (j == centro) {
                        // rey
                        tablero[i][j].setFicha(new Ficha(true, 1));
                    } else {
                        // peones
                        tablero[i][j].setFicha(new Ficha(false, 1));
                    }
                } else if (i == DIM - 1) {
                    //  Equipo 2
                    if (j == centro) {
                        tablero[i][j].setFicha(new Ficha(true, 2));
                    } else {
                        tablero[i][j].setFicha(new Ficha(false, 2));
                    }
                }
            }
        }
    }
    
    public Posicion getPosicion(int x, int y){
    	return tablero[y][x];
    }

    public int getDIM(){
        return DIM;
    }

    //Captura la ficha en la posicion (x, y) y la mata
    //Devuelve true si la ficha capturada era el rey (fin de partida)
    public boolean capturarFicha(int x, int y){
        if (!esCasillaValida(x, y)) {
            return false;
        }
        Posicion pos = tablero[y][x];
        Ficha ficha = pos.getFicha();
        if (ficha == null) {
            return false; //No hay ficha que capturar
        }
        boolean eraRey = ficha.matar();
        pos.setFicha(null); //Vaciar la casilla
        return eraRey;
    }

    //Comprueba si las coordenadas (x, y) estan dentro del tablero
    public boolean esCasillaValida(int x, int y){
        return x >= 0 && x < DIM && y >= 0 && y < DIM;
    }

    //Coloca una casilla trampa en las coordenadas (x, y) indicadas por el jugador
    //No se puede poner en tronos, casillas ocupadas ni casillas que ya son trampa
    public boolean seleccionarCasillaTrampa(int x, int y){
        if (!esCasillaValida(x, y)) {
            return false;
        }
        Posicion pos = tablero[y][x];

        //No poner trampa en tronos, en casillas ocupadas, ni en casillas que ya son trampa
        if (pos != trono1 && pos != trono2 && pos.ocupado() == -1 && !casillaTrampa[y][x]) {
            casillaTrampa[y][x] = true;
            return true;
        }
        return false; //Casilla no valida para trampa
    }

    //Comprueba si la casilla (x, y) es una trampa
    public boolean verificarCasillaTrampa(int x, int y){
        if (!esCasillaValida(x, y)) {
            return false;
        }
        return casillaTrampa[y][x];
    }

    public void getPosicionesEquipos(StringPorReferencia p1, StringPorReferencia p2) {
        String pos1 = "";
        String pos2 = "";
        boolean primerElemento1 = true;
        boolean primerElemento2 = true;

        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                Posicion p = tablero[i][j];
                Ficha f = (p != null) ? p.getFicha() : null;
                if (f == null) continue;
                if (f.getEquipo() == 1) {
                    
                    // Si no es el primero, ponemos una coma separadora
                    if (!primerElemento1) {
                        pos1 = pos1 + ",";
                    }
                    
                    if (f.isRey()) {
                        pos1 = pos1 + "[" + p.getX() + "," + p.getY() + "]";
                    } else {
                        pos1 = pos1 + "(" + p.getX() + "," + p.getY() + ")";
                    }
                    primerElemento1 = false;
                } else if (f.getEquipo() == 2) {
                    if (!primerElemento2) {
                        pos2 = pos2 + ",";
                    }
                    if (f.isRey()) {
                        // Formato Rey: [x,y]
                        pos2 = pos2 + "[" + p.getX() + "," + p.getY() + "]";
                    } else {
                        pos2 = pos2 + "(" + p.getX() + "," + p.getY() + ")";
                    }
                    
                    primerElemento2 = false;
                }
            }
        }

        p2.setValor(pos2);
        p1.setValor(pos1);
    }
}