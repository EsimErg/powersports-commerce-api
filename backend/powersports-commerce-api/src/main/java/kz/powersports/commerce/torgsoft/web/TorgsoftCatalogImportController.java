package kz.powersports.commerce.torgsoft.web;

import kz.powersports.commerce.torgsoft.catalog.sync.CatalogSyncReport;
import kz.powersports.commerce.torgsoft.catalog.sync.TorgsoftCatalogImportService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import kz.powersports.commerce.torgsoft.catalog.history.TorgsoftCatalogImportHistoryEntry;
import kz.powersports.commerce.torgsoft.catalog.history.TorgsoftCatalogImportHistoryStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/internal/torgsoft/catalog")
@ConditionalOnProperty(
        prefix = "torgsoft.manual-import",
        name = "enabled",
        havingValue = "true"
)

public class TorgsoftCatalogImportController {

    private final TorgsoftCatalogImportService importService;
    private final TorgsoftAdminTokenVerifier tokenVerifier;
    private final TorgsoftCatalogImportHistoryStore historyStore;
    public TorgsoftCatalogImportController(
            TorgsoftCatalogImportService importService,
            TorgsoftAdminTokenVerifier tokenVerifier,
            TorgsoftCatalogImportHistoryStore historyStore
    ) {
        this.importService = Objects.requireNonNull(
                importService,
                "importService не должен быть null"
        );

        this.tokenVerifier = Objects.requireNonNull(
                tokenVerifier,
                "tokenVerifier не должен быть null"
        );

        this.historyStore = Objects.requireNonNull(
                historyStore,
                "historyStore не должен быть null"
        );
    }

    @PostMapping("/import")
    public CatalogSyncReport importCatalog(
            @RequestHeader(
                    name = "X-Torgsoft-Admin-Token",
                    required = false
            )
            String adminToken
    ) {
        tokenVerifier.verify(adminToken);

        return importService.importCatalog();
    }
    @GetMapping("/history")
    public List<TorgsoftCatalogImportHistoryEntry> getHistory(
            @RequestHeader(
                    name = "X-Torgsoft-Admin-Token",
                    required = false
            )
            String adminToken,

            @RequestParam(
                    name = "limit",
                    defaultValue = "20"
            )
            int limit
    ) {
        tokenVerifier.verify(adminToken);

        return historyStore.findRecent(limit);
    }
}