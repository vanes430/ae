package net.kyori.adventure.text.object;

import java.util.UUID;

/**
 * Stub class for Paper 1.21.11 Adventure integration.
 * Not needed at runtime — only for compilation.
 */
public class PlayerHeadObjectContents {
    public SkinSource skin;
    
    public PlayerHeadObjectContents() {}
    
    public static class SkinSource {
        public UUID uuid;
        public String texture;
        public String signature;
        
        public SkinSource() {}
    }
}
