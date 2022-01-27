package thederpgamer.edencore.utils;

import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.meta.weapon.Weapon;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/23/2021]
 */
public class ItemUtils {

  public static short getWeaponSubtype(ElementInformation info) {
    switch (info.getName().toUpperCase()) {
      case "LASER WEAPON":
        return Weapon.WeaponSubType.LASER.type;
      case "HEAL BEAM":
        return Weapon.WeaponSubType.HEAL.type;
      case "MARKER BEAM":
        return Weapon.WeaponSubType.MARKER.type;
      case "POWER SUPPLY BEAM":
        return Weapon.WeaponSubType.POWER_SUPPLY.type;
      case "ROCKET LAUNCHER":
        return Weapon.WeaponSubType.ROCKET_LAUNCHER.type;
      case "SNIPER RIFLE":
        return Weapon.WeaponSubType.SNIPER_RIFLE.type;
      case "GRAPPLE BEAM":
        return Weapon.WeaponSubType.GRAPPLE.type;
      case "TORCH":
        return Weapon.WeaponSubType.TORCH.type;
      case "TRANSPORTER BEACON":
        return Weapon.WeaponSubType.TRANSPORTER_MARKER.type;
      default:
        return -1;
    }
  }
}
