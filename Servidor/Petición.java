package Servidor;

import java.util.Arrays;

/**
 * Petición recibida por el canal de comandos, ya separada en sus partes:
 * <number> <COMANDO> <argumento1> <argumento2> ...
 */
public class Petición {

    private final String number;        // identificador del envío (lo pone el cliente)
    private final String comando;       // USER, PASS, ADDVUELO...
    private final String[] argumentos;  // lo que va detrás del comando (puede estar vacío)

    // Constructor privado: solo se crean peticiones a través de parsear()
    private Petición(String number, String comando, String[] argumentos) {
        this.number = number;
        this.comando = comando;
        this.argumentos = argumentos;
    }

    /**
     * Trocea una línea del protocolo.
     * @return la petición, o null si la línea está mal formada
     *         (no trae al menos <number> y <COMANDO>)
     */
    public static Petición parsear(String linea) {
        if (linea == null) {
            return null;
        }
        String[] partes = linea.trim().split(" ");
        if (partes.length < 2) {
            return null;
        }
        // Los argumentos son todo lo que va desde la posición 2 hasta el final
        String[] argumentos = Arrays.copyOfRange(partes, 2, partes.length);
        return new Petición(partes[0], partes[1], argumentos);
    }

    public String getNumber()              { return number; }
    public String getComando()             { return comando; }
    public int getNumArgumentos()          { return argumentos.length; }
    public String getArgumento(int indice) { return argumentos[indice]; }
}
