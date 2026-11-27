package pe.smartprice.jobs.common;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Escritor CSV mínimo (UTF-8, separador coma, comillas cuando hace falta). */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static void write(Path file, List<String> header, List<List<Object>> rows) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeLine(w, List.copyOf(header));
            for (List<Object> row : rows) {
                writeLine(w, row);
            }
        }
    }

    private static void writeLine(Writer w, List<?> values) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(values.get(i)));
        }
        w.write(sb.append('\n').toString());
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
