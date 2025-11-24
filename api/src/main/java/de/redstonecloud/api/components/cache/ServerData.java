package de.redstonecloud.api.components.cache;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.UUID;

public record ServerData(String name, UUID uuid, String template, String status, String serverType, int port, boolean proxy, JsonArray connectedPlayers, JsonObject extraData) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("template", template);
        json.addProperty("status", status);
        json.addProperty("serverType", serverType);
        json.addProperty("port", port);
        json.addProperty("proxy", proxy);
        json.add("connectedPlayers", connectedPlayers);
        json.add("extraData", extraData);
        return json;
    }

    public static ServerData parse(JsonObject json) {
        return new ServerData(
                json.get("name").getAsString(),
                UUID.fromString(json.get("uuid").getAsString()),
                json.get("template").getAsString(),
                json.get("status").getAsString(),
                json.get("serverType").getAsString(),
                json.get("port").getAsInt(),
                json.get("proxy").getAsBoolean(),
                json.getAsJsonArray("connectedPlayers"),
                json.getAsJsonObject("extraData")
        );
    }
}
