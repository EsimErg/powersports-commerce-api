package kz.powersports.commerce.torgsoft.web;

public class InvalidTorgsoftAdminTokenException
        extends RuntimeException {

    public InvalidTorgsoftAdminTokenException() {
        super("Недействительный токен управления Torgsoft");
    }
}