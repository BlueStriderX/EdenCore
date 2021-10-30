package thederpgamer.edencore;

import api.common.GameClient;
import api.common.GameCommon;
import api.common.GameServer;
import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.block.*;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.listener.events.draw.RegisterWorldDrawersEvent;
import api.listener.events.entity.SegmentControllerInstantiateEvent;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerPickupFreeItemEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import api.utils.game.PlayerUtils;
import api.utils.game.inventory.InventoryUtils;
import api.utils.gui.ModGUIHandler;
import org.apache.commons.io.IOUtils;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import org.schema.schine.resource.ResourceLoader;
import thederpgamer.edencore.commands.*;
import thederpgamer.edencore.data.other.BuildSectorData;
import thederpgamer.edencore.data.other.PlayerData;
import thederpgamer.edencore.drawer.BuildSectorHudDrawer;
import thederpgamer.edencore.element.ElementManager;
import thederpgamer.edencore.element.items.PrizeBars;
import thederpgamer.edencore.gui.buildsectormenu.BuildSectorMenuControlManager;
import thederpgamer.edencore.gui.exchangemenu.ExchangeMenuControlManager;
import thederpgamer.edencore.manager.ConfigManager;
import thederpgamer.edencore.manager.LogManager;
import thederpgamer.edencore.manager.ResourceManager;
import thederpgamer.edencore.manager.TransferManager;
import thederpgamer.edencore.navigation.EdenMapDrawer;
import thederpgamer.edencore.navigation.MapIcon;
import thederpgamer.edencore.navigation.NavigationUtilManager;
import thederpgamer.edencore.network.client.*;
import thederpgamer.edencore.network.server.SendCacheUpdatePacket;
import thederpgamer.edencore.utils.DataUtils;
import thederpgamer.edencore.utils.DateUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Main class for EdenCore mod.
 *
 * @author TheDerpGamer
 * @version 1.0 - [06/27/2021]
 */
public class EdenCore extends StarMod {

    //Instance
    private static EdenCore getInstance;
    public static EdenCore getInstance() {
        return getInstance;
    }
    public EdenCore() { }
    public static void main(String[] args) { }

    //Other
    private final String[] overwriteClasses = new String[] {
            "PlayerState",
            "CatalogExtendedPanel",
            "BlueprintEntry"
    };

    //GUI
    public ExchangeMenuControlManager exchangeMenuControlManager;
    public BuildSectorMenuControlManager buildSectorMenuControlManager;

    @Override
    public void onEnable() {
        getInstance = this;
        ConfigManager.initialize(this);
        LogManager.initialize();
        TransferManager.initialize();

        registerPackets();
        registerListeners();
        registerCommands();
        startRunners();
    }

