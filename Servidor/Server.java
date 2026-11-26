package Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor de la compañía aérea.
 * Escucha en el canal de comandos y lanza un hilo por cada cliente.
 */
public class Server {

	// Canal de comandos que fija el protocolo
	public static final int PuertoComando = 5000;

	public static void main(String[] args) {
		// ServerSocket = escucha en un puerto y espera a clientes
		try (ServerSocket serverSocket = new ServerSocket(PuertoComando)) {
			System.out.println("Servidor escuchando en el puerto " + PuertoComando);
			while (true) {
				// accept() BLOQUEA hasta que llega un cliente y devuelve un socket,
				// que es la línea directa con ese cliente en concreto
				Socket socketCliente = serverSocket.accept();
				System.out.println("Cliente conectado desde " + socketCliente.getInetAddress().getHostAddress());

				// Un hilo por cliente: el bucle vuelve enseguida a accept()
				// y puede atender a otro cliente mientras este sigue hablando
				new HiloCliente(socketCliente).start();
			}
		} catch (IOException e) {
			// Nunca dejar un catch vacío: si el puerto está ocupado, hay que enterarse
			System.err.println("Error en el servidor: " + e.getMessage());
		}
	}
}
