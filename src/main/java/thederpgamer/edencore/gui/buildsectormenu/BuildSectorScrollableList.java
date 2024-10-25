package thederpgamer.edencore.gui.buildsectormenu;

import api.common.GameClient;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.buildsectordata.BuildSectorData;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;

import java.util.Collection;
import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class BuildSectorScrollableList extends ScrollableTableList<BuildSectorData> implements GUIActiveInterface {

	private final GUIElement parent;
	
	public BuildSectorScrollableList(InputState state, GUIElement parent) {
		super(state, 100, 100, parent);
		this.parent = parent;
	}

	@Override
	protected Collection<BuildSectorData> getElementList() {
		return BuildSectorDataManager.getInstance().getAccessibleSectors(GameClient.getClientPlayerState());
	}

	@Override
	public void initColumns() {
		addColumn("Owner", 5.0f, (o1, o2) -> {
			if(o1.getOwner().equals(GameClient.getClientPlayerState().getName())) return -1;
			else return o1.getOwner().compareTo(o2.getOwner());
		});
		activeSortColumnIndex = 0;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<BuildSectorData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(BuildSectorData buildSectorData : set) {
			GUIClippedRow ownerRow = getSimpleRow(buildSectorData.getOwner(), this);
			BuildSectorScrollableListRow row = new BuildSectorScrollableListRow(getState(), buildSectorData, ownerRow);
			guiElementList.add(row);
			GUIAncor anchor = new GUIAncor(getState(), parent.getWidth() - 107.0f, 28.0f) {
				@Override
				public void draw() {
					setWidth(parent.getWidth() - 107.0f);
					super.draw();
				}
			};
			GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, anchor);
			buttonPane.onInit();
			BuildSectorData currentSector = BuildSectorDataManager.getInstance().getCurrentBuildSector(GameClient.getClientPlayerState());
			if(currentSector == null || currentSector != buildSectorData) {
				buttonPane.addButton(0, 0, "WARP", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							if(currentSector != null) BuildSectorDataManager.getInstance().leaveBuildSector(GameClient.getClientPlayerState());
							BuildSectorDataManager.getInstance().enterBuildSector(GameClient.getClientPlayerState(), buildSectorData);
						}
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return true;
					}
				});
			} else {
				buttonPane.addButton(0, 0, "LEAVE", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) BuildSectorDataManager.getInstance().leaveBuildSector(GameClient.getClientPlayerState());
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationCallback() {
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
			anchor.attach(buttonPane);
			row.expanded = new GUIElementList(getState());
			row.expanded.add(new GUIListElement(anchor, getState()));
			row.onInit();
			guiElementList.addWithoutUpdate(row);
		}
		guiElementList.updateDim();
	}

	public class BuildSectorScrollableListRow extends ScrollableTableList<BuildSectorData>.Row {

		public BuildSectorScrollableListRow(InputState state, BuildSectorData data, GUIElement... elements) {
			super(state, data, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}