    @Override
    public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
        super.onClientCreated(clientInitializeEvent);
        new EdenMapDrawer();
    }

    @Override
    public void onServerCreated(ServerInitializeEvent serverInitializeEvent) {
        new NavigationUtilManager(); //util to have public saved coordinates
        super.onServerCreated(serverInitializeEvent);
    }

    @Override
    public byte[] onClassTransform(String className, byte[] byteCode) {
        for(String name : overwriteClasses) if(className.endsWith(name)) return overwriteClass(className, byteCode);
        return super.onClassTransform(className, byteCode);
    }

    @Override
    public void onResourceLoad(ResourceLoader resourceLoader) {
        ResourceManager.loadResources(resourceLoader);
        MapIcon.loadSprites();
    }

    @Override
    public void onBlockConfigLoad(BlockConfig blockConfig) {
        //Items
        ElementManager.addItemGroup(new PrizeBars());

        ElementManager.initialize();
    }

    private void registerPackets() {
        PacketUtil.registerPacket(RequestClientCacheUpdatePacket.class);
        PacketUtil.registerPacket(RequestMoveToBuildSectorPacket.class);
        PacketUtil.registerPacket(RequestMoveFromBuildSectorPacket.class);
        PacketUtil.registerPacket(RequestBuildSectorInvitePacket.class);
        PacketUtil.registerPacket(RequestBuildSectorKickPacket.class);
        PacketUtil.registerPacket(RequestSpawnEntryPacket.class);
        PacketUtil.registerPacket(ExchangeItemCreatePacket.class);
        PacketUtil.registerPacket(ExchangeItemRemovePacket.class);
        PacketUtil.registerPacket(SendCacheUpdatePacket.class);
        PacketUtil.registerPacket(NavigationMapPacket.class);
    }

    private void registerListeners() {
        StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
            @Override
            public void onEvent(KeyPressEvent event) {
                char buildSectorKey = ConfigManager.getKeyBinding("build-sector-key");
                if(buildSectorKey != '\0' && event.getChar() == buildSectorKey) {
                    if(buildSectorMenuControlManager == null) {
                        buildSectorMenuControlManager = new BuildSectorMenuControlManager();
                        ModGUIHandler.registerNewControlManager(getSkeleton(), buildSectorMenuControlManager);
                    }

                    if(!GameClient.getClientState().getController().isChatActive() && GameClient.getClientState().getController().getPlayerInputs().isEmpty()) {
                        GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                        GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
                        buildSectorMenuControlManager.setActive(true);
                    }
                }

                char exchangeKey = ConfigManager.getKeyBinding("exchange-menu-key");
                if(exchangeKey != '\0' && event.getChar() == exchangeKey) {
                    if(exchangeMenuControlManager == null) {
                        exchangeMenuControlManager = new ExchangeMenuControlManager();
                        ModGUIHandler.registerNewControlManager(getSkeleton(), exchangeMenuControlManager);
                    }

                    if(!GameClient.getClientState().getController().isChatActive() && GameClient.getClientState().getController().getPlayerInputs().isEmpty()) {
                        GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                        GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
                        exchangeMenuControlManager.setActive(true);
                    }
                }
            }
        }, this);

        StarLoader.registerListener(GUITopBarCreateEvent.class, new Listener<GUITopBarCreateEvent>() {
            @Override
            public void onEvent(final GUITopBarCreateEvent event) {
                if(buildSectorMenuControlManager == null) {
                    buildSectorMenuControlManager = new BuildSectorMenuControlManager();
                    ModGUIHandler.registerNewControlManager(getSkeleton(), buildSectorMenuControlManager);
                }

                if(exchangeMenuControlManager == null) {
                    exchangeMenuControlManager = new ExchangeMenuControlManager();
                    ModGUIHandler.registerNewControlManager(getSkeleton(), exchangeMenuControlManager);
                }

                GUITopBar.ExpandedButton dropDownButton = event.getDropdownButtons().get(event.getDropdownButtons().size() - 1);

                dropDownButton.addExpandedButton("BUILD SECTOR", new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                            GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
                            buildSectorMenuControlManager.setActive(true);
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return false;
                    }

                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });

                dropDownButton.addExpandedButton("EXCHANGE", new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                            GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
                            exchangeMenuControlManager.setActive(true);
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return false;
                    }

                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });
            }
        }, this);

        StarLoader.registerListener(RegisterWorldDrawersEvent.class, new Listener<RegisterWorldDrawersEvent>() {
            @Override
            public void onEvent(RegisterWorldDrawersEvent event) {
                event.getModDrawables().add(new BuildSectorHudDrawer());
            }
        }, this);

        StarLoader.registerListener(SegmentControllerInstantiateEvent.class, new Listener<SegmentControllerInstantiateEvent>() {
            @Override
            public void onEvent(final SegmentControllerInstantiateEvent event) {
                new StarRunnable() {
                    @Override
                    public void run() {
                        try {
                            if(event.getController().getSector(new Vector3i()).x > 100000000 || event.getController().getSector(new Vector3i()).y > 100000000 || event.getController().getSector(new Vector3i()).z > 100000000) {
                                updateClientCacheData();
                            }
                        } catch(Exception ignored) { }
                    }
                }.runLater(getInstance(), 3);
            }
        }, this);

        StarLoader.registerListener(SegmentPieceAddEvent.class, new Listener<SegmentPieceAddEvent>() {
            @Override
            public void onEvent(SegmentPieceAddEvent event) {
                try {
                    if(DataUtils.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
                        BuildSectorData sectorData = DataUtils.getPlayerCurrentBuildSector(GameClient.getClientPlayerState());
                        if(!sectorData.hasPermission(GameClient.getClientPlayerState().getName(), "EDIT")) event.setCanceled(true);
                    }
                } catch(Exception ignored) { }
            }
        }, this);

        StarLoader.registerListener(SegmentPieceRemoveEvent.class, new Listener<SegmentPieceRemoveEvent>() {
            @Override
            public void onEvent(SegmentPieceRemoveEvent event) {
                try {
                    if(DataUtils.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
                        BuildSectorData sectorData = DataUtils.getPlayerCurrentBuildSector(GameClient.getClientPlayerState());
                        if(!sectorData.hasPermission(GameClient.getClientPlayerState().getName(), "EDIT")) event.setCanceled(true);
                    }
                } catch(Exception ignored) { }
            }
        }, this);

        StarLoader.registerListener(SegmentPieceActivateByPlayer.class, new Listener<SegmentPieceActivateByPlayer>() {
            @Override
            public void onEvent(SegmentPieceActivateByPlayer event) {
                try {
                    if(DataUtils.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
                        BuildSectorData sectorData = DataUtils.getPlayerCurrentBuildSector(GameClient.getClientPlayerState());
                        if(!sectorData.hasPermission(GameClient.getClientPlayerState().getName(), "EDIT")) event.setCanceled(true);
                    }
                } catch(Exception ignored) { }
            }
        }, this);

        StarLoader.registerListener(SegmentPieceModifyOnClientEvent.class, new Listener<SegmentPieceModifyOnClientEvent>() {
            @Override
            public void onEvent(SegmentPieceModifyOnClientEvent event) {
                try {
                    if(DataUtils.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
                        BuildSectorData sectorData = DataUtils.getPlayerCurrentBuildSector(GameClient.getClientPlayerState());
                        if(!sectorData.hasPermission(GameClient.getClientPlayerState().getName(), "EDIT")) event.setCanceled(true);
                    }
                } catch(Exception ignored) { }
            }
        }, this);

        StarLoader.registerListener(ClientSelectSegmentPieceEvent.class, new Listener<ClientSelectSegmentPieceEvent>() {
            @Override
            public void onEvent(ClientSelectSegmentPieceEvent event) {
                try {
                    if(DataUtils.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
                        BuildSectorData sectorData = DataUtils.getPlayerCurrentBuildSector(GameClient.getClientPlayerState());
                        if(!sectorData.hasPermission(GameClient.getClientPlayerState().getName(), "EDIT")) event.setCanceled(true);
                    }
                } catch(Exception ignored) { }
            }
        }, this);

        StarLoader.registerListener(ClientSegmentPieceConnectionChangeEvent.class, new Listener<ClientSegmentPieceConnectionChangeEvent>() {
            @Override
            public void onEvent(ClientSegmentPieceConnectionChangeEvent event) {
                try {
                    if(DataUtils.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
                        BuildSectorData sectorData = DataUtils.getPlayerCurrentBuildSector(GameClient.getClientPlayerState());
                        if(!sectorData.hasPermission(GameClient.getClientPlayerState().getName(), "EDIT")) event.setCanceled(true);
                    }
                } catch(Exception ignored) { }
            }
        }, this);

        StarLoader.registerListener(PlayerPickupFreeItemEvent.class, new Listener<PlayerPickupFreeItemEvent>() {
            @Override
            public void onEvent(PlayerPickupFreeItemEvent event) {
                try {
                    if(DataUtils.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
                        BuildSectorData sectorData = DataUtils.getPlayerCurrentBuildSector(GameClient.getClientPlayerState());
                        if(!sectorData.hasPermission(GameClient.getClientPlayerState().getName(), "PICKUP")) event.setCanceled(true);
                    }
                } catch(Exception ignored) { }
            }
        }, this);

        StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
            @Override
            public void onEvent(PlayerDeathEvent event) {
                if(DataUtils.isPlayerInAnyBuildSector(event.getPlayer())) queueSpawnSwitch(event.getPlayer());
            }
        }, this);

        StarLoader.registerListener(PlayerSpawnEvent.class, new Listener<PlayerSpawnEvent>() {
            @Override
            public void onEvent(PlayerSpawnEvent event) {
                //if(DataUtils.isPlayerInAnyBuildSector(event.getPlayer().getOwnerState())) queueSpawnSwitch(event.getPlayer().getOwnerState());
            }
        }, this);

        StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
            @Override
            public void onEvent(final PlayerJoinWorldEvent event) {
                if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
                    new StarRunnable() {
                        @Override
                        public void run() {
                            try {
                                PacketUtil.sendPacket(GameServer.getServerState().getPlayerFromName(event.getPlayerName()), new SendCacheUpdatePacket(GameServer.getServerState().getPlayerFromName(event.getPlayerName())));
                            } catch(PlayerNotFountException exception) {
                                exception.printStackTrace();
                            }
                        }
                    }.runLater(EdenCore.this, 100);

                    new StarRunnable() {
                        @Override
                        public void run() {
                            PlayerState playerState = GameCommon.getPlayerFromName(event.getPlayerName());
                            PlayerData playerData = DataUtils.getPlayerData(playerState);
                            Date date = new Date(playerData.lastDailyPrizeClaim);
                            if(DateUtils.getAgeDays(date) >= 1.0f) {
                                InventoryUtils.addItem(playerState.getInventory(), ElementManager.getItem("Bronze Bar").getId(), 2);
                                playerData.lastDailyPrizeClaim = System.currentTimeMillis();
                                PersistentObjectUtil.save(EdenCore.this.getSkeleton());
                                PlayerUtils.sendMessage(playerState, "You have been given 2 Bronze Bars for logging in. Thanks for playing!");
                            }
                        }
                    }.runLater(EdenCore.this, 10000);
                }
            }
        }, this);
    }

    private void registerCommands() {
        StarLoader.registerCommand(new SaveEntityCommand());
        StarLoader.registerCommand(new LoadEntityCommand());
        StarLoader.registerCommand(new ListEntityCommand());
        StarLoader.registerCommand(new BuildSectorCommand());
        StarLoader.registerCommand(new AwardBarsCommand());
        StarLoader.registerCommand(new BankingSendMoneyCommand());
        StarLoader.registerCommand(new BankingListCommand());
        StarLoader.registerCommand(new BankingAdminListCommand());
    }

    private void startRunners() {
        if(GameCommon.isOnSinglePlayer() || GameCommon.isDedicatedServer()) {
            new StarRunnable() {
                @Override
                public void run() {
                    updateClientCacheData();
                }
            }.runTimer(this, 1000);
        }
    }

    private void queueSpawnSwitch(final PlayerState playerState) {
        new StarRunnable() {
            @Override
            public void run() {
                if(!DataUtils.isPlayerInAnyBuildSector(playerState)) cancel();
                if(!playerState.hasSpawnWait) { //Wait until player has spawned, then warp them
                    try {
                        DataUtils.movePlayerFromBuildSector(playerState);
                    } catch(Exception exception) {
                        LogManager.logException("Encountered a severe exception while trying to move player \"" + playerState.getName() + "\" out of a build sector! Report this ASAP!", exception);
                        playerState.setUseCreativeMode(false);
                        if(!playerState.isAdmin()) playerState.setHasCreativeMode(false);
                        PlayerUtils.sendMessage(playerState, "The server encountered a severe exception while trying to load you in and your player state may be corrupted as a result. Report this to an admin ASAP!");
                    }
                    cancel();
                }
            }
        }.runTimer(this, 50);
    }

    private byte[] overwriteClass(String className, byte[] byteCode) {
        byte[] bytes = null;
        try {
            ZipInputStream file = new ZipInputStream(new FileInputStream(this.getSkeleton().getJarFile()));
            while(true) {
                ZipEntry nextEntry = file.getNextEntry();
                if(nextEntry == null) break;
                if(nextEntry.getName().endsWith(className + ".class")) bytes = IOUtils.toByteArray(file);
            }
            file.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        if(bytes != null) return bytes;
        else return byteCode;
    }

    public void updateClientCacheData() {
        if(GameCommon.isOnSinglePlayer() || GameCommon.isDedicatedServer()) {
            try {
                for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                    PacketUtil.sendPacket(playerState, new SendCacheUpdatePacket(playerState));
                }
            } catch(Exception ignored) { }
        }
    }
}
