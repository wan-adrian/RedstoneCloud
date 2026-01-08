package de.redstonecloud.shared.utils;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class SharedUtils {
    public static void enableDebug() {
        Configurator.setAllLevels(LogManager.ROOT_LOGGER_NAME, org.apache.logging.log4j.Level.DEBUG);
    }

    public static String[] dropFirstString(String[] input) {
        String[] anstring = new String[input.length - 1];
        System.arraycopy(input, 1, anstring, 0, input.length - 1);
        return anstring;
    }

    public static String convertYamlToJson(String yaml) {
        try {
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            Object obj = yamlReader.readValue(yaml, Object.class);

            ObjectMapper jsonWriter = new ObjectMapper();
            return jsonWriter.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static String convertJsonToYaml(String json) {
        try {
            // Jackson JSON reader
            ObjectMapper jsonReader = new ObjectMapper();
            Object obj = jsonReader.readValue(json, Object.class); // Parse JSON to Java object

            // SnakeYAML for YAML output with custom options
            DumperOptions options = new DumperOptions();
            options.setIndent(2); // Set indentation for readable YAML
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // Block style YAML
            Yaml yaml = new Yaml(options);

            return yaml.dump(obj); // Convert Java object to YAML string
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
