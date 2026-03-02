package VO;

import java.util.ArrayList;
import java.util.List;

//Patron Subject para enviar notificaciones 
public class Subject{
    List<Observer> observadores;

    public Subject(){
        observadores = new ArrayList<>();
    }

    public void attach(Observer ob){
        observadores.add(ob);
    }

    public void dettach(Observer ob){
        observadores.remove(ob);
    }

    public void nootify(){
        for (Observer ob : observadores) {
            ob.update();
        }
    }
}