package de.redstonecloud.api.redis.broker;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.message.Message;
import de.redstonecloud.api.redis.broker.packet.Packet;
import de.redstonecloud.api.redis.broker.packet.PacketRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
public class Broker {
    public static final Gson GSON = new Gson();

    protected static Broker instance;

    public static Broker get() {
        return instance;
    }

    protected PacketRegistry packetRegistry;

    protected String mainRoute;
    protected Jedis subscriber;
    protected JedisPool pool;

    protected Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<Packet>>> packetConsumers;
    protected Int2ObjectOpenHashMap<ResponseContainer<?>> pendingPacketResponses;

    protected Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<Message>>> messageConsumers;
    protected Int2ObjectOpenHashMap<Consumer<Message>> pendingMessageResponses;

    private final ExecutorService publishExecutor = Executors.newFixedThreadPool(8);

    private boolean running = false;

    public Broker(String mainRoute, PacketRegistry packetRegistry, String... routes) {
        Preconditions.checkArgument(instance == null, "Broker already initialized");
        Preconditions.checkArgument(routes.length > 0, "Routes should not be empty");
        instance = this;

        this.mainRoute = mainRoute;

        this.packetRegistry = packetRegistry;

        this.packetConsumers = new Object2ObjectOpenHashMap<>();
        this.pendingPacketResponses = new Int2ObjectOpenHashMap<>();

        this.messageConsumers = new Object2ObjectOpenHashMap<>();
        this.pendingMessageResponses = new Int2ObjectOpenHashMap<>();

        initJedis(routes);
    }

    private void initJedis(String... routes) {
        String address = System.getenv("REDIS_IP") != null ? System.getenv("REDIS_IP") : System.getProperty("redis.bind");
        int port = Integer.parseInt(System.getenv("REDIS_PORT") != null ? System.getenv("REDIS_PORT") : System.getProperty("redis.port"));
        int db = Integer.parseInt(System.getenv("REDIS_DB") != null ? System.getenv("REDIS_DB") : System.getProperty("redis.db"));

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(4);
        config.setMaxIdle(8);
        config.setMaxTotal(16);
        config.setBlockWhenExhausted(true);
        config.setTestOnBorrow(true);
        config.setMaxWait(Duration.ofSeconds(1));
        config.setTestOnReturn(true);

        this.pool = new JedisPool(config, address, port, 0, null, db);

        running = true;

        new Thread(() -> {
            while (running) { // Keep the subscriber alive
                try (Jedis jedis = new Jedis(address, port, 0)) { // Use try-with-resources for safe closing
                    jedis.select(db); // Select the correct database

                    this.subscriber = jedis; // Save the subscriber instance if needed elsewhere
                    System.out.println("Connecting to Redis...");

                    jedis.subscribe(new BrokerJedisPubSub(), routes);

                    System.out.println("Subscribed to: " + String.join(", ", routes));
                } catch (Exception e) {
                    // Log the exception for debugging purposes
                    System.err.println("Subscriber error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void publish(Packet packet) {
        publishExecutor.submit(() -> {
            try (Jedis publisher = this.pool.getResource()) {
                publisher.publish(packet.getTo().toLowerCase(), packet.finalDocument().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void publish(Message message) {
        publishExecutor.submit(() -> {
            try (Jedis publisher = this.pool.getResource()) {
                publisher.publish(message.getTo().toLowerCase(), message.toJson());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void listen(String channel, Consumer<Packet> callback) {
        this.packetConsumers.computeIfAbsent(channel, k -> new ObjectArrayList<>()).add(callback);
    }

    public void listenM(String channel, Consumer<Message> callback) {
        this.messageConsumers.computeIfAbsent(channel, k -> new ObjectArrayList<>()).add(callback);
    }

    public void shutdown() {
        running = false;
        this.pool.close();
        this.subscriber.close();
    }

    public void addPendingResponse(int id, ResponseContainer<?> callback) {
        Preconditions.checkArgument(!this.pendingPacketResponses.containsKey(id), "A message with the same id is already waiting for a response");
        this.pendingPacketResponses.put(id, callback);
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() ->
                Optional.ofNullable(this.pendingPacketResponses.remove(id))
                        .ifPresent(responseContainer -> responseContainer.consumer().accept(null)));
    }

    public void addPendingResponse(int id, Consumer<Message> callback) {
        Preconditions.checkArgument(!this.pendingMessageResponses.containsKey(id), "A message with the same id is already waiting for a response");
        this.pendingMessageResponses.put(id, callback);
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() ->
                Optional.ofNullable(this.pendingMessageResponses.remove(id))
                        .ifPresent(consumer -> consumer.accept(null)));
    }

    @SuppressWarnings("unchecked")
    private class BrokerJedisPubSub extends JedisPubSub {
        @Override
        public void onMessage(String channel, String messageString) {
            JsonArray array = GSON.fromJson(messageString, JsonArray.class);

            String type = array.get(0).getAsString();

            switch (type) {
                case "packet" -> {
                    Packet packet = packetRegistry.create(array);

                    if (packet == null) {
                        System.out.println("[BROKER] Received invalid packet: " + messageString);
                        return;
                    }

                    Optional.ofNullable(pendingPacketResponses.remove(packet.getSessionId()))
                            .ifPresent(responseContainer -> {
                                Consumer<? extends Packet> consumer = responseContainer.consumer();
                                Class<? extends Packet> packetClass = responseContainer.packetClass();

                                if (packetClass.isInstance(packet))
                                    ((Consumer<Packet>) consumer).accept(packetClass.cast(packet));
                            });

                    packetConsumers.getOrDefault(channel, new ObjectArrayList<>())
                            .forEach(consumer -> consumer.accept(packet));

                    packetConsumers.getOrDefault("", new ObjectArrayList<>())
                            .forEach(consumer -> consumer.accept(packet));
                }
                case "message" -> {
                    Message message = Message.fromJson(array);

                    Optional.ofNullable(pendingMessageResponses.remove(message.getId()))
                            .ifPresent(consumer -> consumer.accept(message));

                    messageConsumers.getOrDefault(channel, new ObjectArrayList<>())
                            .forEach(consumer -> consumer.accept(message));

                    messageConsumers.getOrDefault("", new ObjectArrayList<>())
                            .forEach(consumer -> consumer.accept(message));
                }
                default -> System.out.println("[BROKER] Received unknown message type " + type);
            }
        }
    }
}
