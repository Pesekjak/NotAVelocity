package me.pesekjak.notavelocity;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import io.netty.buffer.ByteBuf;

import java.util.function.Supplier;

public class PluginMessagePacketHolder {

    private final MinecraftPacket packet;
    private final Supplier<String> channel;
    private final Supplier<ByteBuf> data;

    public PluginMessagePacketHolder(PluginMessagePacket packet) {
        this.packet = packet;
        channel = packet::getChannel;
        data = packet::content;
    }

    public PluginMessagePacketHolder(LoginPluginMessagePacket packet) {
        this.packet = packet;
        channel = packet::getChannel;
        data = packet::content;
    }

    public String getChannel() {
        return channel.get();
    }

    public ByteBuf getData() {
        return data.get();
    }

    public MinecraftPacket getPacket() {
        return packet;
    }

}
