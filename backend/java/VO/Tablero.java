package VO;

public class Tablero {
    public Posicion[][] tablero;
    public Posicion trono1, trono2;

    public Tablero(int DIM){
        // Solo crear tablero si la dimensión es impar
        if(DIM%2!=0){
            tablero = new Posicion[DIM][DIM];
            int centro = DIM / 2; // Posición de los tronos
            
            for (int i=0; i<DIM; i++) {
                for(int j=0; j<DIM; j++) {
                    if (i == 0) {
                        //  Equipo 1
                        if (j == centro) {
                            // rey
                            tablero[i][j] = new Posicion(j, i, new Ficha(true, 1));
                        } else {
                            // peones
                            tablero[i][j] = new Posicion(j, i, new Ficha(false, 1));
                        }
                    } else if (i == DIM - 1) {
                        //  Equipo 2
                        if (j == centro) {
                            tablero[i][j] = new Posicion(j, i, new Ficha(true, 2));
                        } else {
                            tablero[i][j] = new Posicion(j, i, new Ficha(false, 2));
                        }
                    } else {
                        // Filas del medio: Vacías
                        tablero[i][j] = new Posicion(j, i, null);
                    }
                }
            }
            
            // tronos
            trono1 = tablero[0][centro]; // Trono del Equipo 1 (fila 0, columna centro)
            trono2 = tablero[DIM-1][centro]; // Trono del Equipo 2 (última fila, columna centro)
        }
    }
    
    public Posicion getPosicion(int x, int y){
    	return tablero[y][x];
    }
}