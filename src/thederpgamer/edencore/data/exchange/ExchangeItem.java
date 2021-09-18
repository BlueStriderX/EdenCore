package thederpgamer.edencore.data.exchange;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.schine.graphicsengine.forms.gui.GUIOverlay;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/17/2021]
 */
public abstract class ExchangeItem {

    public short barType;
    public int price;
    public String name;
    public String description;

    public ExchangeItem(short barType, int price, String name, String description) {
        this.barType = barType;
        this.price = price;
        this.name = name;
        this.description = description;
    }

    public abstract GUIOverlay getIcon();
    public abstract void serialize(PacketWriteBuffer writeBuffer) throws IOException;
    public abstract void deserialize(PacketReadBuffer readBuffer) throws IOException;
}
