package backend.VO;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tablero {
    public Posicion[][] tablero;
    public Posicion trono1, trono2, rey1, rey2;
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
        Pattern trampa = Pattern.compile("\\|(-?\\d+),(-?\\d+),(\\d+)\\|"); //Trampas: formato |x,y,activa|. activa es 1 si la trampa esta activa, 0 si ya se ha activado (y por tanto la casilla queda inutilizada)
        Matcher t1 = trampa.matcher(p1);
        Matcher t2 = trampa.matcher(p2);
        int x, y, activa;

        if (t1.find()) {
            x = Integer.parseInt(t1.group(1)); // Primer grupo capturado (-?\\d+)
            y = Integer.parseInt(t1.group(2)); // Segundo grupo capturado (-?\\d+)
            activa = Integer.parseInt(t1.group(3)); // Tercer grupo capturado (\\d+)
            tablero[y][x].activarTrampa();
            if (activa == 0) {
                tablero[y][x].desactivarCasilla();
            }
        }

        if (t2.find()) {
            x = Integer.parseInt(t2.group(1));
            y = Integer.parseInt(t2.group(2));
            activa = Integer.parseInt(t2.group(3));
            tablero[y][x].activarTrampa();
            if (activa == 0) {
                tablero[y][x].desactivarCasilla();
            }
        }

        Pattern rey = Pattern.compile("\\[(-?\\d+),(-?\\d+)\\]");
        Matcher r1 = rey.matcher(p1);
        Matcher r2 = rey.matcher(p2);

        if (r1.find()) {
            x = Integer.parseInt(r1.group(1)); // Primer grupo capturado (-?\\d+)
            y = Integer.parseInt(r1.group(2)); // Segundo grupo capturado (-?\\d+)
            tablero[y][x].setFicha(new Ficha(true, 1));
            rey1 = tablero[y][x];
        }

        if (r2.find()) {
            x = Integer.parseInt(r2.group(1));
            y = Integer.parseInt(r2.group(2));
            tablero[y][x].setFicha(new Ficha(true, 2));
            rey2 = tablero[y][x];
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
                        rey1 = tablero[i][j];
                    } else {
                        // peones
                        tablero[i][j].setFicha(new Ficha(false, 1));
                    }
                } else if (i == DIM - 1) {
                    //  Equipo 2
                    if (j == centro) {
                        tablero[i][j].setFicha(new Ficha(true, 2));
                        rey2 = tablero[i][j];
                    } else {
                        tablero[i][j].setFicha(new Ficha(false, 2));
                    }
                }
            }
        }
    }

    public void setRey(int x, int y, int equipo) {
        if (equipo == 1) {
            setRey1(x, y);
        } else {
            setRey2(x, y);
        }
    }
    
    public void setRey1(int x, int y) {
        rey1 = tablero[y][x];
    }

    public void setRey2(int x, int y) {
        rey2 = tablero[y][x];
    }

    public Posicion getRey(int equipo) {
        if (equipo == 1) {
            return rey1;
        } else {
            return rey2;
        }
    }

    public Posicion getRey1() {
        return rey1;
    }

    public Posicion getRey2() {
        return rey2;
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

    public void getPosicionesEquipos(StringPorReferencia p1, StringPorReferencia p2) {
        String pos1 = "";
        String pos2 = "";
        boolean primerElemento1 = true;
        boolean primerElemento2 = true;

        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                Posicion p = tablero[i][j];
                Ficha f = (p != null) ? p.getFicha() : null;
                if (p==null || f == null) continue;
                if (f.getEquipo() == 1) {
                    if (p.esTrampa()) {
                        // Formato Trampa: |x,y,activa|
                        pos1 = pos1 + "|" + p.getX() + "," + p.getY() + "," + (p.estaActiva() ? "1" : "0") + "|";
                        primerElemento1 = false;
                    }
                    
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
                    if (p.esTrampa()) {
                        // Formato Trampa: |x,y,activa|
                        pos2 = pos2 + "|" + p.getX() + "," + p.getY() + "," + (p.estaActiva() ? "1" : "0") + "|";
                        primerElemento2 = false;
                    }

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

    /** Cuenta fichas vivas en tablero del equipo indicado (1 o 2). */
    public int contarFichasEquipo(int equipo) {
        int n = 0;
        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                Ficha f = tablero[i][j].getFicha();
                if (f != null && f.getEquipo() == equipo) {
                    n++;
                }
            }
        }
        return n;
    }

    public boolean existeMovimiento(List<Posicion> mov, int equipo, CartaAccion J1, CartaAccion J2){
        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                Ficha f = tablero[i][j].getFicha();
                if (f != null && f.getEquipo() == equipo) {
                    for(Posicion m : mov){
                        if((J1 == null || (J1 != null && J1.permiteMovimiento(m.getX(), m.getY()))) && (J2 == null || (J2 != null && J2.permiteMovimiento(m.getX(), m.getY())))){
                            int x = (equipo == 1) ? j-m.getX() : j+m.getX();
                            int y = (equipo == 1) ? i+m.getY() : i-m.getY();
                            if(x >= 0 && y >= 0 && x < DIM && y < DIM){
                                Posicion destino = tablero[y][x];
                                Ficha fDest = destino.getFicha();
                                if(destino.estaActiva() && (fDest == null || (fDest != null && fDest.getEquipo() != equipo))){
                                    return true; //Devolvemos true si hay una casilla donde podemos mover con los movimientos que se nos pase
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}