package kz.powersports.commerce.torgsoft.web;

import kz.powersports.commerce.torgsoft.catalog.sync
        .TorgsoftCatalogImportAlreadyRunningException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes =
                TorgsoftCatalogImportController.class
)
public class TorgsoftCatalogImportExceptionHandler {

    @ExceptionHandler(
            InvalidTorgsoftAdminTokenException.class
    )
    public ProblemDetail handleInvalidToken() {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Доступ запрещён"
                );

        problem.setTitle("Ошибка авторизации");
        problem.setProperty(
                "code",
                "INVALID_TORGSOFT_ADMIN_TOKEN"
        );

        return problem;
    }

    @ExceptionHandler(
            TorgsoftCatalogImportAlreadyRunningException.class
    )
    public ProblemDetail handleImportAlreadyRunning() {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Импорт каталога уже выполняется"
                );

        problem.setTitle("Импорт уже запущен");
        problem.setProperty(
                "code",
                "TORGSOFT_IMPORT_ALREADY_RUNNING"
        );

        return problem;
    }
}