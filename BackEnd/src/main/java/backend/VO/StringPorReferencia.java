package backend.VO;

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