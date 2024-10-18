package thederpgamer.edencore.data.playerdata;

import api.common.GameClient;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.edencore.EdenCore;
import thederpgamer.edencore.data.DataManager;
import thederpgamer.edencore.manager.PlayerActionManager;
import thederpgamer.edencore.network.PlayerActionCommandPacket;

import java.util.*;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerDataManager extends DataManager<PlayerData> {

	private final Set<PlayerData> clientCache = new HashSet<>();
	private static PlayerDataManager instance;

	public static PlayerDataManager getInstance() {
		return instance;
	}

	public static void initialize(boolean client) {
		instance = new PlayerDataManager();
		if(client) instance.requestFromServer();
	}

	@Override
	public Set<PlayerData> getServerCache() {
		List<Object> objects = PersistentObjectUtil.getObjects(EdenCore.getInstance().getSkeleton(), PlayerData.class);
		Set<PlayerData> data = new HashSet<>();
		for(Object object : objects) data.add((PlayerData) object);
		return data;
	}

	@Override
	public Set<PlayerData> getClientCache() {
		return Collections.unmodifiableSet(clientCache);
	}

	@Override
	public void addToClientCache(PlayerData data) {
		clientCache.add(data);
	}

	@Override
	public void removeFromClientCache(PlayerData data) {
		clientCache.remove(data);
	}

	@Override
	public void updateClientCache(PlayerData data) {
		clientCache.remove(data);
		clientCache.add(data);
	}

	public PlayerData getFromName(String name, boolean server) {
		for(PlayerData data : (server ? getServerCache() : getClientCache())) {
			if(data.getName().equals(name)) return data;
		}
		return null;
	}

	public Set<PlayerData> getFactionMembers(Faction faction) {
		return getFactionMembers(faction.getIdFaction());
	}

	public Set<PlayerData> getFactionMembers(int factionId) {
		Set<PlayerData> members = new HashSet<>();
		for(PlayerData data : getServerCache()) {
			if(data.getFactionId() == factionId) members.add(data);
		}
		return members;
	}

	public PlayerData getClientOwnData() {
		return getFromName(GameClient.getClientPlayerState().getName(), false);
	}

	public void setPlayerCredits(PlayerState playerState, long credits) {
		if(playerState.isOnServer()) playerState.setCredits(credits);
		else PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(PlayerActionManager.SET_CREDITS, playerState.getName(), String.valueOf(credits)));
	}
}
