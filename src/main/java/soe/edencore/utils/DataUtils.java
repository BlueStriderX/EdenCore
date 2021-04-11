package soe.edencore.utils;

import api.common.GameClient;
import api.common.GameCommon;
import soe.edencore.EdenCore;

/**
 * DataUtils
 * <Description>
 *
 * @author Garret Reichenbach
 * @since 04/10/2021
 */
public class DataUtils {

    public static String getResourcesPath() {
        return EdenCore.instance.getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
    }

    public static String getWorldDataPath() {
        String universeName = GameCommon.getUniqueContextId();
        if(!universeName.contains(":")) {
            return getResourcesPath() + "/data/" + universeName;
        } else {
            try {
                LogUtils.logMessage(MessageType.ERROR, "Client " + GameClient.getClientPlayerState().getName() + " attempted to illegally access server data.");
            } catch(Exception ignored) { }
            return null;
        }
    }
}
