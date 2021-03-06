package com.ketroc.terranbot.models;

import com.github.ocraft.s2client.protocol.data.*;
import com.ketroc.terranbot.GameCache;
import com.ketroc.terranbot.bots.Bot;

public class Cost {
    public int minerals;
    public int gas;
    public float supply;

    public Cost() {

    }

    public Cost(int minerals, int gas) {
        this(minerals, gas, 0);
    }

    public Cost(int minerals, int gas, float supply) {
        this.minerals = minerals;
        this.gas = gas;
        this.supply = supply;
    }

    // =========== METHODS ===========

    public void add(Cost addCost) {
        minerals += addCost.minerals;
        gas += addCost.gas;
        supply += addCost.supply;
    }


    // =========== STATIC METHODS ===========

    public static Cost getUnitCost(UnitType unitType) {
        if (unitType == Units.INVALID) {
            return null;
        }
        UnitTypeData unitData = Bot.OBS.getUnitTypeData(false).get(unitType);
        Cost unitCost = new Cost(unitData.getMineralCost().orElse(0), unitData.getVespeneCost().orElse(0), unitData.getFoodRequired().orElse(0f).intValue());
        if (unitType == Units.TERRAN_ORBITAL_COMMAND || unitType == Units.TERRAN_PLANETARY_FORTRESS) {
            unitCost.minerals -= 400;
        }
        return unitCost;
    }

    public static Cost getUpgradeCost(Upgrades upgrade) {
        UpgradeData upgradeData = Bot.OBS.getUpgradeData(false).get(upgrade);
        return new Cost(upgradeData.getMineralCost().orElse(0), upgradeData.getVespeneCost().orElse(0));
    }
    public static void updateBank(Units unitType) {
        updateBank(getUnitCost(unitType));
    }

    public static void updateBank(Cost cost) {
        GameCache.mineralBank -= cost.minerals;
        GameCache.gasBank -= Math.max(0, cost.gas);
        GameCache.freeSupply -= cost.supply;
    }

    public static boolean isGasBroke() {
        return Bot.OBS.getVespene() == 0 && Bot.OBS.getScore().getDetails().getCollectionRateVespene() == 0;
    }

    public static boolean isMineralBroke() {
        return Bot.OBS.getMinerals() == 0 && Bot.OBS.getScore().getDetails().getCollectionRateMinerals() == 0;
    }
}
