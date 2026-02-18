package luma.hunt.tunnel;

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import luma.hunt.LumaHunt;

import java.util.function.Consumer;

/**
 * Manages the e4mc tunnel lifecycle for LumaHunt.
 * Provides public domain for LAN games without port forwarding.
 */
public class TunnelManager {
    private static TunnelManager instance;
    
    private TunnelSession currentSession;
    private String currentDomain;
    
    public static TunnelManager getInstance() {
        if (instance == null) {
            instance = new TunnelManager();
        }
        return instance;
    }
    
    private TunnelManager() {}

    public void startTunnel(ChannelHandler childHandler, EventLoopGroup group, Consumer<String> onDomainReady) {
        if (currentSession != null && currentSession.state == TunnelSession.State.STARTED) {
            if (currentDomain != null && onDomainReady != null) {
                onDomainReady.accept(currentDomain);
            }
            return;
        }
        
        LumaHunt.LOGGER.info("Starting tunnel session...");
        
        currentSession = new TunnelSession(childHandler, group);
        currentSession.setOnDomainAssigned(domain -> {
            currentDomain = domain;
            LumaHunt.LOGGER.info("Tunnel domain ready: {}", domain);
            if (onDomainReady != null) {
                onDomainReady.accept(domain);
            }
        });
        currentSession.setOnError(error -> {
            LumaHunt.LOGGER.error("Tunnel failed", error);
            currentDomain = null;
        });
        
        currentSession.startAsync();
    }

    public void stopTunnel() {
        if (currentSession != null) {
            LumaHunt.LOGGER.info("Stopping tunnel session...");
            currentSession.stop();
            currentSession = null;
            currentDomain = null;
        }
    }

    public String getCurrentDomain() {
        return currentDomain;
    }

    public boolean isActive() {
        return currentSession != null && currentSession.state == TunnelSession.State.STARTED;
    }

    public TunnelSession.State getState() {
        return currentSession != null ? currentSession.state : TunnelSession.State.STOPPED;
    }
}
