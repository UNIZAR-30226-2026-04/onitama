package VO;

import java.util.List;
import java.util.ArrayList;

public class Carta {
    public String nombre;
    public List<Posicion> movimientos;

    public Carta(String Nom){
        nombre = Nom;
        movimientos = new ArrayList<Posicion>();
    }
    
    public boolean anyadirMovimiento(Posicion mov){
    	return movimientos.add(mov);
    }
    
    public List<Posicion> jugarCarta(Posicion origen, int DIM){
    	int xDestino;
    	int yDestino;
    	List<Posicion> destino = new ArrayList<Posicion>();
    	for(Posicion P : movimientos) {
    		xDestino = origen.getX() + P.getX();
    		yDestino = origen.getY() + P.getY();
    		if(xDestino >= 0 && xDestino < DIM && yDestino >= 0 && yDestino < DIM) {
    			destino.add(new Posicion(xDestino, yDestino, null));
    		}
    	}
    	return destino;
    }
    
    public List<Posicion> getMov(){
    	return movimientos;
    }
    
    public String pasarAString(List<Posicion> PS){
    	String pos = "";
    	for(int i=0; i<PS.size(); i++) {
    		Posicion P = PS.get(i);
    		pos = pos + "{x: " + String.valueOf(P.getX()) + ", y: " + String.valueOf(P.getY()) + "}";
    		if(i < PS.size()-1) {
    			pos = pos + ", ";
    		}
    	}
    	return pos;
    }
}
