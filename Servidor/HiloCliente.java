package Servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende a un cliente por el canal de comandos.
 * Hay un hilo de estos por cada cliente conectado.
 */
public class HiloCliente extends Thread {

    // Único usuario del sistema (apartado 2.1 del enunciado)
    private static final String USUARIO_VALIDO = "admin";
    private static final String CLAVE_VALIDA = "admin";

    // Compartido por TODOS los hilos (static): clientes conectados ahora mismo
    private static int clientesConectados = 0;

    // Propio de CADA hilo: su socket, su salida y su estado de sesión
    private final Socket socket;
    private PrintWriter salida;
    private EstadoSesion estado = EstadoSesion.SIN_USUARIO;
    private boolean activo = true;   // pasa a false con EXIT

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    // ---------- Contador de sesiones (acceso sincronizado) ----------

    private static synchronized void sumarCliente()         { clientesConectados++; }
    private static synchronized void restarCliente()        { clientesConectados--; }
    private static synchronized int getClientesConectados() { return clientesConectados; }

    // ---------- Bucle principal ----------

    @Override
    public void run() {
        sumarCliente();
        try (
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            this.salida = out;
            String linea;
            // Se sale si el cliente manda EXIT (activo = false) o si cierra la conexión (null)
            while (activo && (linea = entrada.readLine()) != null) {
                System.out.println("<< " + linea);
                procesar(linea);
            }
        } catch (IOException e) {
            System.err.println("Conexión perdida: " + e.getMessage());
        } finally {
            // finally se ejecuta SIEMPRE: salga bien, con EXIT o por un error
            restarCliente();
        }
        System.out.println("Cliente desconectado");
    }

    /** Decide qué hacer con una línea recibida */
    private void procesar(String linea) {
        Petición peticion = Petición.parsear(linea);
        if (peticion == null) {
            responder("FAILED", "?", 400, "Línea mal formada");
            return;
        }
        String n = peticion.getNumber();

        switch (peticion.getComando()) {
            // --- Comandos de sesión: NO exigen estar autenticado ---
            case "USER" -> {
                if (comprobarArgumentos(peticion, 1)) {
                    user(n, peticion.getArgumento(0));
                }
            }
            case "PASS" -> {
                if (comprobarArgumentos(peticion, 1)) {
                    pass(n, peticion.getArgumento(0));
                }
            }
            case "EXIT" -> {
                if (comprobarArgumentos(peticion, 0)) {
                    exit(n);
                }
            }
            // --- El resto: primero argumentos (401), luego sesión (402) ---
            case "SESIONES" -> {
                if (comprobarArgumentos(peticion, 0) && comprobarSesion(n)) {
                    responder("OK", n, 203, String.valueOf(getClientesConectados()));
                }
            }
            case "ADDVUELO" -> {
                if (comprobarArgumentos(peticion, 0) && comprobarSesion(n)) {
                    responder("OK", n, 200, "ADDVUELO pendiente (día 4)");   // TEMPORAL
                }
            }
            case "GETVUELO" -> {
                if (comprobarArgumentos(peticion, 1) && comprobarSesion(n)) {
                    responder("OK", n, 200, "GETVUELO pendiente (día 5)");   // TEMPORAL
                }
            }
            default -> responder("FAILED", n, 400, "Comando desconocido");
        }
    }

    // ---------- Comandos de sesión ----------

    /** USER <nombre> */
    private void user(String n, String nombre) {
        if (estado == EstadoSesion.AUTENTICADO) {
            responder("FAILED", n, 403, "La sesión ya está iniciada");
            return;
        }
        if (!nombre.equals(USUARIO_VALIDO)) {
            estado = EstadoSesion.SIN_USUARIO;
            responder("FAILED", n, 410, "Usuario desconocido");
            return;
        }
        estado = EstadoSesion.ESPERANDO_CLAVE;
        responder("OK", n, 201, "Envíe la contraseña");
    }

    /** PASS <clave> */
    private void pass(String n, String clave) {
        if (estado == EstadoSesion.AUTENTICADO) {
            responder("FAILED", n, 403, "La sesión ya está iniciada");
            return;
        }
        if (estado == EstadoSesion.SIN_USUARIO) {
            responder("FAILED", n, 403, "Falta el usuario");
            return;
        }
        if (!clave.equals(CLAVE_VALIDA)) {
            // Seguimos en ESPERANDO_CLAVE: puede repetir PASS sin volver a mandar USER
            responder("FAILED", n, 411, "Contraseña incorrecta");
            return;
        }
        estado = EstadoSesion.AUTENTICADO;
        responder("OK", n, 202, "Bienvenido, " + USUARIO_VALIDO);
        System.out.println("Sesión iniciada desde " + socket.getInetAddress().getHostAddress());
    }

    /** EXIT: responde y marca el hilo para terminar (se cierra la conexión) */
    private void exit(String n) {
        responder("OK", n, 200, "Sesión cerrada");
        activo = false;
    }

    // ---------- Comprobaciones comunes ----------

    /** Si el número de argumentos no es el esperado, responde 401 y devuelve false */
    private boolean comprobarArgumentos(Petición peticion, int esperados) {
        if (peticion.getNumArgumentos() != esperados) {
            responder("FAILED", peticion.getNumber(), 401,
                    "Se esperaban " + esperados + " argumentos y llegaron " + peticion.getNumArgumentos());
            return false;
        }
        return true;
    }

    /** Si no hay sesión iniciada, responde 402 y devuelve false */
    private boolean comprobarSesion(String n) {
        if (estado != EstadoSesion.AUTENTICADO) {
            responder("FAILED", n, 402, "No autenticado");
            return false;
        }
        return true;
    }

    /** Envía una respuesta con el formato: <TIPO> <number> <código> <información> */
    private void responder(String tipo, String number, int codigo, String info) {
        String linea = tipo + " " + number + " " + codigo + " " + info;
        System.out.println(">> " + linea);
        salida.println(linea);
    }
}
