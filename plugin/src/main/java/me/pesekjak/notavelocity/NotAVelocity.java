package me.pesekjak.notavelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.client.*;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

@Plugin(
        id = "notavelocity",
        name = "NotAVelocity",
        version = "1.0",
        description = "Plugin that removes Velocity watermark from server's brand",
        authors = "pesekjak"
)
public class NotAVelocity {

    private static final String VELOCITY_HANDLER = "minecraft-encoder";
    private static final String NOT_ON_VELOCITY_HANDLER = "not-on-velocity";
    private static final int LOGIN_PLUGIN_ID = 159623; // just a random number for this plugin

    private final String suffix;

    @Inject
    public NotAVelocity(ProxyServer server) {
        suffix = " (" + server.getVersion().getName() + ")";
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!(event.getPlayer() instanceof ConnectedPlayer player)) return;
        ProtocolVersion protocolVersion = player.getProtocolVersion();

        int loginID = getPacketID(StateRegistry.LOGIN, ProtocolUtils.Direction.CLIENTBOUND, protocolVersion, new LoginPluginMessagePacket());
        int configID = getPacketID(StateRegistry.CONFIG, ProtocolUtils.Direction.CLIENTBOUND, protocolVersion, new PluginMessagePacket());
        int playID = getPacketID(StateRegistry.PLAY, ProtocolUtils.Direction.CLIENTBOUND, protocolVersion, new PluginMessagePacket());

        Channel channel = player.getConnection().getChannel();

        channel.eventLoop().submit(() -> channel.pipeline().addBefore(VELOCITY_HANDLER, NOT_ON_VELOCITY_HANDLER, new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                ByteBuf buf = ((ByteBuf) msg).copy();
                int packetID = ProtocolUtils.readVarInt(buf);
                if (packetID != loginID && packetID != configID && packetID != playID) {
                    super.write(ctx, msg, promise);
                    return;
                }

                @Nullable ClientState state = ClientState.from(player.getConnection());
                if (state == null
                        || (state == ClientState.LOGIN && packetID != loginID)
                        || (state == ClientState.CONFIG && packetID != configID)
                        || (state == ClientState.PLAY && packetID != playID)) {
                    super.write(ctx, msg, promise);
                    return;
                }

                PluginMessagePacketHolder holder;
                if (state == ClientState.LOGIN)
                    holder = new PluginMessagePacketHolder(new LoginPluginMessagePacket());
                else
                    holder = new PluginMessagePacketHolder(new PluginMessagePacket());
                holder.getPacket().decode(buf, ProtocolUtils.Direction.CLIENTBOUND, protocolVersion);

                if (!isBrandChannel(holder.getChannel())) {
                    super.write(ctx, msg, promise);
                    return;
                }

                String brand = removeSuffix(PluginMessageUtil.readBrandMessage(holder.getData()));

                ByteBuf rewritten = Unpooled.buffer();
                if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_8))
                    ProtocolUtils.writeString(rewritten, brand);
                else
                    rewritten.writeCharSequence(brand, StandardCharsets.UTF_8);

                MinecraftPacket packet;
                if (state == ClientState.LOGIN)
                    packet = new LoginPluginMessagePacket(LOGIN_PLUGIN_ID, holder.getChannel(), rewritten);
                else
                    packet = new PluginMessagePacket(holder.getChannel(), rewritten);

                ByteBuf encoded = Unpooled.buffer();
                ProtocolUtils.writeVarInt(encoded, packetID);
                packet.encode(encoded, ProtocolUtils.Direction.CLIENTBOUND, protocolVersion);

                super.write(ctx, encoded, promise);
            }
        }));
    }

    private int getPacketID(StateRegistry registry, ProtocolUtils.Direction direction, ProtocolVersion version, MinecraftPacket packet) {
        try {
            return registry.getProtocolRegistry(direction, version).getPacketId(packet);
        } catch (Exception exception) {
            return -1;
        }
    }

    private boolean isBrandChannel(String channel) {
        PluginMessagePacket dummy = new PluginMessagePacket(channel, Unpooled.buffer());
        return PluginMessageUtil.isMcBrand(dummy);
    }

    private String removeSuffix(String brand) {
        if (brand.endsWith(suffix))
            brand = brand.subSequence(0, brand.length() - suffix.length()).toString();
        return brand;
    }

}
