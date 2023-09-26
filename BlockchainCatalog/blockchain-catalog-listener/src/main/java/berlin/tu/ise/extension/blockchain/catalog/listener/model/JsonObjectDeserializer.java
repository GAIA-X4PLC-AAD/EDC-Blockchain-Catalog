package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.io.StringReader;

public class JsonObjectDeserializer extends JsonDeserializer<JsonObject> {

    @Override
    public JsonObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try (JsonReader reader = Json.createReader(new StringReader(p.readValueAsTree().toString()))) {
            return reader.readObject();
        }
    }
}
