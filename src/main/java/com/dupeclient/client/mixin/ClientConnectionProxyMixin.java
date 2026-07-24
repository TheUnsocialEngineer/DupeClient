package com.dupeclient.client.mixin;

import com.dupeclient.client.multiplayer.ProxyManager;
import com.dupeclient.client.multiplayer.ProxyProfile;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionProxyMixin {
    @Redirect(
        method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/network/NetworkingBackend;Lnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/Bootstrap;"
        ),
        require = 0
    )
    private static Bootstrap dupeclient$wrapProxyHandler(Bootstrap bootstrap, ChannelHandler handler) {
        if (!ProxyManager.INSTANCE.shouldUseProxy()) {
            return bootstrap.handler(handler);
        }
        ProxyProfile proxy = ProxyManager.INSTANCE.getActive().orElse(null);
        if (proxy == null) {
            return bootstrap.handler(handler);
        }
        return bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                InetSocketAddress proxyAddress = new InetSocketAddress(proxy.host(), proxy.port());
                switch (proxy.type()) {
                    case SOCKS4 -> pipeline.addFirst("dupeclient_proxy", new Socks4ProxyHandler(
                        proxyAddress,
                        proxy.username().isEmpty() ? null : proxy.username()
                    ));
                    case SOCKS5 -> pipeline.addFirst("dupeclient_proxy", new Socks5ProxyHandler(
                        proxyAddress,
                        proxy.username().isEmpty() ? null : proxy.username(),
                        proxy.password().isEmpty() ? null : proxy.password()
                    ));
                }
                if (handler instanceof ChannelInitializer<?> initializer) {
                    invokeInitChannel(initializer, ch);
                } else {
                    pipeline.addLast(handler);
                }
            }
        });
    }

    private static void invokeInitChannel(ChannelInitializer<?> initializer, Channel ch) {
        try {
            Method method = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
            method.setAccessible(true);
            method.invoke(initializer, ch);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
