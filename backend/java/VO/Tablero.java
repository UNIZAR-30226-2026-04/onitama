package VO;

public class Tablero {
    public Posicion[][] tablero;
    public Posicion trono1, trono2;

    public Tablero(int DIM){
        if(DIM%2!=0){
            tablero = new Posicion[DIM][DIM];
            for (int i=0; i<DIM; i++) {
                for(int j=0; j<DIM; j++) {
                    if (i == 0) {
                        tablero[i][j] = new Posicion(j, i, new Ficha(false, 1)); //Codigo peon
                        tablero[i][j] = new Posicion(j, i, new Ficha(true, 1)); //Codigo rey
                    }else if (i == DIM - 1) {
                        tablero[i][j] = new Posicion(j, i, new Ficha(false, 2)); 
                        tablero[i][j] = new Posicion(j, i, new Ficha(true, 2)); 
                    }else{
                        tablero[i][j] = new Posicion(j, i, null);
                    }
                }
            }
            trono1 = new Posicion(DIM/2, 0, null);
            trono2 = new Posicion(DIM/2, DIM-1, null);
        }
    }
    
    public Posicion getPosicion(int x, int y){
    	return tablero[y][x];
    }
}