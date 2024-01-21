package me.pesekjak.notavelocity;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientConfigSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.InitialConnectSessionHandler;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import org.jetbrains.annotations.Nullable;

public enum ClientState {

    LOGIN,
    CONFIG,
    PLAY;

    public static @Nullable ClientState from(MinecraftConnection connection) {
        MinecraftSessionHandler handler = connection.getActiveSessionHandler();
        if (handler == null) return null;
        if (handler instanceof InitialLoginSessionHandler) return LOGIN;
        if (handler instanceof InitialConnectSessionHandler) return LOGIN;
        if (handler instanceof ClientConfigSessionHandler) return CONFIG;
        if (handler instanceof ClientPlaySessionHandler) return PLAY;
        return null;
    }

}
