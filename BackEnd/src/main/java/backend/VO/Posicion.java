package backend.VO;

public class Posicion {
    private final int x, y;
    private boolean trampa;
    private boolean activa;
    private Ficha ficha;

    public Posicion(int px, int py, Ficha F){
        x=px;
        y=py;
        ficha = F;
        trampa = false;
        activa = true;
    }

    public void activarTrampa(){
        trampa = true;
    }

    public boolean esTrampa(){
        return trampa;
    }

    public boolean estaActiva(){
        return activa;
    }

    public void desactivarCasilla(){
        activa = false;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }
    
    public Ficha getFicha(){
        return ficha;
    }

    public int setFicha(Ficha F){
        if (F != null && this.ocupado() != -1 && this.ocupado() != F.getEquipo()) {
            boolean reyMatado = this.matar(); // Mata la ficha actual ANTES de reemplazarla
            ficha = F;
            if(reyMatado){
                return 3; //Mata al rey
            }else{
                return 2; //Mata un peon
            }
        }else if (F != null && this.ocupado() != -1) {
            return 1; //No puede mover (Esta ocupado por otra ficha suya)
        }else{
            ficha = F;
            return 0; //Casilla vacia
        }
    }

    public int ocupado(){
        if(ficha == null){
            return -1;
        }else{
            return ficha.getEquipo();
        }
    }

    public boolean matar(){
        return ficha.matar();
    }
}