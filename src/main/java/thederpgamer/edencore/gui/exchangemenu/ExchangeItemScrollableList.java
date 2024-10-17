package thederpgamer.edencore.gui.exchangemenu;

import api.common.GameClient;
import api.utils.game.inventory.InventoryUtils;
import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.BlueprintPlayerHandleRequest;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.network.objects.remote.RemoteBlueprintPlayerRequest;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.DataManager;
import thederpgamer.edencore.data.exchangedata.ExchangeData;
import thederpgamer.edencore.data.exchangedata.ExchangeDataManager;
import thederpgamer.edencore.element.ElementManager;

import java.util.*;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class ExchangeItemScrollableList extends ScrollableTableList<ExchangeData> implements GUIActiveInterface {

	private final GUIAncor pane;
	private final int type;

	public ExchangeItemScrollableList(InputState state, GUIAncor pane, int type) {
		super(state, 10, 10, pane);
		this.pane = pane;
		this.type = type;
	}

	@Override
	public void initColumns() {
		addColumn(Lng.str("Name"), 15.0F, Comparator.comparing(ExchangeData::getName));
		addColumn(Lng.str("Producer"), 10.0f, Comparator.comparing(ExchangeData::getProducer));
		addColumn(Lng.str("Price"), 3.0f, Comparator.comparingInt(ExchangeData::getPrice));
		addColumn(Lng.str("Category"), 10.0f, Comparator.comparing(ExchangeData::getCategory));
		addColumn(Lng.str("Mass"), 5.0f, (o1, o2) -> Float.compare(o1.getMass(), o2.getMass()));

		addTextFilter(new GUIListFilterText<ExchangeData>() {
			public boolean isOk(String s, ExchangeData item) {
				return item.getName().toLowerCase().contains(s.toLowerCase());
			}
		}, ControllerElement.FilterRowStyle.FULL);
		addTextFilter(new GUIListFilterText<ExchangeData>() {
			public boolean isOk(String s, ExchangeData item) {
				return item.getProducer().toLowerCase().contains(s.toLowerCase());
			}
		}, ControllerElement.FilterRowStyle.LEFT);
		switch(type) {
			case ExchangeDialog.SHIPS:
				addDropdownFilter(new GUIListFilterDropdown<ExchangeData, BlueprintClassification>(getShipClassifications()) {
					public boolean isOk(BlueprintClassification classification, ExchangeData item) {
						return item.getClassification() == classification;
					}

				}, new CreateGUIElementInterface<BlueprintClassification>() {
					@Override
					public GUIElement create(BlueprintClassification classification) {
						GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
						GUITextOverlayTableDropDown dropDown;
						(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(classification.getName());
						dropDown.setPos(4.0F, 4.0F, 0.0F);
						anchor.setUserPointer(classification.name());
						anchor.attach(dropDown);
						return anchor;
					}

					@Override
					public GUIElement createNeutral() {
						GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
						GUITextOverlayTableDropDown dropDown;
						(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(Lng.str("ALL"));
						dropDown.setPos(4.0F, 4.0F, 0.0F);
						anchor.setUserPointer("ALL");
						anchor.attach(dropDown);
						return anchor;
					}
				}, ControllerElement.FilterRowStyle.RIGHT);
				break;
			case ExchangeDialog.STATIONS:
				addDropdownFilter(new GUIListFilterDropdown<ExchangeData, BlueprintClassification>(BlueprintClassification.stationValues().toArray(getStationClassifications())) {
					public boolean isOk(BlueprintClassification classification, ExchangeData item) {
						return item.getClassification() == classification;
					}

				}, new CreateGUIElementInterface<BlueprintClassification>() {
					@Override
					public GUIElement create(BlueprintClassification classification) {
						GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
						GUITextOverlayTableDropDown dropDown;
						(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(classification.getName());
						dropDown.setPos(4.0F, 4.0F, 0.0F);
						anchor.setUserPointer(classification.name());
						anchor.attach(dropDown);
						return anchor;
					}

					@Override
					public GUIElement createNeutral() {
						GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
						GUITextOverlayTableDropDown dropDown;
						(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(Lng.str("ALL"));
						dropDown.setPos(4.0F, 4.0F, 0.0F);
						anchor.setUserPointer("ALL");
						anchor.attach(dropDown);
						return anchor;
					}
				}, ControllerElement.FilterRowStyle.RIGHT);
				break;
		}
		activeSortColumnIndex = 0;
	}

	@Override
	protected Collection<ExchangeData> getElementList() {
		switch(type) {
			case ExchangeDialog.SHIPS:
				return ExchangeDialog.getShipList();
			case ExchangeDialog.STATIONS:
				return ExchangeDialog.getStationList();
			default:
				return Collections.emptyList();
		}
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<ExchangeData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(ExchangeData data : set) {
			GUIClippedRow nameRow = getSimpleRow(data.getName(), this);
			GUIClippedRow producerRow = getSimpleRow(data.getProducer(), this);
			GUIClippedRow priceRow = getSimpleRow(String.valueOf(data.getPrice()), this);
			GUIClippedRow categoryRow = getSimpleRow(data.getCategory(), this);
			GUIClippedRow massRow = getSimpleRow(StringTools.massFormat(data.getMass()), this);
			ExchangeItemScrollableListRow entryListRow = new ExchangeItemScrollableListRow(getState(), data, nameRow, producerRow, priceRow, categoryRow, massRow);
			GUIAncor anchor = new GUIAncor(getState(), pane.getWidth() - 107.0f, 28.0f) {
				@Override
				public void draw() {
					setWidth(pane.getWidth() - 107.0f);
					super.draw();
				}
			};
			GUIHorizontalButtonTablePane buttonTablePane = redrawButtonPane(data, anchor);
			anchor.attach(buttonTablePane);
			entryListRow.expanded = new GUIElementList(getState());
			GUITextOverlayTableInnerDescription description = new GUITextOverlayTableInnerDescription(10, 10, getState());
			description.onInit();
			description.setTextSimple(data.getDescription());
			entryListRow.expanded.add(new GUIListElement(description, getState()));
			entryListRow.expanded.add(new GUIListElement(anchor, getState()));
//			entryListRow.expanded.attach(anchor);
			entryListRow.onInit();
			guiElementList.add(entryListRow);
		}
		guiElementList.updateDim();
	}

	private GUIHorizontalButtonTablePane redrawButtonPane(ExchangeData data, GUIAncor anchor) {
		boolean isOwner = GameClient.getClientPlayerState().getFactionName().equals(data.getProducer());
		GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), (isOwner ? 2 : 1), 1, anchor);
		buttonPane.onInit();
		if(isOwner) {
			buttonPane.addButton(0, 0, Lng.str("EDIT"), GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new ExchangeDataDialog(data, ExchangeDataDialog.EDIT, Lng.str("Edit Listing"))).activate();
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
			buttonPane.addButton(1, 0, Lng.str("REMOVE"), GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new PlayerOkCancelInput("Confirm", getState(), Lng.str("Confirm"), Lng.str("Do you want to remove this Blueprint?")) {
							@Override
							public void onDeactivate() {

							}

							@Override
							public void pressedOK() {
								ExchangeDataManager.getInstance().sendPacket(data, DataManager.REMOVE_DATA, true);
							}
						}).activate();
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
			buttonPane.addButton(0, 0, Lng.str("BUY"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new PlayerOkCancelInput("Confirm", getState(), Lng.str("Confirm"), Lng.str("Do you want to buy this Blueprint?")) {
							@Override
							public void onDeactivate() {

							}

							@Override
							public void pressedOK() {
								String error = canBuy(data);
								if(error != null) ((GameClientState) getState()).getPlayer().sendServerMessagePlayerError(new Object[]{error});
								else buyBlueprint(data);
							}
						}).activate();
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
		}
		return buttonPane;
	}

	public String canBuy(ExchangeData data) {
		GameClientState state = (GameClientState) getState();
		if(!hasPermission(data)) return "Selected blueprint is not available or you don't have access to it!";
		else {
			Inventory playerInventory = state.getPlayer().getInventory();
			int amount = InventoryUtils.getItemAmount(playerInventory, ElementManager.getItem("Bronze Bar").getId());
			if(amount < data.getPrice()) return "You don't have enough Bronze Bars to buy this blueprint!";
		}
		return null;
	}

	private boolean hasPermission(ExchangeData data) {
		for(CatalogPermission permission : ((GameClientState) getState()).getCatalog().getAvailableCatalog()) {
			if(permission.getUid().equals(data.getCatalogName())) return true;
		}
		return false;
	}

	private void buyBlueprint(ExchangeData data) {
		BlueprintPlayerHandleRequest req = new BlueprintPlayerHandleRequest();
		req.catalogName = data.getCatalogName();
		req.entitySpawnName = "";
		req.save = false;
		req.toSaveShip = -1;
		req.directBuy = true;
		((GameClientState) getState()).getPlayer().getNetworkObject().catalogPlayerHandleBuffer.add(new RemoteBlueprintPlayerRequest(req, false));
		InventoryUtils.consumeItems(((GameClientState) getState()).getPlayer().getInventory(), ElementManager.getItem("Bronze Bar").getId(), data.getPrice());
	}

	private BlueprintClassification[] getShipClassifications() {
		List<BlueprintClassification> classifications = new ArrayList<>();
		for(BlueprintClassification classification : BlueprintClassification.shipValues()) {
			if(classification != BlueprintClassification.NONE && classification != BlueprintClassification.ALL_SHIPS) classifications.add(classification);
		}
		return classifications.toArray(new BlueprintClassification[0]);
	}

	private BlueprintClassification[] getStationClassifications() {
		List<BlueprintClassification> classifications = new ArrayList<>();
		for(BlueprintClassification classification : BlueprintClassification.stationValues()) {
			if(classification != BlueprintClassification.NONE && classification != BlueprintClassification.NONE_STATION) classifications.add(classification);
		}
		return classifications.toArray(new BlueprintClassification[0]);
	}

	public class ExchangeItemScrollableListRow extends ScrollableTableList<ExchangeData>.Row {

		public ExchangeItemScrollableListRow(InputState state, ExchangeData data, GUIElement... elements) {
			super(state, data, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}