package de.redstonecloud.api.components.cache;

import com.google.gson.JsonObject;

import java.util.UUID;

public record PlayerData(String name, UUID uuid, String address, String network, String server, JsonObject extraData) {
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("address", address);
        json.addProperty("network", network);
        json.addProperty("server", server);
        json.add("extraData", extraData);
        return json;
    }

    public static PlayerData parse(JsonObject data) {
        return new PlayerData(
                data.get("name").getAsString(),
                UUID.fromString(data.get("uuid").getAsString()),
                data.get("address").getAsString(),
                data.get("network").getAsString(),
                data.get("server").getAsString(),
                data.getAsJsonObject("extraData")
        );
    }
}
