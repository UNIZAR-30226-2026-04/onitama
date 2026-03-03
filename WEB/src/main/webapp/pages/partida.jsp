<%@ page import="VO.Partida" %>
<%@ page import="VO.Jugador" %>
<%@ page import="VO.Ficha" %>
<%@ page import="VO.Posicion" %>
<%@ page import="VO.Carta" %>
<%@ page import="VO.Tablero" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%
	String XF = request.getParameter("xF");
	String XI = request.getParameter("xA");
	String YF = request.getParameter("yF");
	String YI = request.getParameter("yA");
	Partida partida = (Partida) session.getAttribute("partida");
	
    String correo = (String) session.getAttribute("correo");
    int DIM = 7;
    int dis_casilla = 85;
    
    //Esto es para la prueba, si es null de normal debe saltar error
    if (partida == null) {
    	Jugador j1 = new Jugador("admin@test.com", "Ciro", "pass1234", 2000, 500, 10, 15); // 2000 puntos, 500 cores, 10 ganadas, 15 jugadas
        Jugador j2 = new Jugador("rival@test.com", "Rival", "pass4567", 1500, 300, 5, 12); // 1500 puntos, 300 cores, 5 ganadas, 12 jugadas
        j1.registrarse();
        j2.registrarse();
        partida = new Partida(999, "Pe", "Pe", j1.getNombre(), j2.getNombre());
        partida.registrarPartida();
    }
    
    //--------CARTAS-----------
	Carta tigre = new Carta("tigre");
    tigre.anyadirMovimiento(new Posicion(1,1,null));
    tigre.anyadirMovimiento(new Posicion(0,4,null));
    tigre.anyadirMovimiento(new Posicion(-1,0,null));
    tigre.anyadirMovimiento(new Posicion(0,-1,null));
    //-------------------------
    
    Tablero tablero = partida.getTablero();
	if(XF!=null && XI!=null && YF!=null && YI!=null){
		Posicion origen = tablero.getPosicion(Integer.parseInt(XI), Integer.parseInt(YI));		
		Posicion destino = tablero.getPosicion(Integer.parseInt(XF), Integer.parseInt(YF));	
		destino.setFicha(origen.getFicha());
		origen.setFicha(null);
	}
%>

<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Onitama</title>
  <style>
    body { margin:0; font-family:Arial,sans-serif; background-color:#12023c; color:white; display:flex; flex-direction:column; align-items:center; }
    .header { width:100%; display:flex; justify-content:space-between; align-items:center; background-color:#1d0a5a; border-bottom:3px solid #6a00ff; padding:10px 20px; box-sizing:border-box; }
    
    /* ESTILO PARA EL BOTÓN DE CERRAR SESIÓN */
    .casilla {
        position: absolute;
        top: var(--Y);
    	left: var(--X);
        background-color: white;
        padding: 40px 40px;
    }
    
    .ficha {
	    position: absolute;
	    top: var(--Y);
	    left: var(--X);
	    background-color: var(--Equipo);
	    width: 45px; 
	    height: 45px;
	    border-radius: 50%;
    }
    
    .carta {
        position: absolute;
        top: var(--Y);
    	left: var(--X);
        background-color: white;
        padding: 55px 40px;
    }
    
  </style>
</head>
<body>


<div class="carta" 
	style="--Y: 50px; --X: 1000px;"
	onclick="añadirDestinos([<%=tigre.pasarAString(tigre.getMov())%>])"></div>

<%for(int i=0; i<DIM; i++){
	for(int j=0; j<DIM; j++){%>
<div class="casilla" 
	id="casilla<%=i%><%=j%>"
	style="--Y: <%=(i+1)*dis_casilla%>px; --X: <%=(j+1)*dis_casilla%>px;"></div>
<%
		Posicion p = tablero.getPosicion(j,i);
		if(p.ocupado()==1){			
%>
<div class="ficha" style="--Equipo: red; --Y: <%=(i+1)*dis_casilla + 17.5%>px; --X: <%=(j+1)*dis_casilla + 17.5%>px;"></div>
<%		}else if(p.ocupado()==2){			
	%>
<div class="ficha" 
	id="ficha<%=i%><%=j%>"
	style="--Equipo: blue; --Y: <%=(i+1)*dis_casilla + 17.5%>px; --X: <%=(j+1)*dis_casilla + 17.5%>px;"
	onclick="cambiarColor(casilla<%=i%><%=j%>)"></div>
<%}}}%>

<script>
let casillaAntigua = null;
let destinos = [];
let carta = null;

	function cambiarColor(casilla) {
		document.querySelectorAll('.casilla').forEach(c => {
		    c.style.backgroundColor = 'white';
		    c.onclick = null;
		});
	
	    casilla.style.backgroundColor = 'yellow';
	    casillaAntigua = casilla;
	    
	    if(carta != null){
	        let idTexto = casilla.id; 
	        
	        let yActual = parseInt(idTexto.charAt(7)); 
	        let xActual = parseInt(idTexto.charAt(8));
	        console.log(destinos);
	        destinos.forEach(mov => {
	            let yFinal = yActual - mov.y;
	            let xFinal = xActual - mov.x;
	            
	            let idDestino = "casilla" + yFinal + xFinal;
	            let idPosibleFicha = "ficha" + yFinal + xFinal;
	            let casillaDestino = document.getElementById(idDestino);
	            
	            if(casillaDestino && !document.getElementById(idPosibleFicha)) {
	                casillaDestino.style.backgroundColor = 'yellow';
	                casillaDestino.onclick = function() {
	                    mover(casillaDestino, xFinal, yFinal, xActual, yActual);
	                };
	                console.log("Destino válido: " + idDestino);
	            }else{
	            	console.log("Destino inválido: " + idDestino);
	            }
	        });
	    }
	}
    
    function añadirDestinos(listaMov) {
    	destinos = listaMov;
    	carta = 1;
    }
    
    function mover(casillaDestino, xFinal, yFinal, xActual, yActual) {
        if(casillaDestino) {
        	window.location.href = "partida.jsp?xF=" + xFinal + "&yF=" + yFinal + "&xA=" + xActual + "&yA=" + yActual;
        }
    	
    }
    
</script>

</body>
</html>