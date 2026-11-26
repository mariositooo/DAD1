package Servidor;

/** Estados por los que pasa la sesión de un cliente en el canal de comandos */
public enum EstadoSesion {
    SIN_USUARIO,      // recién conectado, o tras un USER erróneo
    ESPERANDO_CLAVE,  // USER correcto, falta el PASS
    AUTENTICADO       // USER y PASS correctos: puede usar los comandos de gestión
}
