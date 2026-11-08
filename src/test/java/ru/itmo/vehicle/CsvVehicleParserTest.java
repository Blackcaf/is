package ru.itmo.vehicle;

import org.junit.jupiter.api.Test;
import ru.itmo.vehicle.exception.ValidationException;
import ru.itmo.vehicle.service.importing.CsvVehicleParser;
import ru.itmo.vehicle.service.importing.VehicleImportRow;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvVehicleParserTest {
    private final CsvVehicleParser parser = new CsvVehicleParser();

    @Test
    void parsesSemicolonSeparatedFileWithNestedCoordinates() {
        String csv = "name;coordinates.x;coordinates.y;type;enginePower;numberOfWheels;capacity;distanceTravelled;fuelConsumption;fuelType\n"
                + "Test;12.5;7;DRONE;15.5;4;2;120.0;3.5;ELECTRICITY\n";
        List<VehicleImportRow> rows = parser.parse(stream(csv));

        assertEquals(1, rows.size());
        VehicleImportRow row = rows.get(0);
        assertEquals("Test", row.get("name"));
        assertEquals("12.5", row.get("coordinates.x"));
        assertEquals("7", row.get("coordinates.y"));
        assertEquals("DRONE", row.get("type"));
        assertEquals("ELECTRICITY", row.get("fuelType"));
    }

    @Test
    void parsesCommaSeparatedFileAndQuotedValues() {
        String csv = "name,coordinates.x,coordinates.y,enginePower,capacity,distanceTravelled,fuelConsumption\n"
                + "\"Name, with comma\",1,2,3,4,5,6\n";
        List<VehicleImportRow> rows = parser.parse(stream(csv));

        assertEquals(1, rows.size());
        assertEquals("Name, with comma", rows.get(0).get("name"));
    }

    @Test
    void supportsAliasesAndBom() {
        String csv = "\uFEFFx;y;name;enginePower;capacity;distanceTravelled;fuelConsumption\n"
                + "1;2;Alias;3;4;5;6\n";
        List<VehicleImportRow> rows = parser.parse(stream(csv));

        assertEquals("1", rows.get(0).get("coordinates.x"));
        assertEquals("2", rows.get(0).get("coordinates.y"));
    }

    @Test
    void rejectsMissingRequiredHeader() {
        String csv = "name;coordinates.x\nTest;1\n";
        assertThrows(ValidationException.class, () -> parser.parse(stream(csv)));
    }

    @Test
    void rejectsUnclosedQuote() {
        String csv = "name;coordinates.x;coordinates.y;enginePower;capacity;distanceTravelled;fuelConsumption\n"
                + "\"Test;1;2;3;4;5;6\n";
        assertThrows(ValidationException.class, () -> parser.parse(stream(csv)));
    }

    @Test
    void rejectsUnknownHeader() {
        String csv = "name;coordinates.x;coordinates.y;enginePower;capacity;distanceTravelled;fuelConsumption;unknown\n"
                + "Test;1;2;3;4;5;6;x\n";
        assertThrows(ValidationException.class, () -> parser.parse(stream(csv)));
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
