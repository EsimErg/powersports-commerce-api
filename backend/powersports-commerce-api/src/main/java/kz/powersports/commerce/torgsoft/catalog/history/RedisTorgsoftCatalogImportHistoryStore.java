package kz.powersports.commerce.torgsoft.catalog.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "torgsoft",
        name = "enabled",
        havingValue = "true"
)
public class RedisTorgsoftCatalogImportHistoryStore
        implements TorgsoftCatalogImportHistoryStore {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RedisTorgsoftCatalogImportHistoryStore.class
            );

    private static final String HISTORY_KEY =
            "powersports:torgsoft:catalog-import:history";

    private static final int MAX_ENTRIES = 50;

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public RedisTorgsoftCatalogImportHistoryStore(
            StringRedisTemplate redisTemplate,
            JsonMapper jsonMapper
    ) {
        this.redisTemplate = Objects.requireNonNull(
                redisTemplate,
                "redisTemplate не должен быть null"
        );

        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "jsonMapper не должен быть null"
        );
    }

    @Override
    public void save(TorgsoftCatalogImportHistoryEntry entry) {
        Objects.requireNonNull(
                entry,
                "entry не должен быть null"
        );

        try {
            String json =
                    jsonMapper.writeValueAsString(entry);

            redisTemplate
                    .opsForList()
                    .leftPush(HISTORY_KEY, json);

            redisTemplate
                    .opsForList()
                    .trim(
                            HISTORY_KEY,
                            0,
                            MAX_ENTRIES - 1
                    );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Не удалось сериализовать историю импорта Torgsoft",
                    exception
            );
        }
    }

    @Override
    public List<TorgsoftCatalogImportHistoryEntry> findRecent(
            int limit
    ) {
        int safeLimit = Math.max(
                1,
                Math.min(limit, MAX_ENTRIES)
        );

        List<String> entries =
                redisTemplate
                        .opsForList()
                        .range(
                                HISTORY_KEY,
                                0,
                                safeLimit - 1
                        );

        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<TorgsoftCatalogImportHistoryEntry> result =
                new ArrayList<>();

        for (String json : entries) {
            try {
                TorgsoftCatalogImportHistoryEntry entry =
                        jsonMapper.readValue(
                                json,
                                TorgsoftCatalogImportHistoryEntry.class
                        );

                result.add(entry);

            } catch (JacksonException exception) {
                log.warn(
                        "Пропущена повреждённая запись истории Torgsoft",
                        exception
                );
            }
        }

        return List.copyOf(result);
    }
}