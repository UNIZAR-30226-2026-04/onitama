package backend.VO;

//Forma de pasar un String por referencia para que un metodo sea capaz de modificarlo
public class StringPorReferencia{
    private String valor;

    public StringPorReferencia(String valor){
        this.valor = valor;
    }

    public String getValor(){
        return valor;
    }

    public void setValor(String v){
        valor = v;
    }
}