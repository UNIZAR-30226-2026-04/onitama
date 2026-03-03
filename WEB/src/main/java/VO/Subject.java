package VO;

import java.util.ArrayList;
import java.util.List;

//Patron Subject para enviar notificaciones 
public class Subject{
    List<Observer> observadores;

    public Subject(){
        observadores = new ArrayList<>();
    }

    //Añade un observador, seria como subscribirse a las notificaciones de la partida (lo tendrian que ejecutar los jugadores antes de empezar la partida)
    public void attach(Observer ob){
        observadores.add(ob);
    }

    //No se si es necesario, pero podria ser para que un jugador se desuscriba de las notificaciones de la partida (lo tendrian que ejecutar los jugadores al acabar la partida)
    public void dettach(Observer ob){
        observadores.remove(ob);
    }

    public void nootify(){
        for (Observer ob : observadores) {
            ob.update();
        }
    }
}