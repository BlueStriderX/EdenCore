package thederpgamer.edencore.gui.buildsectormenu;

import api.common.GameClient;
import api.common.GameCommon;
import api.network.packets.PacketUtil;
import api.utils.gui.GUIMenuPanel;
import api.utils.gui.SimplePlayerTextInput;
import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.network.client.RequestBuildSectorInvitePacket;
import thederpgamer.edencore.network.client.RequestClientCacheUpdatePacket;
import thederpgamer.edencore.utils.DataUtils;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [10/22/2021]
 */
public class BuildSectorMenuPanel extends GUIMenuPanel {

    public BuildSectorMenuPanel(InputState inputState) {
        super(inputState, "BuildSectorMenu", (int) (GLFrame.getWidth() / 1.5), (int) (GLFrame.getHeight() / 1.5));
    }

    @Override
    public void recreateTabs() {
        DataUtils.getBuildSector(GameClient.getClientPlayerState().getName());
        PacketUtil.sendPacketToServer(new RequestClientCacheUpdatePacket());
        int lastTab = guiWindow.getSelectedTab();
        if(guiWindow.getTabs().size() > 0) guiWindow.clearTabs();

        GUIContentPane managementTab = guiWindow.addTab("MANAGEMENT");
        managementTab.setTextBoxHeightLast((int) (GLFrame.getHeight() / 1.5));
        createManagementTab(managementTab);

        GUIContentPane entitiesTab = guiWindow.addTab("ENTITIES");
        entitiesTab.setTextBoxHeightLast((int) (GLFrame.getHeight() / 1.5));
        createEntitiesTab(entitiesTab);

        GUIContentPane catalogTab = guiWindow.addTab("CATALOG");
        catalogTab.setTextBoxHeightLast((int) (GLFrame.getHeight() / 1.5));
        createCatalogTab(catalogTab);
        guiWindow.setSelectedTab(lastTab);
    }

    private void createManagementTab(GUIContentPane contentPane) {
        contentPane.addDivider(600);
        (new BuildSectorScrollableList(getState(), contentPane.getContent(0, 0), this)).onInit();
        (new BuildSectorUserScrollableList(getState(), contentPane.getContent(1, 0), this)).onInit();
        contentPane.setTextBoxHeight(1, 0, (int) (contentPane.getHeight() - 139));

        contentPane.addNewTextBox(1, 28);
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, contentPane.getContent(1, 1));
        buttonPane.onInit();
        buttonPane.addButton(0, 0, "INVITE", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    (new SimplePlayerTextInput("Enter Username", "") {
                        @Override
                        public boolean onInput(String s) {
                            if(GameCommon.getPlayerFromName(s) != null && !DataUtils.getBuildSector(GameClient.getClientPlayerState().getName()).getAllowedPlayersByName().contains(s)) {
                                PacketUtil.sendPacketToServer(new RequestBuildSectorInvitePacket(s));
                                return true;
                            } else return false;
                        }
                    }).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return !getState().getController().getPlayerInputs().isEmpty();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return getState().getController().getPlayerInputs().isEmpty();
            }
        });
        contentPane.getContent(1, 1).attach(buttonPane);
    }

    private void createEntitiesTab(GUIContentPane contentPane) {
        (new BuildSectorEntitiesScrollableList(getState(), DataUtils.getBuildSector(GameClient.getClientPlayerState().getName()), contentPane.getContent(0), this)).onInit();
    }

    private void createCatalogTab(GUIContentPane contentPane) {
        (new BuildSectorCatalogScrollableList(getState(), DataUtils.getBuildSector(GameClient.getClientPlayerState().getName()), contentPane.getContent(0))).onInit();
    }
}
