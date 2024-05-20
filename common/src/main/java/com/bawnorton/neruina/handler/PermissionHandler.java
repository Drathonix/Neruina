package com.bawnorton.neruina.handler;

import com.bawnorton.neruina.Neruina;
import com.bawnorton.neruina.config.Config;
import com.bawnorton.neruina.handler.NeruinaTickHandler;
import com.bawnorton.neruina.thread.ConditionalRunnable;
import com.bawnorton.neruina.version.Version;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;

import java.util.concurrent.ExecutionException;

public class PermissionHandler {
    private static final String ADMIN_LOGGING_NODE = "neruina.admin.logs";

    private static LuckPerms lp = null;
    static {
        try{
            lp = LuckPermsProvider.get();
        } catch (Throwable ignored){}
    }

    public static boolean hasPermission(PlayerEntity player, String node){
        if(lp == null){
            return player.hasPermissionLevel(4);
        }
        else{
            User user = lp.getUserManager().loadUser(player.getUuid()).join();
            if(user != null){
                return user.getNodes().stream()
                        .filter(NodeType.PERMISSION::matches)
                        .map(NodeType.PERMISSION::cast)
                        .filter(PermissionNode::getValue)
                        .map(PermissionNode::getPermission)
                        .anyMatch(str->str.equals(node));
            }
        }
        return false;
    }

    public static void informAdmins(Text message){
        if (!Config.getInstance().broadcastErrors) return;
        PlayerManager playerManager = NeruinaTickHandler.server.getPlayerManager();
        ConditionalRunnable.create(() -> playerManager.getPlayerList().forEach(player ->{
            if(hasPermission(player,ADMIN_LOGGING_NODE)) {
                player.sendMessage(Version.formatText(message), false);
            }
        }), () -> playerManager.getCurrentPlayerCount() >= 1);
    }
}
