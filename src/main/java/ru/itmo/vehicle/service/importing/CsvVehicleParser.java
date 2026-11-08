package ru.itmo.vehicle.service.importing;

import org.springframework.stereotype.Component;
import ru.itmo.vehicle.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class CsvVehicleParser {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "name", "coordinates.x", "coordinates.y", "enginePower",
            "capacity", "distanceTravelled", "fuelConsumption");

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "name", "coordinates.x", "coordinates.y", "type", "enginePower",
            "numberOfWheels", "capacity", "distanceTravelled", "fuelConsumption", "fuelType");

    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("name", "name"),
            Map.entry("название", "name"),
            Map.entry("coordinates.x", "coordinates.x"),
            Map.entry("coordinate.x", "coordinates.x"),
            Map.entry("coordx", "coordinates.x"),
            Map.entry("x", "coordinates.x"),
            Map.entry("coordinates.y", "coordinates.y"),
            Map.entry("coordinate.y", "coordinates.y"),
            Map.entry("coordy", "coordinates.y"),
            Map.entry("y", "coordinates.y"),
            Map.entry("type", "type"),
            Map.entry("vehicletype", "type"),
            Map.entry("enginepower", "enginePower"),
            Map.entry("numberofwheels", "numberOfWheels"),
            Map.entry("capacity", "capacity"),
            Map.entry("distancetravelled", "distanceTravelled"),
            Map.entry("fuelconsumption", "fuelConsumption"),
            Map.entry("fueltype", "fuelType")
    );

    public List<VehicleImportRow> parse(InputStream inputStream) {
        String content;
        try {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ValidationException(Map.of("file", "Не удалось прочитать файл: " + exception.getMessage()));
        }
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        if (content.isBlank()) {
            throw new ValidationException(Map.of("file", "Файл пуст"));
        }

        char delimiter = detectDelimiter(content);
        List<List<String>> records = parseRecords(content, delimiter);
        if (records.isEmpty()) {
            throw new ValidationException(Map.of("file", "Файл не содержит данных"));
        }

        List<String> header = records.get(0);
        Map<String, Integer> indexes = mapHeader(header);
        Set<String> missing = new LinkedHashSet<>(REQUIRED_FIELDS);
        missing.removeAll(indexes.keySet());
        if (!missing.isEmpty()) {
            throw new ValidationException(Map.of("file",
                    "В заголовке отсутствуют обязательные колонки: " + String.join(", ", missing)));
        }

        List<VehicleImportRow> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (record.stream().allMatch(value -> value == null || value.isBlank())) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : indexes.entrySet()) {
                int index = entry.getValue();
                values.put(entry.getKey(), index < record.size() ? record.get(index).trim() : "");
            }
            rows.add(new VehicleImportRow(i, values));
        }
        if (rows.isEmpty()) {
            throw new ValidationException(Map.of("file", "Файл не содержит строк с объектами"));
        }
        return rows;
    }

    private Map<String, Integer> mapHeader(List<String> header) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();
        for (int i = 0; i < header.size(); i++) {
            String raw = header.get(i).trim();
            if (raw.isEmpty()) {
                continue;
            }
            String canonical = HEADER_ALIASES.get(raw.toLowerCase(Locale.ROOT));
            if (canonical == null) {
                unknown.add(raw);
            } else {
                indexes.put(canonical, i);
            }
        }
        if (!unknown.isEmpty()) {
            throw new ValidationException(Map.of("file",
                    "Неизвестные колонки в заголовке: " + String.join(", ", unknown)));
        }
        if (!ALLOWED_FIELDS.containsAll(indexes.keySet())) {
            throw new ValidationException(Map.of("file", "Заголовок содержит неподдерживаемые колонки"));
        }
        return indexes;
    }

    private char detectDelimiter(String content) {
        int semicolons = 0;
        int commas = 0;
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (!quoted) {
                if (c == ';') {
                    semicolons++;
                } else if (c == ',') {
                    commas++;
                } else if (c == '\n' || c == '\r') {
                    break;
                }
            }
        }
        return semicolons > commas ? ';' : ',';
    }

    private List<List<String>> parseRecords(String content, char delimiter) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }
            if (c == '"' && field.length() == 0) {
                quoted = true;
            } else if (c == delimiter) {
                record.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else if (c != '\r') {
                field.append(c);
            }
        }
        if (quoted) {
            throw new ValidationException(Map.of("file", "Незакрытая кавычка в CSV-файле"));
        }
        if (field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
