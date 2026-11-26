package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Cliente de consola de la compañía aérea.
 * Numera cada envío, lo manda por el canal de comandos y muestra la respuesta.
 */
public class Cliente {

    public static void main(String[] args) {
        String host = "127.0.0.1";   // este mismo ordenador
        int puerto = 5000;           // canal de comandos

        try (
            Socket socket = new Socket(host, puerto);   // aquí se conecta
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Scanner teclado = new Scanner(System.in)    // para leer lo que escribes en consola
        ) {
            System.out.println("Conectado. Escribe comandos (EXIT para salir).");
            System.out.println("Truco de pruebas: empieza con ! para enviar la línea tal cual, sin número.");
            int contador = 1;   // número del próximo envío

            while (true) {
                System.out.print("> ");
                String mensaje = teclado.nextLine().trim();
                if (mensaje.isEmpty()) continue;   // ignorar líneas vacías

                String envio;
                if (mensaje.startsWith("!")) {
                    envio = mensaje.substring(1);       // modo pruebas: enviar sin número
                } else {
                    envio = contador + " " + mensaje;   // modo normal: el cliente numera
                    contador++;
                }

                salida.println(envio);
                System.out.println("C: " + envio);

                String respuesta = entrada.readLine();   // esperar la respuesta
                if (respuesta == null) {
                    System.out.println("El servidor ha cerrado la conexión");
                    break;
                }
                System.out.println("S: " + respuesta);

                // Si hemos enviado EXIT y el servidor responde OK, terminamos
                String[] partesEnvio = envio.split(" ");
                if (partesEnvio.length >= 2 && partesEnvio[1].equals("EXIT")
                        && respuesta.startsWith("OK ")) {
                    System.out.println("Sesión terminada.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo conectar con el servidor: " + e.getMessage());
        }
    }
}
